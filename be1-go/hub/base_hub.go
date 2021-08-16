package hub

import (
	"context"
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"sync"

	"student20_pop/message"
	"student20_pop/network/socket"
	"student20_pop/validation"

	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"

	_ "github.com/mattn/go-sqlite3"
)

// rootPrefix denotes the prefix for the root channel
const rootPrefix = "/root/"

// baseHub implements hub.Hub interface
type baseHub struct {
	messageChan chan socket.IncomingMessage

	sync.RWMutex
	channelByID map[string]Channel

	closedSockets chan string

	public kyber.Point

	schemaValidator *validation.SchemaValidator

	wg *sync.WaitGroup
}

// NewBaseHub returns a Base Hub.
func NewBaseHub(public kyber.Point, wg *sync.WaitGroup) (*baseHub, error) {

	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	baseHub := baseHub{
		messageChan:     make(chan socket.IncomingMessage),
		channelByID:     make(map[string]Channel),
		closedSockets:   make(chan string),
		public:          public,
		schemaValidator: schemaValidator,
		wg:              wg,
	}

	if os.Getenv("HUB_DB") != "" {
		log.Printf("loading channels from db at %s", os.Getenv("HUB_DB"))

		channels, err := getChannelsFromDB(&baseHub)
		if err != nil {
			log.Printf("error: failed to load channels from db: %v", err)
		} else {
			baseHub.channelByID = channels
		}
	}

	return &baseHub, nil
}

func (h *baseHub) Start(ctx context.Context) {
	h.wg.Add(1)
	defer h.wg.Done()

	log.Printf("started hub...")
	for {
		select {
		case incomingMessage := <-h.messageChan:
			h.handleIncomingMessage(&incomingMessage)
		case id := <-h.closedSockets:
			h.RLock()
			for _, channel := range h.channelByID {
				// dummy Unsubscribe message because it's only used for
				// logging...
				channel.Unsubscribe(id, message.Unsubscribe{})
			}
			h.RUnlock()
		case <-ctx.Done():
			log.Println("closing the hub...")
			return
		}
	}
}

func (h *baseHub) Receiver() chan<- socket.IncomingMessage {
	return h.messageChan
}

func (h *baseHub) OnSocketClose() chan<- string {
	return h.closedSockets
}

// handleRootChannelMesssage handles an incoming message on the root channel.
func (h *baseHub) handleRootChannelMesssage(id int, socket socket.Socket, query *message.Query) {
	if query.Publish == nil {
		err := &message.Error{
			Code:        -4,
			Description: "only publish is allowed on /root",
		}

		socket.SendError(&id, err)
		return
	}

	// Check if the structure of the message is correct
	msg := query.Publish.Params.Message

	// Verify the data
	err := h.schemaValidator.VerifyJson(msg.RawData, validation.Data)
	if err != nil {
		err = message.NewError("failed to validate the data", err)
		socket.SendError(&id, err)
		return
	}

	// Unmarshal the data
	err = query.Publish.Params.Message.VerifyAndUnmarshalData()
	if err != nil {
		// Return a error of type "-4 request data is invalid" for all the
		// verifications and unmarshaling problems of the data
		err = &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to verify and unmarshal data: %v", err),
		}
		socket.SendError(&id, err)
		return
	}

	if query.Publish.Params.Message.Data.GetAction() == message.DataAction(message.CreateLaoAction) &&
		query.Publish.Params.Message.Data.GetObject() == message.DataObject(message.LaoObject) {
		err := h.createLao(*query.Publish)
		if err != nil {
			err = message.NewError("failed to create lao", err)

			socket.SendError(&id, err)
			return
		}
	} else {
		log.Printf("invalid method: %s", query.GetMethod())
		socket.SendError(&id, &message.Error{
			Code:        -1,
			Description: "you may only invoke lao/create on /root",
		})
		return
	}

	status := 0
	result := message.Result{General: &status}

	log.Printf("sending result: %+v", result)
	socket.SendResult(id, result)
}

