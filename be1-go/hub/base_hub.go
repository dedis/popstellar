package hub

import (
	"context"
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"log"
	"os"
	"sync"
	"time"

	"student20_pop/message"
	"student20_pop/message2"
	"student20_pop/message2/messagedata"
	"student20_pop/message2/query/method"
	messageX "student20_pop/message2/query/method/message"
	"student20_pop/network/socket"
	"student20_pop/validation"

	"go.dedis.ch/kyber/v3"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"

	_ "github.com/mattn/go-sqlite3"
	"github.com/rs/zerolog"
)

// rootPrefix denotes the prefix for the root channel
const rootPrefix = "/root/"

const (
	// numWorkers denote the number of worker go-routines
	// allowed to process requests concurrently.
	numWorkers = 10

	dbPrepareErr  = "failed to prepare query: %v"
	dbParseRowErr = "failed to parse row: %v"
	dbRowIterErr  = "error in row iteration: %v"
	dbQueryRowErr = "failed to query rows: %v"
)

const defaultLevel = zerolog.InfoLevel

var logout = zerolog.ConsoleWriter{
	Out:        os.Stdout,
	TimeFormat: time.RFC3339,
}

// baseHub implements hub.Hub interface
type baseHub struct {
	messageChan chan socket.IncomingMessage

	sync.RWMutex
	channelByID map[string]Channel

	closedSockets chan string

	public kyber.Point

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	workers *semaphore.Weighted

	log *zerolog.Logger
}

