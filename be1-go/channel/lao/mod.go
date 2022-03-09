package lao

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"fmt"
	be1_go "popstellar"
	"popstellar/channel"
	"popstellar/channel/chirp"
	"popstellar/channel/consensus"
	"popstellar/channel/election"
	"popstellar/channel/generalChirping"
	"popstellar/channel/reaction"
	"popstellar/crypto"
	"popstellar/db/sqlite"
	"popstellar/inbox"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"strconv"
	"sync"

	"github.com/rs/zerolog/log"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

const (
	keyDecodeError    = "failed to decode sender key: %v"
	keyUnmarshalError = "failed to unmarshal public key of the sender: %v"

	failedToDecodeData = "failed to decode message data: %v"

	dbPrepareErr  = "failed to prepare query: %v"
	dbParseRowErr = "failed to parse row: %v"
	dbRowIterErr  = "error in row iteration: %v"
	dbQueryRowErr = "failed to query rows: %v"
	msgID         = "msg id"
	social        = "/social/"
	chirps        = "chirps"
)

// Channel defines a LAO channel
type Channel struct {
	sockets channel.Sockets

	inbox     *inbox.Inbox
	general   channel.Broadcastable
	reactions channel.LAOFunctionalities

	// /root/<ID>
	channelID string

	organizerPubKey kyber.Point

	witnessMu sync.Mutex
	witnesses map[string]struct{}

	rollCall rollCall

	hub channel.HubFunctionalities

	attendees map[string]struct{}

	log zerolog.Logger
}