// handleMessageFromClient handles an incoming message from an end user.
func (h *baseHub) handleMessageFromClient(incomingMessage *socket.IncomingMessage) {
	socket := incomingMessage.Socket
	byteMessage := incomingMessage.Message

	// Check if the GenericMessage has a field "id"
	genericMsg := &message.GenericMessage{}
	id, ok := genericMsg.UnmarshalID(byteMessage)
	if !ok {
		err := &message.Error{
			Code:        -4,
			Description: "The message does not have a valid `id` field",
		}
		socket.SendError(nil, err)
		return
	}

	// Verify the message
	err := h.schemaValidator.VerifyJson(byteMessage, validation.GenericMessage)
	if err != nil {
		err = message.NewError("failed to verify incoming message", err)
		socket.SendError(&id, err)
		return
	}

	// Unmarshal the message
	err = json.Unmarshal(byteMessage, genericMsg)
	if err != nil {
		// Return a error of type "-4 request data is invalid" for all the
		// unmarshaling problems of the incoming message
		err = &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to unmarshal incoming message: %v", err),
		}

		socket.SendError(&id, err)
		return
	}

	query := genericMsg.Query

	if query == nil {
		return
	}

	channelPath := query.GetChannel()
	log.Printf("channel: %s", channelPath)

	if channelPath == "/root" {
		h.handleRootChannelMesssage(id, socket, query)
		return
	}

	if channelPath[:6] != rootPrefix {
		log.Printf("channel id must begin with /root/")
		socket.SendError(&id, &message.Error{
			Code:        -2,
			Description: "channel id must begin with /root/",
		})
		return
	}

	h.RLock()
	channel, ok := h.channelByID[channelPath]
	if !ok {
		log.Printf("invalid channel: %s", channelPath)
		socket.SendError(&id, &message.Error{
			Code:        -2,
			Description: fmt.Sprintf("channel %s does not exist", channelPath),
		})
		h.RUnlock()
		return
	}
	h.RUnlock()

	method := query.GetMethod()
	log.Printf("method: %s", method)

	msg := []message.Message{}

	// TODO: use constants
	switch method {
	case "subscribe":
		err = channel.Subscribe(socket, *query.Subscribe)
	case "unsubscribe":
		err = channel.Unsubscribe(socket.ID(), *query.Unsubscribe)
	case "publish":
		err = channel.Publish(*query.Publish)
	case "message":
		log.Printf("cannot handle broadcasts right now")
	case "catchup":
		msg = channel.Catchup(*query.Catchup)
		// TODO send catchup response to client
	}

	if err != nil {
		err = message.NewError("failed to process query", err)
		socket.SendError(&id, err)
		return
	}

	result := message.Result{}

	if method == "catchup" {
		result.Catchup = msg
	} else {
		general := 0
		result.General = &general
	}

	socket.SendResult(id, result)
}

// handleMessageFromWitness handles an incoming message from a witness server.
func (h *baseHub) handleMessageFromWitness(incomingMessage *socket.IncomingMessage) {
	//TODO
}

// handleIncomingMessage handles an incoming message based on the socket it
// originates from.
func (h *baseHub) handleIncomingMessage(incomingMessage *socket.IncomingMessage) {
	log.Printf("Hub::handleMessageFromClient: %s", incomingMessage.Message)

	switch incomingMessage.Socket.Type() {
	case socket.ClientSocketType:
		h.handleMessageFromClient(incomingMessage)
	case socket.WitnessSocketType:
		h.handleMessageFromWitness(incomingMessage)
	default:
		log.Printf("error: invalid socket type")
	}

}

// createLao creates a new LAO using the data in the publish parameter.
func (h *baseHub) createLao(publish message.Publish) error {
	h.Lock()
	defer h.Unlock()

	data, ok := publish.Params.Message.Data.(*message.CreateLAOData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: "failed to cast data to CreateLAOData",
		}
	}

	encodedID := base64.URLEncoding.EncodeToString(data.ID)
	laoChannelPath := rootPrefix + encodedID

	if _, ok := h.channelByID[laoChannelPath]; ok {
		return &message.Error{
			Code:        -3,
			Description: "failed to create lao: another one with the same ID exists",
		}
	}

	laoCh := laoChannel{
		rollCall:    rollCall{},
		attendees:   NewAttendees(),
		baseChannel: createBaseChannel(h, laoChannelPath),
	}

	laoCh.inbox.storeMessage(*publish.Params.Message)

	h.channelByID[laoChannelPath] = &laoCh

	if os.Getenv("HUB_DB") != "" {
		saveChannel(laoChannelPath)
	}

	return nil
}

func saveChannel(channelID string) error {
	log.Printf("trying to save the channel in db at %s", os.Getenv("HUB_DB"))

	db, err := sql.Open("sqlite3", os.Getenv("HUB_DB"))
	if err != nil {
		return xerrors.Errorf("failed to open connection: %v", err)
	}

	defer db.Close()

	stmt, err := db.Prepare("insert into lao_channel(lao_channel_id) values(?)")
	if err != nil {
		return xerrors.Errorf("failed to prepare query: %v", err)
	}

	defer stmt.Close()

	_, err = stmt.Exec(channelID)
	if err != nil {
		return xerrors.Errorf("failed to insert channel: %v", err)
	}

	return nil
}

// DB operations. To be replaced by an abstraction.