// NewBaseHub returns a Base Hub.
func NewBaseHub(public kyber.Point) (*baseHub, error) {

	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log := zerolog.New(logout).Level(defaultLevel).
		With().Timestamp().Logger().
		With().Caller().Logger()

	baseHub := baseHub{
		messageChan:     make(chan socket.IncomingMessage),
		channelByID:     make(map[string]Channel),
		closedSockets:   make(chan string),
		public:          public,
		schemaValidator: schemaValidator,
		stop:            make(chan struct{}),
		workers:         semaphore.NewWeighted(numWorkers),
		log:             &log,
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

func (h *baseHub) Start() {
	go func() {
		for {
			select {
			case incomingMessage := <-h.messageChan:
				ok := h.workers.TryAcquire(1)
				if !ok {
					log.Print("warn: worker pool full, waiting...")
					h.workers.Acquire(context.Background(), 1)
				}

				go h.handleIncomingMessage(&incomingMessage)
			case id := <-h.closedSockets:
				h.RLock()
				for _, channel := range h.channelByID {
					// dummy Unsubscribe message because it's only used for logging...
					channel.Unsubscribe(id, method.Unsubscribe{})
				}
				h.RUnlock()
			case <-h.stop:
				log.Println("Stopping the hub")
				return
			}
		}
	}()
}

func (h *baseHub) Stop() {
	close(h.stop)
	log.Println("Waiting for existing workers to finish...")
	h.workers.Acquire(context.Background(), numWorkers)
}

func (h *baseHub) Receiver() chan<- socket.IncomingMessage {
	return h.messageChan
}

func (h *baseHub) OnSocketClose() chan<- string {
	return h.closedSockets
}

// handleRootChannelMesssage handles an incoming message on the root channel.
func (h *baseHub) handleRootChannelMesssage(socket socket.Socket, publish method.Publish) {
	// if query.Publish == nil {
	// 	err := message.NewError(-4, "only publish is allowed on /root")
	// 	socket.SendError(&id, err)
	// 	return
	// }

	// Check if the structure of the message is correct
	// msg := query.Publish.Params.Message

	jsonData, err := base64.URLEncoding.DecodeString(publish.Params.Message.Data)
	if err != nil {
		socket.SendError(&publish.ID, xerrors.Errorf("failed to decode message data: %v", err))
		return
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJson(jsonData, validation.Data)
	if err != nil {
		socket.SendError(&publish.ID, err)
		return
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		socket.SendError(&publish.ID, err)
		return
	}

	// Unmarshal the data
	// err = query.Publish.Params.Message.VerifyAndUnmarshalData(rootPrefix)
	// if err != nil {
	// 	// Return a error of type "-4 request data is invalid" for all the
	// 	// verifications and unmarshaling problems of the data
	// 	err := message.NewErrorf(-4, "failed to verify and unmarshal data: %v", err)
	// 	socket.SendError(&id, err)
	// 	return
	// }

	// must be "lao#create"
	if object != "lao" || action != "create" {
		log.Printf("invalid method: %s#%s", object, action)
		err := message.NewError(-1, "failed to invoke lao/create: operation only allowed on /root")
		socket.SendError(&publish.ID, err)
		return
	}

	var laoCreate messagedata.LaoCreate

	err = publish.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		socket.SendError(&publish.ID, err)
		return
	}

	err = h.createLao(publish, laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		socket.SendError(&publish.ID, err)
		return
	}

	// if query.Publish.Params.Message.Data.GetAction() == message.DataAction(message.CreateLaoAction) &&
	// 	query.Publish.Params.Message.Data.GetObject() == message.DataObject(message.LaoObject) {
	// 	err := h.createLao(*query.Publish)
	// 	if err != nil {
	// 		socket.SendError(&id, err)
	// 		return
	// 	}
	// } else {
	// 	log.Printf("invalid method: %s", query.GetMethod())
	// 	err := message.NewError(-1, "failed to invoke lao/create: operation only allowed on /root")
	// 	socket.SendError(&id, err)
	// 	return
	// }

	log.Printf("sending empty result")
	socket.SendResult(publish.ID, nil)
}

// handleMessageFromClient handles an incoming message from an end user.
func (h *baseHub) handleMessageFromClient(incomingMessage *socket.IncomingMessage) {
	socket := incomingMessage.Socket
	byteMessage := incomingMessage.Message

	// validate against json schema
	err := h.schemaValidator.VerifyJson(byteMessage, validation.GenericMessage)
	if err != nil {
		h.log.Err(err).Msg("message is not valid against json schema")
		socket.SendError(nil, xerrors.Errorf("message is not valid against json schema: %v", err))
		return
	}

	// unmarshal to jsonRPC
	var jsonRPC message2.JSONRPC

	err = json.Unmarshal(byteMessage, &jsonRPC)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal jsonRPC message")
		socket.SendError(nil, err)
		return
	}

	// check type (answer or query), we expect a query
	if jsonRPC.Type() != message2.RPCTypeQuery {
		h.log.Error().Msgf("jsonRPC message is not of type query")
		socket.SendError(nil, xerrors.New("jsonRPC message is not of type query"))
		return
	}

	// Check if the GenericMessage has a field "id"
	// genericMsg := &message.GenericMessage{}
	// id, ok := genericMsg.UnmarshalID(byteMessage)
	// if !ok {
	// 	err := message.NewError(-4, "The message does not have a valid `id` field")
	// 	socket.SendError(nil, err)
	// 	return
	// }

	// Verify the message
	// err = h.schemaValidator.VerifyJson(byteMessage, validation.GenericMessage)
	// if err != nil {
	// 	socket.SendError(&id, err)
	// 	return
	// }

	// Unmarshal the message
	// err = json.Unmarshal(byteMessage, genericMsg)
	// if err != nil {
	// 	// Return a error of type "-4 request data is invalid" for all the
	// 	// unmarshaling problems of the incoming message
	// 	err := message.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
	// 	socket.SendError(&id, err)
	// 	return
	// }

	// query := genericMsg.Query

	// if query == nil {
	// 	return
	// }

	query := jsonRPC.Query

	// special case if this is a publish on /root
	if query.Method == "publish" && query.Publish.Params.Channel == "/root" {
		h.handleRootChannelMesssage(socket, query.Publish)
		return
	}

	// channelPath := query.GetChannel()
	// log.Printf("channel: %s", channelPath)

	// if channelPath == "/root" {
	// 	h.handleRootChannelMesssage(id, socket, query)
	// 	return
	// }

	// (nkcr) Not ideal, will be optimized
	var channelPath string
	var id int

	switch query.Method {
	case "publish":
		channelPath = query.Publish.Params.Channel
		id = query.Publish.ID
	case "subscribe":
		channelPath = query.Subscribe.Params.Channel
		id = query.Subscribe.ID
	case "unsubscribe":
		channelPath = query.Unsubscribe.Params.Channel
		id = query.Unsubscribe.ID
	case "catchup":
		channelPath = query.Catchup.Params.Channel
		id = query.Catchup.ID
	default:
		log.Printf("unexpected method: %s", query.Method)
		err := message.NewErrorf(-2, "unexpected method: %s", query.Method)
		socket.SendError(nil, err)
	}

	if channelPath[:6] != rootPrefix {
		log.Printf("channel id must begin with /root/")
		err := message.NewErrorf(-2, "channel id must begin with \"/root/\", got: %q", channelPath[:6])
		socket.SendError(&id, err)
		return
	}

	h.RLock()
	channel, ok := h.channelByID[channelPath]
	if !ok {
		log.Printf("invalid channel: %s", channelPath)
		err := message.NewErrorf(-2, "channel %s does not exist", channelPath)
		socket.SendError(&id, err)
		h.RUnlock()
		return
	}
	h.RUnlock()

	msg := []messageX.Message{}

	// TODO: use constants
	switch query.Method {
	case "subscribe":
		err = channel.Subscribe(socket, query.Subscribe)
	case "unsubscribe":
		err = channel.Unsubscribe(socket.ID(), query.Unsubscribe)
	case "publish":
		err = channel.Publish(query.Publish)
	case "message":
		log.Printf("cannot handle broadcasts right now")
	case "catchup":
		msg = channel.Catchup(query.Catchup)
		// TODO send catchup response to client
	}

	if err != nil {
		socket.SendError(&id, err)
		return
	}

	if query.Method == "catchup" {
		socket.SendResult(id, msg)
		return
	}

	socket.SendResult(id, nil)
}

// handleMessageFromWitness handles an incoming message from a witness server.
func (h *baseHub) handleMessageFromWitness(incomingMessage *socket.IncomingMessage) {
	//TODO
}

// handleIncomingMessage handles an incoming message based on the socket it
// originates from.
func (h *baseHub) handleIncomingMessage(incomingMessage *socket.IncomingMessage) {
	defer h.workers.Release(1)

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
func (h *baseHub) createLao(publish method.Publish, laoCreate messagedata.LaoCreate) error {
	h.Lock()
	defer h.Unlock()

	// data, ok := publish.Params.Message.Data.(*message.CreateLAOData)
	// if !ok {
	// 	return message.NewError(-4, "failed to cast data to CreateLAOData")
	// }

	laoChannelPath := rootPrefix + laoCreate.ID

	if _, ok := h.channelByID[laoChannelPath]; ok {
		h.log.Error().Msgf("failed to create lao: duplicate lao path: %q", laoChannelPath)
		return message.NewErrorf(-3, "failed to create lao: duplicate lao path: %q", laoChannelPath)
	}

	laoCh := laoChannel{
		rollCall:    rollCall{},
		attendees:   NewAttendees(),
		baseChannel: createBaseChannel(h, laoChannelPath),
	}

	laoCh.inbox.storeMessage(publish.Params.Message)

	h.log.Info().Msgf("storing new channel '%s' %v", laoChannelPath, publish.Params.Message)

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

	query := `
	INSERT INTO
		lao_channel(
			lao_channel_id)
	VALUES(?)`

	stmt, err := db.Prepare(query)
	if err != nil {
		return xerrors.Errorf(dbPrepareErr, err)
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
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		channel, err := createChannelFromDB(db, h, id)
		if err != nil {
			return nil, xerrors.Errorf("failed to create channel from db: %v", err)
		}

		result[id] = channel
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}

func createChannelFromDB(db *sql.DB, h *baseHub, channelID string) (Channel, error) {
	channel := laoChannel{
		rollCall:    rollCall{},
		attendees:   NewAttendees(),
		baseChannel: createBaseChannel(h, channelID),
	}

	attendees, err := getAttendeesChannelFromDB(db, channelID)
	if err != nil {
		return nil, xerrors.Errorf("failed to get attendees: %v", err)
	}

	for _, attendee := range attendees {
		channel.attendees.Add(attendee)
	}

	witnesses, err := getWitnessChannelFromDB(db, channelID)
	if err != nil {
		return nil, xerrors.Errorf("failed to get witnesses: %v", err)
	}

	channel.witnesses = witnesses

	messages, err := getMessagesChannelFromDB(db, channelID)
	if err != nil {
		return nil, xerrors.Errorf("failed to get messages: %v", err)
	}

	channel.inbox = createInbox(channelID)

	for i := range messages {
		msgID := messages[i].message.MessageID
		channel.inbox.msgs[msgID] = &messages[i]
	}

	return &channel, nil
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
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := make([]string, 0)

	for rows.Next() {
		var attendeeKey string

		err = rows.Scan(&attendeeKey)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, attendeeKey)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
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
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := make([]message.PublicKey, 0)

	for rows.Next() {
		var pubKey string

		err = rows.Scan(&pubKey)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, message.PublicKey([]byte(pubKey)))
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
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
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
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
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		witnesses, err := getWitnessesMessageFromDB(db, messageID)
		if err != nil {
			return nil, xerrors.Errorf("failed to get witnesses: %v", err)
		}

		messageInfo := messageInfo{
			message: messageX.Message{
				MessageID:         messageID,
				Sender:            sender,
				Signature:         messageSignature,
				WitnessSignatures: witnesses,
				Data:              rawData,
			},
			storedTime: message.Timestamp(timestamp),
		}

		log.Printf("Msg load: %+v", messageInfo.message)

		result = append(result, messageInfo)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}

func getWitnessesMessageFromDB(db *sql.DB, messageID string) ([]messageX.WitnessSignature, error) {
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
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(messageID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := make([]messageX.WitnessSignature, 0)

	for rows.Next() {
		var pubKey string
		var signature string

		err = rows.Scan(&pubKey, &signature)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, messageX.WitnessSignature{
			Witness:   pubKey,
			Signature: signature,
		})
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}
