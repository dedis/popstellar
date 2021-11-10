package lao

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"popstellar/channel"
	"popstellar/channel/consensus"
	"popstellar/channel/election"
	"popstellar/channel/inbox"
	"popstellar/crypto"
	"popstellar/db/sqlite"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"strconv"
	"strings"
	"sync"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

const (
	dbPrepareErr  = "failed to prepare query: %v"
	dbParseRowErr = "failed to parse row: %v"
	dbRowIterErr  = "error in row iteration: %v"
	dbQueryRowErr = "failed to query rows: %v"
	msgID         = "msg id"
)

// Channel defines a LAO channel
type Channel struct {
	sockets channel.Sockets

	inbox *inbox.Inbox

	// /root/<ID>
	channelID string

	organizerKey kyber.Point

	witnessMu sync.Mutex
	witnesses []string

	rollCall rollCall

	hub channel.HubFunctionalities

	attendees map[string]struct{}

	log zerolog.Logger
}

// NewChannel returns a new initialized LAO channel. It automatically creates
// its associated consensus channel and register it to the hub
func NewChannel(channelID string, hub channel.HubFunctionalities, msg message.Message, log zerolog.Logger, socket socket.Socket) channel.Channel {

	log = log.With().Str("channel", "lao").Logger()

	inbox := inbox.NewInbox(channelID)
	inbox.StoreMessage(msg)

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		log.Err(err).Msgf("failed to decode sender key: %v", err)
		return nil
	}

	organizerPoint := crypto.Suite.Point()
	err = organizerPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		log.Err(err).Msgf("failed to unmarshal public key of the sender: %v", err)
	}

	consensusPath := fmt.Sprintf("%s/consensus", channelID)

	consensusCh := consensus.NewChannel(consensusPath, hub, log)

	hub.RegisterNewChannel(consensusPath, &consensusCh, socket)

	return &Channel{
		channelID:    channelID,
		sockets:      channel.NewSockets(),
		inbox:        inbox,
		organizerKey: organizerPoint,
		hub:          hub,
		rollCall:     rollCall{},
		attendees:    make(map[string]struct{}),
		log:          log,
	}
}

// Subscribe is used to handle a subscribe message from the client.
func (c *Channel) Subscribe(socket socket.Socket, msg method.Subscribe) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(msg.ID)).
		Msg("received a subscribe")
	c.sockets.Upsert(socket)

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *Channel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(msg.ID)).
		Msg("received an unsubscribe")

	ok := c.sockets.Delete(socketID)

	if !ok {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	c.log.Info().
		Str(msgID, strconv.Itoa(catchup.ID)).
		Msg("received a catchup")

	return c.inbox.GetSortedMessages()
}

// Broadcast is used to handle a broadcast message.
func (c *Channel) Broadcast(msg method.Broadcast) error {
	err := xerrors.Errorf("a lao shouldn't need to broadcast a message")
	c.log.Err(err)
	return err
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *Channel) broadcastToAllClients(msg message.Message) {
	c.log.Info().
		Str(msgID, msg.MessageID).
		Msg("broadcasting message to all clients")

	rpcMessage := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "broadcast",
		},
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			c.channelID,
			msg,
		},
	}

	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		c.log.Err(err).Msg("failed to marshal broadcast query")
	}

	c.sockets.SendToAll(buf)
}

// VerifyPublishMessage checks if a Publish message is valid
func (c *Channel) VerifyPublishMessage(publish method.Publish) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	// Check if the structure of the message is correct
	msg := publish.Params.Message

	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	// Verify the data
	err = c.hub.GetSchemaValidator().VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return xerrors.Errorf("failed to verify json schema: %w", err)
	}

	// Check if the message already exists
	if _, ok := c.inbox.GetMessage(msg.MessageID); ok {
		return answer.NewError(-3, "message already exists")
	}

	return nil
}

// rollCallState denotes the state of the roll call.
type rollCallState string

const (
	// Open represents the open roll call state.
	Open rollCallState = "open"

	// Closed represents the closed roll call state.
	Closed rollCallState = "closed"

	// Created represents the created roll call state.
	Created rollCallState = "created"
)

// rollCall represents a roll call.
type rollCall struct {
	state rollCallState
	id    string
}

