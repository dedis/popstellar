package lao

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	popstellar "popstellar"
	"popstellar/channel"
	"popstellar/channel/chirp"
	"popstellar/channel/coin"
	"popstellar/channel/consensus"
	"popstellar/channel/election"
	"popstellar/channel/generalChirping"
	"popstellar/channel/reaction"
	"popstellar/channel/registry"
	"popstellar/crypto"
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
	"strings"
	"sync"

	"go.dedis.ch/kyber/v3"

	"github.com/rs/zerolog/log"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

const (
	keyDecodeError    = "failed to decode sender key: %v"
	keyUnmarshalError = "failed to unmarshal public key of the sender: %v"

	failedToDecodeData = "failed to decode message data: %v"

	msgID  = "msg id"
	social = "/social/"
	chirps = "chirps"

	// Open represents the open roll call state.
	Open rollCallState = "open"

	// Closed represents the closed roll call state.
	Closed rollCallState = "closed"

	// Created represents the created roll call state.
	Created rollCallState = "created"
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

	registry registry.MessageRegistry
}

// rollCallState denotes the state of the roll call.
type rollCallState string

// rollCall represents a roll call.
type rollCall struct {
	state rollCallState
	id    string
}

// NewChannel returns a new initialized LAO channel. It automatically creates
// its associated consensus channel and register it to the hub.
func NewChannel(channelID string, hub channel.HubFunctionalities, msg message.Message,
	log zerolog.Logger, organizerPubKey kyber.Point, socket socket.Socket) (channel.Channel, error) {

	log = log.With().Str("channel", "lao").Logger()

	box := inbox.NewInbox(channelID)
	box.StoreMessage(msg)

	generalCh := createGeneralChirpingChannel(channelID, hub, socket)

	reactionPath := fmt.Sprintf("%s/social/reactions", channelID)
	reactionCh := reaction.NewChannel(reactionPath, hub, log)
	hub.NotifyNewChannel(reactionPath, reactionCh, socket)

	consensusPath := fmt.Sprintf("%s/consensus", channelID)
	consensusCh := consensus.NewChannel(consensusPath, hub, log, organizerPubKey)
	hub.NotifyNewChannel(consensusPath, consensusCh, socket)

	newChannel := &Channel{
		channelID:       channelID,
		sockets:         channel.NewSockets(),
		inbox:           box,
		general:         generalCh,
		reactions:       reactionCh,
		organizerPubKey: organizerPubKey,
		witnesses:       make(map[string]struct{}),
		hub:             hub,
		rollCall:        rollCall{},
		attendees:       make(map[string]struct{}),
		log:             log,
	}

	newChannel.registry = newChannel.NewLAORegistry()

	err := newChannel.createAndSendLAOGreet()
	if err != nil {
		return nil, xerrors.Errorf("failed to send the greeting message: %v", err)
	}

	newChannel.createCoinChannel(socket, newChannel.log)

	return newChannel, err
}

// ---
// Publish-subscribe / channel.Channel implementation
// ---

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

// ---
// Message handling
// ---

// handleMessage handles a message received in a broadcast or publish method
func (c *Channel) handleMessage(msg message.Message, socket socket.Socket) error {
	err := c.registry.Process(msg, socket)
	if err != nil {
		return xerrors.Errorf("failed to process message: %w", err)
	}

	c.inbox.StoreMessage(msg)

	err = c.broadcastToAllClients(msg)
	if err != nil {
		return xerrors.Errorf("failed to broadcast message: %v", err)
	}

	return nil
}

// NewLAORegistry creates a new registry for the LAO channel
func (c *Channel) NewLAORegistry() registry.MessageRegistry {
	registry := registry.NewMessageRegistry()

	registry.Register(messagedata.LaoUpdate{}, c.processEmptyFun)
	registry.Register(messagedata.LaoState{}, c.processLaoState)
	registry.Register(messagedata.MeetingCreate{}, c.processEmptyFun)
	registry.Register(messagedata.MeetingState{}, c.processEmptyFun)
	registry.Register(messagedata.RollCallCreate{}, c.processRollCallCreate)
	registry.Register(messagedata.RollCallOpen{}, c.processRollCallOpen)
	registry.Register(messagedata.RollCallReOpen{}, c.processRollCallOpen)
	registry.Register(messagedata.RollCallClose{}, c.processRollCallClose)
	registry.Register(messagedata.ElectionSetup{}, c.processElectionObject)
	registry.Register(messagedata.MessageWitness{}, c.processMessageWitness)

	return registry
}