// NewChannel returns a new initialized LAO channel. It automatically creates
// its associated consensus channel and register it to the hub.
func NewChannel(channelID string, hub channel.HubFunctionalities, msg message.Message,
	log zerolog.Logger, organizerPubKey kyber.Point, socket socket.Socket) channel.Channel {

	log = log.With().Str("channel", "lao").Logger()

	box := inbox.NewInbox(channelID)
	box.StoreMessage(msg)

	generalCh := createGeneralChirpingChannel(channelID, hub, socket)

	reactionPath := fmt.Sprintf("%s/social/reactions", channelID)
	reactionCh := reaction.NewChannel(reactionPath, hub, log)
	hub.NotifyNewChannel(reactionPath, &reactionCh, socket)

	consensusPath := fmt.Sprintf("%s/consensus", channelID)
	consensusCh := consensus.NewChannel(consensusPath, hub, log)
	hub.NotifyNewChannel(consensusPath, consensusCh, socket)

	return &Channel{
		channelID:       channelID,
		sockets:         channel.NewSockets(),
		inbox:           box,
		general:         generalCh,
		reactions:       &reactionCh,
		organizerPubKey: organizerPubKey,
		witnesses:       make(map[string]struct{}),
		hub:             hub,
		rollCall:        rollCall{},
		attendees:       make(map[string]struct{}),
		log:             log,
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
func (c *Channel) Broadcast(broadcast method.Broadcast, socket socket.Socket) error {
	c.log.Info().Msg("received a broadcast")

	err := c.verifyMessage(broadcast.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify broadcast message: %w", err)
	}

	err = c.handleMessage(broadcast.Params.Message, socket)
	if err != nil {
		return xerrors.Errorf("failed to handle broadcast message: %v", err)
	}

	return nil
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

// verifyMessage checks if a message in a Publish or Broadcast method is valid
func (c *Channel) verifyMessage(msg message.Message) error {

	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf(failedToDecodeData, err)
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

// createGeneralChirpingChannel creates a new general chirping channel and returns it
func createGeneralChirpingChannel(laoID string, hub channel.HubFunctionalities, socket socket.Socket) *generalChirping.Channel {
	generalChannelPath := laoID + social + chirps
	generalChirpingChannel := generalChirping.NewChannel(generalChannelPath, hub, be1_go.Logger)
	hub.NotifyNewChannel(generalChannelPath, generalChirpingChannel, socket)

	log.Info().Msgf("storing new channel '%s' ", generalChannelPath)

	return generalChirpingChannel
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
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	err := c.verifyMessage(publish.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message: %w", err)
	}

	err = c.handleMessage(publish.Params.Message, socket)
	if err != nil {
		return xerrors.Errorf("failed to handle publish message: %v", err)
	}

	return nil
}

// handleMessage handles a message received in a broadcast or publish method
func (c *Channel) handleMessage(msg message.Message, socket socket.Socket) error {

	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf(failedToDecodeData, err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return xerrors.Errorf("failed to get the action or the object: %v", err)
	}

	switch object {
	case messagedata.LAOObject:
		err = c.processLaoObject(action, msg)
	case messagedata.MeetingObject:
		err = c.processMeetingObject(action, msg)
	case messagedata.MessageObject:
		err = c.processMessageObject(action, msg)
	case messagedata.RollCallObject:
		err = c.processRollCallObject(action, msg, socket)
	case messagedata.ElectionObject:
		err = c.processElectionObject(action, msg, socket)
	default:
		err = xerrors.Errorf("object not accepted in a LAO channel.")
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

		err = c.verifyMessageLaoState(laoState)
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

	// Check if the signatures are from witnesses we need.
	c.witnessMu.Lock()
	match := 0
	expected := len(c.witnesses)
	for j := 0; j < len(data.ModificationSignatures); j++ {
		_, ok := c.witnesses[data.ModificationSignatures[j].Witness]
		if ok {
			match++
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

	err = updateMsgData.Verify()
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
func (c *Channel) processRollCallObject(action string, msg message.Message, socket socket.Socket) error {
	sender := msg.Sender

	senderBuf, err := base64.URLEncoding.DecodeString(sender)
	if err != nil {
		return xerrors.Errorf(keyDecodeError, err)
	}

	// Check if the sender of the roll call message is the organizer
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewErrorf(-4, keyUnmarshalError, err)
	}

	if !c.organizerPubKey.Equal(senderPoint) {
		return answer.NewErrorf(-5, "sender's public key %q does not match the organizer's", msg.Sender)
	}

	switch action {
	case messagedata.RollCallActionCreate:
		var rollCallCreate messagedata.RollCallCreate

		err := msg.UnmarshalData(&rollCallCreate)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal roll call create: %v", err)
		}

		err = c.processRollCallCreate(rollCallCreate)
		if err != nil {
			return xerrors.Errorf("failed to process roll call create: %v", err)
		}

	case messagedata.RollCallActionOpen, messagedata.RollCallActionReopen:
		err := c.processRollCallOpen(msg, action)
		if err != nil {
			return xerrors.Errorf("failed to process open roll call: %v", err)
		}

	case messagedata.RollCallActionClose:
		var rollCallClose messagedata.RollCallClose

		err := msg.UnmarshalData(&rollCallClose)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal roll call close: %v", err)
		}

		err = c.processRollCallClose(rollCallClose, socket)
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

func (c *Channel) createChirpingChannel(publicKey string, socket socket.Socket) {
	chirpingChannelPath := c.channelID + social + publicKey

	cha := chirp.NewChannel(chirpingChannelPath, publicKey, c.hub, c.general, be1_go.Logger)
	c.hub.NotifyNewChannel(chirpingChannelPath, cha, socket)
	log.Info().Msgf("storing new chirp channel (%s) for: '%s'", c.channelID, publicKey)
}

// processElectionObject handles an election object.
func (c *Channel) processElectionObject(action string, msg message.Message,
	socket socket.Socket) error {
	expectedAction := messagedata.ElectionActionSetup

	if action != expectedAction {
		return answer.NewErrorf(-4, "invalid action %s, should be %s)", action, expectedAction)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf(keyDecodeError, err)
	}

	// Check if the sender of election creation message is the organizer
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewErrorf(-4, keyUnmarshalError, err)
	}

	if !c.organizerPubKey.Equal(senderPoint) {
		return answer.NewErrorf(-5, "Sender key does not match the "+
			"organizer's one: %s != %s", senderPoint, c.organizerPubKey)
	}

	var electionSetup messagedata.ElectionSetup

	err = msg.UnmarshalData(&electionSetup)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal election setup: %v", err)
	}

	err = c.verifyMessageElectionSetup(electionSetup)
	if err != nil {
		return xerrors.Errorf("invalid election#setup message: %v", err)
	}

	err = c.createElection(msg, electionSetup, socket)
	if err != nil {
		return xerrors.Errorf("failed to create election: %w", err)
	}

	c.log.Info().Msg("election created with success")
	return nil
}

// createElection creates an election in the LAO.
func (c *Channel) createElection(msg message.Message,
	setupMsg messagedata.ElectionSetup, socket socket.Socket) error {

	// Check if the Lao ID of the message corresponds to the channel ID
	channelID := c.channelID[6:]
	if channelID != setupMsg.Lao {
		return answer.NewErrorf(-4, "Lao ID of the message is %s, should be equal to the channel ID %s",
			setupMsg.Lao, channelID)
	}

	// Compute the new election channel id
	channelPath := "/root/" + setupMsg.Lao + "/" + setupMsg.ID

	// Create the new election channel
	electionCh := election.NewChannel(channelPath, setupMsg.StartTime, setupMsg.EndTime,
		setupMsg.Questions, c.attendees, c.hub, c.log, c.organizerPubKey)

	// Saving the election channel creation message on the lao channel
	c.inbox.StoreMessage(msg)

	// Add the new election channel to the organizerHub
	c.hub.NotifyNewChannel(channelPath, &electionCh, socket)

	return nil
}

// processRollCallCreate processes a roll call creation object.
func (c *Channel) processRollCallCreate(msg messagedata.RollCallCreate) error {

	// Check that data is correct
	err := c.verifyMessageRollCallCreate(msg)
	if err != nil {
		return xerrors.Errorf("invalid roll_call#create message: %v", err)
	}

	// Check that the ProposedEnd is greater than the ProposedStart
	if msg.ProposedStart > msg.ProposedEnd {
		return answer.NewErrorf(-4, "The field `proposed_start` is greater than the field `proposed_end`: %d > %d", msg.ProposedStart, msg.ProposedEnd)
	}

	c.rollCall.id = string(msg.ID)
	c.rollCall.state = Created
	return nil
}

// processRollCallOpen processes an open roll call object.
func (c *Channel) processRollCallOpen(msg message.Message, action string) error {
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

	// check that data is correct
	err = c.verifyMessageRollCallOpen(rollCallOpen)
	if err != nil {
		return xerrors.Errorf("invalid roll_call#open message: %v", err)
	}

	if !c.rollCall.checkPrevID([]byte(rollCallOpen.Opens)) {
		return answer.NewError(-1, "The field `opens` does not correspond to the id of the previous roll call message")
	}

	c.rollCall.id = rollCallOpen.UpdateID
	c.rollCall.state = Open
	return nil
}

// processRollCallClose processes a close roll call message.
func (c *Channel) processRollCallClose(msg messagedata.RollCallClose, socket socket.Socket) error {

	// check that data is correct
	err := c.verifyMessageRollCallClose(msg)
	if err != nil {
		return xerrors.Errorf("invalid roll_call#close message: %v", err)
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

		c.createChirpingChannel(attendee, socket)

		c.reactions.AddAttendee(attendee)

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
		witnesses: make(map[string]struct{}),
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

	for _, witness := range witnesses {
		channel.witnesses[witness] = struct{}{}
	}

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