// Publish handles publish messages for the LAO channel.
func (c *Channel) Publish(publish method.Publish, socket socket.Socket) error {
	err := c.VerifyPublishMessage(publish)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message: %w", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	jsonData, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)

	switch object {
	case messagedata.LAOObject:
		err = c.processLaoObject(action, msg)
	case messagedata.MeetingObject:
		err = c.processMeetingObject(action, msg)
	case messagedata.MessageObject:
		err = c.processMessageObject(action, msg)
	case messagedata.RollCallObject:
		err = c.processRollCallObject(action, msg)
	case messagedata.ElectionObject:
		err = c.processElectionObject(action, msg, socket)
	}

	if err != nil {
		return xerrors.Errorf("failed to process %q object: %w", object, err)
	}

	c.broadcastToAllClients(msg)
	return nil
}

// processLaoObject processes a LAO object.
func (c *Channel) processLaoObject(action string, msg message.Message) error {
	switch action {
	case messagedata.LAOActionUpdate:
	case messagedata.LAOActionState:
		var laoState messagedata.LaoState

		err := msg.UnmarshalData(&laoState)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal lao#state: %v", err)
		}

		err = c.verifyMessageLaoID(laoState.ID)
		if err != nil {
			return xerrors.Errorf("invalid lao#state message: %v", err)
		}

		err = c.processLaoState(laoState)
		if err != nil {
			return xerrors.Errorf("failed to process state action: %w", err)
		}
	default:
		return answer.NewInvalidActionError(action)
	}

	c.inbox.StoreMessage(msg)

	return nil
}

// processLaoState processes a lao state action.
func (c *Channel) processLaoState(data messagedata.LaoState) error {
	// Check if we have the update message
	msg, ok := c.inbox.GetMessage(data.ModificationID)

	if !ok {
		return answer.NewErrorf(-4, "cannot find lao/update_properties with ID: %s", data.ModificationID)
	}

	// Check if the signatures are from witnesses we need. We maintain
	// the current state of witnesses for a LAO in the channel instance
	// TODO: threshold signature verification

	c.witnessMu.Lock()
	match := 0
	expected := len(c.witnesses)
	// TODO: O(N^2), O(N) possible
	for i := 0; i < expected; i++ {
		for j := 0; j < len(data.ModificationSignatures); j++ {
			if c.witnesses[i] == data.ModificationSignatures[j].Witness {
				match++
				break
			}
		}
	}
	c.witnessMu.Unlock()

	if match != expected {
		return answer.NewErrorf(-4, "not enough witness signatures provided. Needed %d got %d", expected, match)
	}

	// Check if the signatures match
	for _, pair := range data.ModificationSignatures {
		err := schnorr.VerifyWithChecks(crypto.Suite, []byte(pair.Witness), []byte(data.ModificationID), []byte(pair.Signature))
		if err != nil {
			return answer.NewErrorf(-4, "signature verfication failed for witness: %s", pair.Witness)
		}
	}

	var updateMsgData messagedata.LaoUpdate

	err := msg.UnmarshalData(&updateMsgData)
	if err != nil {
		return &answer.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to unmarshal message from the inbox: %v", err),
		}
	}

	err = updateMsgData.Verifiy()
	if err != nil {
		return &answer.Error{
			Code:        -4,
			Description: fmt.Sprintf("invalid lao#update message: %v", err),
		}
	}

	err = compareLaoUpdateAndState(updateMsgData, data)
	if err != nil {
		return xerrors.Errorf("failed to compare lao/update and existing state: %w", err)
	}

	return nil
}

func compareLaoUpdateAndState(update messagedata.LaoUpdate, state messagedata.LaoState) error {
	if update.LastModified != state.LastModified {
		return answer.NewErrorf(-4, "mismatch between last modified: expected %d got %d", update.LastModified, state.LastModified)
	}

	if update.Name != state.Name {
		return answer.NewErrorf(-4, "mismatch between name: expected %s got %s", update.Name, state.Name)
	}

	M := len(update.Witnesses)
	N := len(state.Witnesses)

	if M != N {
		return answer.NewErrorf(-4, "mismatch between witness count: expected %d got %d", M, N)
	}

	match := 0

	for i := 0; i < M; i++ {
		for j := 0; j < N; j++ {
			if update.Witnesses[i] == state.Witnesses[j] {
				match++
				break
			}
		}
	}

	if match != M {
		return answer.NewErrorf(-4, "mismatch between witness keys: expected %d keys to match but %d matched", M, match)
	}

	return nil
}