func getChannelsFromDB(h *baseHub) (map[string]Channel, error) {
	db, err := sql.Open("sqlite3", os.Getenv("HUB_DB"))
	if err != nil {
		return nil, xerrors.Errorf("failed to open connection: %v", err)
	}

	defer db.Close()

	query := `
		SELECT
			lao_channel_id
		FROM
			lao_channel`

	rows, err := db.Query(query)
	if err != nil {
		return nil, xerrors.Errorf("failed to query channels: %v", err)
	}

	defer rows.Close()

	result := make(map[string]Channel)

	for rows.Next() {
		var id string

		err = rows.Scan(&id)
		if err != nil {
			return nil, xerrors.Errorf("failed to parse row: %v", err)
		}

		channel := laoChannel{
			rollCall:    rollCall{},
			attendees:   NewAttendees(),
			baseChannel: createBaseChannel(h, id),
		}

		attendees, err := getAttendeesChannelFromDB(db, id)
		if err != nil {
			return nil, xerrors.Errorf("failed to get attendees: %v", err)
		}

		for _, attendee := range attendees {
			channel.attendees.Add(attendee)
		}

		witnesses, err := getWitnessChannelFromDB(db, id)
		if err != nil {
			return nil, xerrors.Errorf("failed to get witnesses: %v", err)
		}

		channel.witnesses = witnesses

		messages, err := getMessagesChannelFromDB(db, id)
		if err != nil {
			return nil, xerrors.Errorf("failed to get messages: %v", err)
		}

		channel.inbox = createInbox(id)

		for i := range messages {
			msgID := messages[i].message.MessageID
			msgIDEncoded := base64.URLEncoding.EncodeToString(msgID)

			channel.inbox.msgs[msgIDEncoded] = &messages[i]
		}

		result[id] = &channel
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf("error in row iteration: %v", err)
	}

	return result, nil
}

func getAttendeesChannelFromDB(db *sql.DB, channelID string) ([]string, error) {
	query := `
		SELECT
			attendee_key
		FROM
			lao_attendee
		WHERE
			lao_channel_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf("failed to prepare query: %v", err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelID)
	if err != nil {
		return nil, xerrors.Errorf("failed to query rows: %v", err)
	}

	defer rows.Close()

	result := make([]string, 0)

	for rows.Next() {
		var attendeeKey string

		err = rows.Scan(&attendeeKey)
		if err != nil {
			return nil, xerrors.Errorf("failed to parse row: %v", err)
		}

		result = append(result, attendeeKey)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf("error in row iteration: %v", err)
	}

	return result, nil
}

func getWitnessChannelFromDB(db *sql.DB, channelID string) ([]message.PublicKey, error) {
	query := `
		SELECT
			pub_key
		FROM
			lao_witness
		WHERE
			lao_channel_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf("failed to prepare query: %v", err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelID)
	if err != nil {
		return nil, xerrors.Errorf("failed to query rows: %v", err)
	}

	defer rows.Close()

	result := make([]message.PublicKey, 0)

	for rows.Next() {
		var pubKey string

		err = rows.Scan(&pubKey)
		if err != nil {
			return nil, xerrors.Errorf("failed to parse row: %v", err)
		}

		result = append(result, message.PublicKey([]byte(pubKey)))
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf("error in row iteration: %v", err)
	}

	return result, nil
}

func getMessagesChannelFromDB(db *sql.DB, channelID string) ([]messageInfo, error) {
	query := `
		SELECT
			message_id, 
			sender, 
			message_signature, 
			raw_data, 
			message_timestamp
		FROM
			message_info
		WHERE
			lao_channel_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf("failed to prepare query: %v", err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelID)
	if err != nil {
		return nil, xerrors.Errorf("failed to query rows: %v", err)
	}

	defer rows.Close()

	result := make([]messageInfo, 0)

	for rows.Next() {
		var messageID string
		var sender string
		var messageSignature string
		var rawData string
		var timestamp int64

		err = rows.Scan(&messageID, &sender, &messageSignature, &rawData, &timestamp)
		if err != nil {
			return nil, xerrors.Errorf("failed to parse row: %v", err)
		}

		witnesses, err := getWitnessesMessageFromDB(db, messageID)
		if err != nil {
			return nil, xerrors.Errorf("failed to get witnesses: %v", err)
		}

		messageInfo := messageInfo{
			message: &message.Message{
				MessageID:         message.Base64URLBytes(messageID),
				Sender:            message.PublicKey(sender),
				Signature:         message.Signature(messageSignature),
				WitnessSignatures: witnesses,
				RawData:           message.Base64URLBytes(rawData),
			},
			storedTime: message.Timestamp(timestamp),
		}

		log.Printf("Msg load: %+v", messageInfo.message)

		result = append(result, messageInfo)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf("error in row iteration: %v", err)
	}

	return result, nil
}

func getWitnessesMessageFromDB(db *sql.DB, messageID string) ([]message.PublicKeySignaturePair, error) {
	query := `
		SELECT
			pub_key,
			witness_signature
		FROM
			message_witness
		WHERE
			message_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf("failed to prepare query: %v", err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(messageID)
	if err != nil {
		return nil, xerrors.Errorf("failed to query rows: %v", err)
	}

	defer rows.Close()

	result := make([]message.PublicKeySignaturePair, 0)

	for rows.Next() {
		var pubKey string
		var signature string

		err = rows.Scan(&pubKey, &signature)
		if err != nil {
			return nil, xerrors.Errorf("failed to parse row: %v", err)
		}

		result = append(result, message.PublicKeySignaturePair{
			Witness:   message.PublicKey(pubKey),
			Signature: message.Signature(signature),
		})
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf("error in row iteration: %v", err)
	}

	return result, nil
}