// processLaoState processes a lao state action.
func (c *Channel) processLaoState(rawMessage message.Message, msgData interface{},
	sender socket.Socket) error {

	data, ok := msgData.(*messagedata.LaoState)
	if !ok {
		return xerrors.Errorf("message %v isn't a lao#state message", msgData)
	}

	c.log.Info().Msg("received a lao#state message")

	// Check if we have the update message
	msg, ok := c.inbox.GetMessage(data.ModificationID)

	if !ok {
		return answer.NewErrorf(-4, "cannot find lao/update_properties with ID: %s",
			data.ModificationID)
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
		return answer.NewErrorf(-4, "not enough witness signatures provided. Needed %d got %d",
			expected, match)
	}

	// Check if the signatures match
	for _, pair := range data.ModificationSignatures {
		err := schnorr.VerifyWithChecks(crypto.Suite, []byte(pair.Witness),
			[]byte(data.ModificationID), []byte(pair.Signature))
		if err != nil {
			return answer.NewErrorf(-4, "signature verfication failed for witness: %s",
				pair.Witness)
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

	err = compareLaoUpdateAndState(updateMsgData, *data)
	if err != nil {
		return xerrors.Errorf("failed to compare lao/update and existing state: %w", err)
	}

	return nil
}

// processRollCallCreate processes a roll call creation object.
func (c *Channel) processRollCallCreate(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.RollCallCreate)
	if !ok {
		return xerrors.Errorf("message %v isn't a rollcall#create message", msgData)
	}

	// Check that data is correct
	err := c.verifyMessageRollCallCreate(data)
	if err != nil {
		return xerrors.Errorf("invalid roll_call#create message: %v", err)
	}

	// Check that the ProposedEnd is greater than the ProposedStart
	if data.ProposedStart > data.ProposedEnd {
		return answer.NewErrorf(-4, "The field `proposed_start` is greater than the field "+
			"`proposed_end`: %d > %d", data.ProposedStart, data.ProposedEnd)
	}

	c.rollCall.id = string(data.ID)
	c.rollCall.state = Created

	return nil
}

// processRollCallOpen processes an open roll call object.
func (c *Channel) processRollCallOpen(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	_, ok := msgData.(*messagedata.RollCallOpen)
	if !ok {
		_, ok2 := msgData.(*messagedata.RollCallReOpen)
		if !ok2 {
			return xerrors.Errorf("message %v isn't a rollcall#open/reopen message", msgData)
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
		return answer.NewError(-1, "The field `opens` does not correspond to the id of "+
			"the previous roll call message")
	}

	c.rollCall.id = rollCallOpen.UpdateID
	c.rollCall.state = Open

	return nil
}

// processRollCallClose processes a close roll call message.
func (c *Channel) processRollCallClose(msg message.Message, msgData interface{},
	senderSocket socket.Socket) error {

	data, ok := msgData.(*messagedata.RollCallClose)
	if !ok {
		return xerrors.Errorf("message %v isn't a rollcall#close message", msgData)
	}

	// check that data is correct
	err := c.verifyMessageRollCallClose(data)
	if err != nil {
		return xerrors.Errorf("invalid roll_call#close message: %v", err)
	}

	if c.rollCall.state != Open {
		return answer.NewError(-1, "The roll call cannot be closed since it's not open")
	}

	if !c.rollCall.checkPrevID([]byte(data.Closes)) {
		return answer.NewError(-4, "The field `closes` does not correspond to the id of "+
			"the previous roll call message")
	}

	c.rollCall.id = data.UpdateID
	c.rollCall.state = Closed

	for _, attendee := range data.Attendees {
		c.attendees[attendee] = struct{}{}

		c.createChirpingChannel(attendee, senderSocket)

		c.reactions.AddAttendee(attendee)
	}

	return nil
}

// processElectionObject handles an election object.
func (c *Channel) processElectionObject(msg message.Message, msgData interface{},
	senderSocket socket.Socket) error {

	_, ok := msgData.(*messagedata.ElectionSetup)
	if !ok {
		return xerrors.Errorf("message %v isn't a election#setup message", msgData)
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

	err = c.createElection(msg, electionSetup, senderSocket)
	if err != nil {
		return xerrors.Errorf("failed to create election: %w", err)
	}

	c.log.Info().Msg("election created with success")

	return nil
}

// processMessageWitness handles a message object.
func (c *Channel) processMessageWitness(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	_, ok := msgData.(*messagedata.MessageWitness)
	if !ok {
		return xerrors.Errorf("message %v isn't a message#witness message", msgData)
	}

	var witnessData messagedata.MessageWitness

	err := msg.UnmarshalData(&witnessData)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal witness data: %v", err)
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, []byte(msg.Sender), []byte(witnessData.MessageID),
		[]byte(witnessData.Signature))
	if err != nil {
		return answer.NewError(-4, "invalid witness signature")
	}

	err = c.inbox.AddWitnessSignature(witnessData.MessageID, msg.Sender, witnessData.Signature)
	if err != nil {
		return xerrors.Errorf("failed to add witness signature: %w", err)
	}

	return nil
}

func (c *Channel) processEmptyFun(message.Message, interface{}, socket.Socket) error {
	return nil
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
		return answer.NewDuplicateResourceError("message already exists")
	}

	return nil
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *Channel) broadcastToAllClients(msg message.Message) error {
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
		return xerrors.Errorf("failed to marshal broadcast query: %v", err)
	}

	c.sockets.SendToAll(buf)

	return nil
}