// verify if a lao message id is the same as the lao id
func (c *Channel) verifyMessageLaoID(id string) error {
	expectedID := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	if expectedID != id {
		return xerrors.Errorf("lao id is %s, should be %s", id, expectedID)
	}

	return nil
}

// processMeetingObject handles a meeting object.
func (c *Channel) processMeetingObject(action string, msg message.Message) error {

	// Nothing to do ...🤷‍♂️
	switch action {
	case messagedata.MeetingActionCreate:
	case messagedata.MeetingActionState:
	}

	c.inbox.StoreMessage(msg)

	return nil
}

// processMessageObject handles a message object.
func (c *Channel) processMessageObject(action string, msg message.Message) error {

	switch action {
	case messagedata.MessageActionWitness:
		var witnessData messagedata.MessageWitness

		err := msg.UnmarshalData(&witnessData)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal witness data: %v", err)
		}

		err = schnorr.VerifyWithChecks(crypto.Suite, []byte(msg.Sender), []byte(witnessData.MessageID), []byte(witnessData.Signature))
		if err != nil {
			return answer.NewError(-4, "invalid witness signature")
		}

		err = c.inbox.AddWitnessSignature(witnessData.MessageID, msg.Sender, witnessData.Signature)
		if err != nil {
			return xerrors.Errorf("failed to add witness signature: %w", err)
		}
	default:
		return answer.NewInvalidActionError(action)
	}

	return nil
}

// processRollCallObject handles a roll call object.
func (c *Channel) processRollCallObject(action string, msg message.Message) error {
	sender := msg.Sender

	senderBuf, err := base64.URLEncoding.DecodeString(sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	// Check if the sender of the roll call message is the organizer
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewErrorf(-4, "failed to unmarshal public key of the sender: %v", err)
	}

	if !c.organizerKey.Equal(senderPoint) {
		return answer.NewErrorf(-5, "sender's public key %q does not match the organizer's", msg.Sender)
	}

	switch action {
	case messagedata.RollCallActionCreate:
		var rollCallCreate messagedata.RollCallCreate

		err := msg.UnmarshalData(&rollCallCreate)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal roll call create: %v", err)
		}

		err = c.processCreateRollCall(rollCallCreate)
		if err != nil {
			return xerrors.Errorf("failed to process roll call create: %v", err)
		}

	case messagedata.RollCallActionOpen, messagedata.RollCallActionReopen:
		err := c.processOpenRollCall(msg, action)
		if err != nil {
			return xerrors.Errorf("failed to process open roll call: %v", err)
		}

	case messagedata.RollCallActionClose:
		var rollCallClose messagedata.RollCallClose

		err := msg.UnmarshalData(&rollCallClose)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal roll call close: %v", err)
		}

		err = c.processCloseRollCall(rollCallClose)
		if err != nil {
			return xerrors.Errorf("failed to process close roll call: %v", err)
		}
	default:
		return answer.NewInvalidActionError(action)
	}

	if err != nil {
		return xerrors.Errorf("failed to process roll call action: %s %w", action, err)
	}

	c.inbox.StoreMessage(msg)

	return nil
}

// verify

// processElectionObject handles an election object.
func (c *Channel) processElectionObject(action string, msg message.Message, socket socket.Socket) error {
	expectedAction := messagedata.ElectionActionSetup

	if action != expectedAction {
		return answer.NewErrorf(-4, "invalid action: %s != %s)", action, expectedAction)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	// Check if the sender of election creation message is the organizer
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewErrorf(-4, "failed to unmarshal public key of the sender: %v", err)
	}

	if !c.organizerKey.Equal(senderPoint) {
		return answer.NewError(-5, "The sender of the election setup message has a different public key from the organizer")
	}

	var electionSetup messagedata.ElectionSetup

	err = msg.UnmarshalData(&electionSetup)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal election setup: %v", err)
	}

	err = c.createElection(msg, electionSetup, socket)
	if err != nil {
		return xerrors.Errorf("failed to create election: %w", err)
	}

	c.log.Info().Msg("election created with success")
	return nil
}