// createGeneralChirpingChannel creates a new general chirping channel and returns it
func createGeneralChirpingChannel(laoID string, hub channel.HubFunctionalities,
	socket socket.Socket) *generalChirping.Channel {

	generalChannelPath := laoID + social + chirps
	generalChirpingChannel := generalChirping.NewChannel(generalChannelPath, hub, popstellar.Logger)
	hub.NotifyNewChannel(generalChannelPath, generalChirpingChannel, socket)

	log.Info().Msgf("storing new channel '%s' ", generalChannelPath)

	return generalChirpingChannel
}

func (c *Channel) createChirpingChannel(publicKey string, socket socket.Socket) {
	chirpingChannelPath := c.channelID + social + publicKey

	cha := chirp.NewChannel(chirpingChannelPath, publicKey, c.hub, c.general, popstellar.Logger)
	c.hub.NotifyNewChannel(chirpingChannelPath, cha, socket)
	log.Info().Msgf("storing new chirp channel (%s) for: '%s'", c.channelID, publicKey)
}

// createCoinChannel creates a coin channel to handle digital cash project
func (c *Channel) createCoinChannel(socket socket.Socket, log zerolog.Logger) {
	coinPath := fmt.Sprintf("%s/coin", c.channelID)
	coinCh := coin.NewChannel(coinPath, c.hub, log)
	c.hub.NotifyNewChannel(coinPath, coinCh, socket)
}

// createElection creates an election in the LAO.
func (c *Channel) createElection(msg message.Message,
	setupMsg messagedata.ElectionSetup, socket socket.Socket) error {

	// Check if the Lao ID of the message corresponds to the channel ID
	channelID := c.channelID[6:]
	if channelID != setupMsg.Lao {
		return answer.NewInvalidMessageFieldError("Lao ID of the message is %s, should be "+
			"equal to the channel ID %s", setupMsg.Lao, channelID)
	}

	// Compute the new election channel id
	channelPath := "/root/" + setupMsg.Lao + "/" + setupMsg.ID

	// Create the new election channel
	electionCh, err := election.NewChannel(channelPath, msg, setupMsg, c.attendees,
		c.hub, c.log, c.organizerPubKey)
	if err != nil {
		return xerrors.Errorf("failed to create the election: %v", err)
	}

	// Saving the election channel creation message on the lao channel
	c.inbox.StoreMessage(msg)

	// Add the new election channel to the organizerHub
	c.hub.NotifyNewChannel(channelPath, electionCh, socket)

	return nil
}

func compareLaoUpdateAndState(update messagedata.LaoUpdate, state messagedata.LaoState) error {
	if update.LastModified != state.LastModified {
		return answer.NewErrorf(-4, "mismatch between last modified: expected %d got %d",
			update.LastModified, state.LastModified)
	}

	if update.Name != state.Name {
		return answer.NewErrorf(-4, "mismatch between name: expected %s got %s",
			update.Name, state.Name)
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
		return answer.NewErrorf(-4, "mismatch between witness keys: expected %d keys to "+
			"match but %d matched", M, match)
	}

	return nil
}

func (c *Channel) createAndSendLAOGreet() error {
	// Marshalls the organizer's public key
	orgPkBuf, err := c.organizerPubKey.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal the organizer key: %v", err)
	}

	msgData := messagedata.LaoGreet{
		Object:   messagedata.LAOObject,
		Action:   messagedata.LAOActionGreet,
		LaoID:    c.extractLaoID(),
		Frontend: base64.URLEncoding.EncodeToString(orgPkBuf),
		Address:  fmt.Sprintf("wss://%s/server/client", c.hub.GetServerAddress()),
		Peers:    []messagedata.Peer{},
	}

	// Marshalls the message data
	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		return xerrors.Errorf("failed to marshal the data: %v", err)
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	// Marshalls the server public key
	pk := c.hub.GetPubKeyServ()
	skBuf, err := pk.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal the server key: %v", err)
	}

	// Sign the data
	signatureBuf, err := c.hub.Sign(dataBuf)
	if err != nil {
		return xerrors.Errorf("failed to sign the data: %v", err)
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	laoGreetMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(skBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	err = c.broadcastToAllClients(laoGreetMsg)
	if err != nil {
		return xerrors.Errorf("failed to broadcast greeting message: %v", err)
	}

	c.inbox.StoreMessage(laoGreetMsg)

	return nil
}

func (c *Channel) extractLaoID() string {
	return strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
}

// checkPrevID is a helper method which validates the roll call ID.
func (r *rollCall) checkPrevID(prevID []byte) bool {
	return string(prevID) == r.id
}