// createElection creates an election in the LAO.
func (c *Channel) createElection(msg message.Message, setupMsg messagedata.ElectionSetup, socket socket.Socket) error {

	// Check if the Lao ID of the message corresponds to the channel ID
	channelID := c.channelID[6:]
	if channelID != setupMsg.Lao {
		return answer.NewErrorf(-4, "Lao ID of the message (Lao: %s) is different from the channelID (channel: %s)", setupMsg.Lao, channelID)
	}

	// Compute the new election channel id
	channelPath := "/root/" + setupMsg.Lao + "/" + setupMsg.ID

	// Create the new election channel
	electionCh := election.NewChannel(channelPath, setupMsg.StartTime, setupMsg.EndTime, false, setupMsg.Questions, c.attendees, msg, c.hub, c.log)
	// {
	// 	createBaseChannel(organizerHub, channelPath),
	// 	setupMsg.StartTime,
	// 	setupMsg.EndTime,
	// 	false,
	// 	getAllQuestionsForElectionChannel(setupMsg.Questions),
	// 	c.attendees,
	// }

	// Saving the election channel creation message on the lao channel
	c.inbox.StoreMessage(msg)

	// Add the new election channel to the organizerHub
	c.hub.RegisterNewChannel(channelPath, &electionCh, socket)

	return nil
}

// processCreateRollCall processes a roll call creation object.
func (c *Channel) processCreateRollCall(msg messagedata.RollCallCreate) error {

	// Check that the ID is correct
	err := c.verifyMessageRollCallCreateID(msg)
	if err != nil {
		return xerrors.Errorf("invalid rollcall#create message: %v", err)
	}

	// Check that the ProposedEnd is greater than the ProposedStart
	if msg.ProposedStart > msg.ProposedEnd {
		return answer.NewErrorf(-4, "The field `proposed_start` is greater than the field `proposed_end`: %d > %d", msg.ProposedStart, msg.ProposedEnd)
	}

	c.rollCall.id = string(msg.ID)
	c.rollCall.state = Created
	return nil
}

// processOpenRollCall processes an open roll call object.
func (c *Channel) processOpenRollCall(msg message.Message, action string) error {
	if action == messagedata.RollCallActionOpen {
		// If the action is an OpenRollCallAction,
		// the previous roll call action should be a CreateRollCallAction
		if c.rollCall.state != Created {
			return answer.NewError(-1, "The roll call cannot be opened since it does not exist")
		}
	} else {
		// If the action is an RepenRollCallAction,
		// the previous roll call action should be a CloseRollCallAction
		if c.rollCall.state != Closed {
			return answer.NewError(-1, "The roll call cannot be reopened since it has not been closed")
		}
	}

	// Why not messagedata.RollCallReopen ? Maybe we should assume that Reopen
	// message is useless.
	var rollCallOpen messagedata.RollCallOpen

	err := msg.UnmarshalData(&rollCallOpen)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal roll call open: %v", err)
	}

	err = c.verifyMessageRollCallOpenID(rollCallOpen)
	if err != nil {
		return xerrors.Errorf("invalid rollcall#open message: %v", err)
	}

	if !c.rollCall.checkPrevID([]byte(rollCallOpen.Opens)) {
		return answer.NewError(-1, "The field `opens` does not correspond to the id of the previous roll call message")
	}

	c.rollCall.id = rollCallOpen.UpdateID
	c.rollCall.state = Open
	return nil
}

// processCloseRollCall processes a close roll call message.
func (c *Channel) processCloseRollCall(msg messagedata.RollCallClose) error {

	err := c.verifyMessageRollCallCloseID(msg)
	if err != nil {
		return xerrors.Errorf("invalid rollcall#close message: %v", err)
	}

	if c.rollCall.state != Open {
		return answer.NewError(-1, "The roll call cannot be closed since it's not open")
	}

	if !c.rollCall.checkPrevID([]byte(msg.Closes)) {
		return answer.NewError(-4, "The field `closes` does not correspond to the id of the previous roll call message")
	}

	c.rollCall.id = msg.UpdateID
	c.rollCall.state = Closed

	var db *sql.DB

	if sqlite.GetDBPath() != "" {
		db, err := sql.Open("sqlite3", sqlite.GetDBPath())
		if err != nil {
			c.log.Err(err).Msg("failed to connect to db")
			db = nil
		} else {
			defer db.Close()
		}
	}

	for _, attendee := range msg.Attendees {
		c.attendees[attendee] = struct{}{}

		if db != nil {
			c.log.Info().Msgf("inserting attendee %s into db", attendee)

			err := insertAttendee(db, attendee, c.channelID)
			if err != nil {
				c.log.Err(err).Msgf("failed to insert attendee %s into db", attendee)
			}
		}
	}

	return nil
}

const InvalidIDMessage string = "ID %s does not correspond with message data, should be %s"

// verifyMessageRollCallCreateID verify the id of a message
func (c *Channel) verifyMessageRollCallCreateID(msg messagedata.RollCallCreate) error {
	expectedID := messagedata.Hash(
		"R",
		strings.ReplaceAll(c.channelID, messagedata.RootPrefix, ""),
		fmt.Sprintf("%d", msg.Creation),
		msg.Name,
	)

	if msg.ID != expectedID {
		return xerrors.Errorf(InvalidIDMessage, msg.ID, expectedID)
	}

	return nil
}

// verifyMessageRollCallOpenID verify the id of a message
func (c *Channel) verifyMessageRollCallOpenID(msg messagedata.RollCallOpen) error {
	expectedID := messagedata.Hash(
		"R",
		strings.ReplaceAll(c.channelID, messagedata.RootPrefix, ""),
		msg.Opens,
		fmt.Sprintf("%d", msg.OpenedAt),
	)

	if msg.UpdateID != expectedID {
		return xerrors.Errorf(InvalidIDMessage, msg.UpdateID, expectedID)
	}

	return nil
}

// verifyMessageRollCallCloseID verify the id of a message
func (c *Channel) verifyMessageRollCallCloseID(msg messagedata.RollCallClose) error {
	expectedID := messagedata.Hash(
		"R",
		strings.ReplaceAll(c.channelID, messagedata.RootPrefix, ""),
		msg.Closes,
		fmt.Sprintf("%d", msg.ClosedAt),
	)

	if msg.UpdateID != expectedID {
		return xerrors.Errorf(InvalidIDMessage, msg.UpdateID, expectedID)
	}

	return nil
}

// ---
// DB operations
// ---

func insertAttendee(db *sql.DB, key string, channelID string) error {
	stmt, err := db.Prepare("insert into lao_attendee(attendee_key, lao_channel_id) values(?, ?)")
	if err != nil {
		return xerrors.Errorf("failed to prepare query: %v", err)
	}

	defer stmt.Close()

	_, err = stmt.Exec(key, channelID)
	if err != nil {
		return xerrors.Errorf("failed to exec query: %v", err)
	}

	return nil
}

// checkPrevID is a helper method which validates the roll call ID.
func (r *rollCall) checkPrevID(prevID []byte) bool {
	return string(prevID) == r.id
}

// CreateChannelFromDB restores a channel from the db
func CreateChannelFromDB(db *sql.DB, channelPath string, hub channel.HubFunctionalities, log zerolog.Logger) (channel.Channel, error) {
	log = log.With().Str("channel", "lao").Logger()

	channel := Channel{
		channelID: channelPath,
		sockets:   channel.NewSockets(),
		inbox:     inbox.NewInbox(channelPath),
		hub:       hub,
		rollCall:  rollCall{},
		attendees: make(map[string]struct{}),
		log:       log,
	}

	attendees, err := getAttendeesChannelFromDB(db, channelPath)
	if err != nil {
		return nil, xerrors.Errorf("failed to get attendees: %v", err)
	}

	for _, attendee := range attendees {
		channel.attendees[attendee] = struct{}{}
	}

	witnesses, err := getWitnessChannelFromDB(db, channelPath)
	if err != nil {
		return nil, xerrors.Errorf("failed to get witnesses: %v", err)
	}

	channel.witnesses = witnesses

	inbox, err := inbox.CreateInboxFromDB(db, channelPath)
	if err != nil {
		return nil, xerrors.Errorf("failed to load inbox: %v", err)
	}

	channel.inbox = inbox

	return &channel, nil
}

func getAttendeesChannelFromDB(db *sql.DB, channelPath string) ([]string, error) {
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

	rows, err := stmt.Query(channelPath)
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

func getWitnessChannelFromDB(db *sql.DB, channelPath string) ([]string, error) {
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

	rows, err := stmt.Query(channelPath)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := make([]string, 0)

	for rows.Next() {
		var pubKey string

		err = rows.Scan(&pubKey)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, pubKey)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}
