package consensus

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/channel"
	"popstellar/channel/register"
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
	"reflect"
	"strconv"
	"sync"
	"time"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
)

const (
	msgID                       = "msg id"
	messageNotReceived          = "message doesn't correspond to any previously received message"
	messageStateInexistant      = "messageState with ID %s inexistant"
	consensusInstanceInexistant = "consensusInstance with ID %s inexistant"
	messageNotInCorrectPhase    = "consensus corresponding to the message hasn't entered"
)

// Channel defines a consensus channel
type Channel struct {
	sockets channel.Sockets

	inbox *inbox.Inbox

	// /root/<lao_id>/consensus
	channelID string

	hub channel.HubFunctionalities

	attendees map[string]struct{}

	log zerolog.Logger

	register register.MessageRegistry

	consensusInstances map[string]*ConsensusInstance
	messageStates      map[string]*MessageState
}

// Save the state of a consensus instance
type ConsensusInstance struct {
	sync.RWMutex
	id string

	timeout   chan string
	last_sent string

	proposed_try int64
	promised_try int64
	accepted_try int64

	accepted_value bool

	decided        bool
	decision       bool
	proposed_value bool

	promises []messagedata.ConsensusPromise
	accepts  []messagedata.ConsensusAccept
}

func (i *ConsensusInstance) startTimer() {

	lastSent := i.last_sent

	if lastSent == "learn" {
		return
	}

	select {
	case action := <-i.timeout:

	case <-time.After(time.Second):
		switch lastSent {
		case "elect-accept":
			byteMsg, err := c.createPrepareMessage(data.InstanceID,
				data.MessageID, consensusInstance)
			if err != nil {
				return xerrors.Errorf("failed to create consensus#prepare message: %v", err)
			}

			consensusInstance.last_sent = "prepare"
			err = c.publishNewMessage(consensusInstance, byteMsg)
			if err != nil {
				return xerrors.Errorf("failed to send new consensus#prepare message: %v", err)
			}
		case "prepare":
		case "promise":
		case "propose":
		case "accept":

		}
	}
}

// State of a consensus by messageID, used when two messages on the same object
// happens
type MessageState struct {
	sync.Mutex

	currentPhase      Phase
	proposer          kyber.Point
	electAcceptNumber int
}

type Phase int

const (
	ElectAcceptPhase Phase = 1
	PromisePhase     Phase = 2
	AcceptPhase      Phase = 3
)

// NewChannel returns a new initialized consensus channel
func NewChannel(channelID string, hub channel.HubFunctionalities, log zerolog.Logger) channel.Channel {
	inbox := inbox.NewInbox(channelID)

	log = log.With().Str("channel", "consensus").Logger()

	newChannel := &Channel{
		sockets:            channel.NewSockets(),
		inbox:              inbox,
		channelID:          channelID,
		hub:                hub,
		attendees:          make(map[string]struct{}),
		log:                log,
		consensusInstances: make(map[string]*ConsensusInstance),
		messageStates:      make(map[string]*MessageState),
	}

	newChannel.register = newChannel.NewConsensusRegistry()

	return newChannel
}

// NewConsensusRegistry creates a new registry for the consensus channel
func (c *Channel) NewConsensusRegistry() register.MessageRegistry {
	registry := register.NewMessageRegistry()

	registry.Register("elect", c.processConsensusElect, messagedata.ConsensusElect{})
	registry.Register("elect-accept", c.processConsensusElectAccept, messagedata.ConsensusElectAccept{})
	registry.Register("prepare", c.processConsensusPrepare, messagedata.ConsensusPrepare{})
	registry.Register("promise", c.processConsensusPromise, messagedata.ConsensusPromise{})
	registry.Register("propose", c.processConsensusPropose, messagedata.ConsensusPropose{})
	registry.Register("accept", c.processConsensusAccept, messagedata.ConsensusAccept{})
	registry.Register("learn", c.processConsensusLearn, messagedata.ConsensusLearn{})

	return registry
}

// Subscribe is used to handle a subscribe message from the client
func (c *Channel) Subscribe(sock socket.Socket, msg method.Subscribe) error {
	c.log.Info().Str(msgID, strconv.Itoa(msg.ID)).Msg("received a subscribe")

	c.sockets.Upsert(sock)

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *Channel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	c.log.Info().Str(msgID, strconv.Itoa(msg.ID)).Msg("received an unsubscribe")

	ok := c.sockets.Delete(socketID)
	if !ok {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	c.log.Info().Str(msgID, strconv.Itoa(catchup.ID)).Msg("received a catchup")

	return c.inbox.GetSortedMessages()
}

// Broadcast is used to handle a broadcast message.
func (c *Channel) Broadcast(msg method.Broadcast) error {
	err := xerrors.Errorf("a consensus channel shouldn't need to broadcast a message")
	c.log.Err(err)
	return err
}

// broadcastToAllWitnesses is a helper message to broadcast a message to all
// witnesses.
func (c *Channel) broadcastToAllClients(msg message.Message) error {
	c.log.Info().Str(msgID, msg.MessageID).Msg("broadcasting message to all witnesses")

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

// Publish handles publish messages for the consensus channel
func (c *Channel) Publish(publish method.Publish, _ socket.Socket) error {
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
	if err != nil {
		return xerrors.Errorf("failed to get object or action: %v", err)
	}

	switch object {
	case messagedata.ConsensusObject:
		err = c.processConsensusObject(action, msg)
	default:
		return answer.NewInvalidObjectError(object)
	}

	if err != nil {
		return xerrors.Errorf("failed to process %q object: %w", object, err)
	}

	err = c.broadcastToAllClients(msg)
	if err != nil {
		return xerrors.Errorf("failed to broadcast message: %v", err)
	}
	return nil
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

// processConsensusObject processes a Consensus Object.
func (c *Channel) processConsensusObject(action string, msg message.Message) error {

	data, found := c.register.Registry[action]
	if !found {
		return xerrors.Errorf("action '%s' not found", action)
	}

	concreteType := reflect.New(reflect.ValueOf(data.ConcreteType).Type()).Interface()

	err := msg.UnmarshalData(&concreteType)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal data: %v", err)
	}

	err = data.Callback(msg, concreteType)
	if err != nil {
		return xerrors.Errorf("failed to process action '%s': %v", action, err)
	}

	return nil
}

// getSender unmarshal the sender from a message.Message
func getSender(msg message.Message) (kyber.Point, error) {
	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return nil, xerrors.Errorf("failed to decode sender key: %v", err)
	}

	// Unmarshal sender of the message, used to know who is the propose
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return nil, answer.NewErrorf(-4, "failed to unmarshal public key of the sender: %v", err)
	}

	return senderPoint, nil
}

// createConsensusInstance adds a new consensus instance to the
// consensusInstances array
func (c *Channel) createConsensusInstance(instanceID string) {
	c.consensusInstances[instanceID] = &ConsensusInstance{

		id: instanceID,

		timeout:   make(chan string, 1),
		last_sent: "",

		proposed_try: 0,
		promised_try: -1,
		accepted_try: -1,

		accepted_value: false,
		decided:        false,
		decision:       false,
		proposed_value: false,

		promises: make([]messagedata.ConsensusPromise, 0),
		accepts:  make([]messagedata.ConsensusAccept, 0),
	}
}

// createMessageInstance creates a new message instance to the messageStates
// array
func (c *Channel) createMessageInstance(messageID string, proposer kyber.Point) {
	newMessageState := MessageState{
		currentPhase:      ElectAcceptPhase,
		proposer:          proposer,
		electAcceptNumber: 0,
	}

	c.messageStates[messageID] = &newMessageState
}

// processConsensusElect processes an elect action.
func (c *Channel) processConsensusElect(message message.Message, msgData interface{}) error {

	data, ok := msgData.(*messagedata.ConsensusElect)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#elect message", msgData)
	}

	c.log.Info().Msg("received a consensus#elect message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#elect message: %v", err)
	}

	// Creates a consensus instance if there is none on the object
	_, ok = c.consensusInstances[data.InstanceID]
	if !ok {
		c.createConsensusInstance(data.InstanceID)
	}

	sender, err := getSender(message)
	if err != nil {
		return xerrors.Errorf("failed to get consensus#elect message sender: %v", err)
	}

	c.createMessageInstance(message.MessageID, sender)

	return nil
}

// processConsensusElectAccept processes an elect accept action.
func (c *Channel) processConsensusElectAccept(_ message.Message, msgData interface{}) error {

	data, ok := msgData.(*messagedata.ConsensusElectAccept)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#elect-accept message", msgData)
	}

	c.log.Info().Msg("received a consensus#elect-accept message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#elect-accept message: %v", err)
	}

	// check whether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf(messageNotReceived)
	}

	messageState, ok := c.messageStates[data.MessageID]
	if !ok {
		return xerrors.Errorf(messageStateInexistant, data.MessageID)
	}
	messageState.Lock()
	defer messageState.Unlock()
	if data.Accept {
		messageState.electAcceptNumber += 1
	}

	// Once all Elect_Accept have been received, proposer creates new prepare
	// message
	if messageState.electAcceptNumber < c.hub.GetServerNumber() {
		return nil
	}
	if messageState.currentPhase != ElectAcceptPhase ||
		!messageState.proposer.Equal(c.hub.GetPubKeyOrg()) {
		return nil
	}

	consensusInstance, ok := c.consensusInstances[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInstanceInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	if consensusInstance.proposed_try >= consensusInstance.promised_try {
		consensusInstance.proposed_try += 1
	} else {
		consensusInstance.proposed_try = consensusInstance.promised_try + 1
	}

	// For now the consensus always accept a true if it complete
	consensusInstance.proposed_value = true

	byteMsg, err := c.createPrepareMessage(data.InstanceID,
		data.MessageID, consensusInstance)
	if err != nil {
		return xerrors.Errorf("failed to create consensus#prepare message: %v", err)
	}

	consensusInstance.last_sent = "prepare"
	err = c.publishNewMessage(consensusInstance, byteMsg)
	if err != nil {
		return xerrors.Errorf("failed to send new consensus#prepare message: %v", err)
	}

	return nil
}

// createPrepareMessage creates the data for a new prepare message
func (c *Channel) createPrepareMessage(instanceID string, messageID string,
	consensusInstance *ConsensusInstance) ([]byte, error) {

	newData := messagedata.ConsensusPrepare{
		Object:     "consensus",
		Action:     "prepare",
		InstanceID: instanceID,
		MessageID:  messageID,

		CreatedAt: time.Now().Unix(),

		Value: messagedata.ValuePrepare{
			ProposedTry: consensusInstance.proposed_try,
		},
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#prepare message: %v", err)
	}

	return byteMsg, nil
}

// processConsensusPrepare processes a prepare action.
func (c *Channel) processConsensusPrepare(_ message.Message, msgData interface{}) error {

	data, ok := msgData.(*messagedata.ConsensusPrepare)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#prepare message", msgData)
	}

	c.log.Info().Msg("received a consensus#prepare message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#prepare message: %v", err)
	}

	// check whether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)
	if !valid {
		return xerrors.Errorf(messageNotReceived)
	}

	messageState, ok := c.messageStates[data.MessageID]
	if !ok {
		return xerrors.Errorf(messageStateInexistant, data.MessageID)
	}
	messageState.Lock()
	defer messageState.Unlock()

	messageState.currentPhase = PromisePhase

	consensusInstance, ok := c.consensusInstances[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInstanceInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	if consensusInstance.promised_try >= data.Value.ProposedTry {
		return nil
	}

	consensusInstance.promised_try = data.Value.ProposedTry

	byteMsg, err := c.createPromiseMessage(data.InstanceID,
		data.MessageID, consensusInstance)
	if err != nil {
		return xerrors.Errorf("failed to create consensus#promise message, %v", err)
	}

	consensusInstance.last_sent = "promise"
	err = c.publishNewMessage(consensusInstance, byteMsg)
	if err != nil {
		return err
	}

	return nil
}

// createPromiseMessage creates the data for a new prepare message
func (c *Channel) createPromiseMessage(instanceID string, messageID string,
	consensusInstance *ConsensusInstance) ([]byte, error) {

	newData := messagedata.ConsensusPromise{
		Object:     "consensus",
		Action:     "promise",
		InstanceID: instanceID,
		MessageID:  messageID,

		CreatedAt: time.Now().Unix(),

		Value: messagedata.ValuePromise{
			AcceptedTry:   consensusInstance.accepted_try,
			AcceptedValue: consensusInstance.accepted_value,
			PromisedTry:   consensusInstance.promised_try,
		},
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#promise message: %v", err)
	}

	return byteMsg, nil
}

// processConsensusPromise processes a promise action.
func (c *Channel) processConsensusPromise(_ message.Message, msgData interface{}) error {

	data, ok := msgData.(*messagedata.ConsensusPromise)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#promise message", msgData)
	}

	c.log.Info().Msg("received a consensus#promise message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#promise message: %v", err)
	}

	// check whether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)
	if !valid {
		return xerrors.Errorf(messageNotReceived)
	}

	messageState, ok := c.messageStates[data.MessageID]
	if !ok {
		return xerrors.Errorf(messageStateInexistant, data.MessageID)
	}
	messageState.Lock()
	defer messageState.Unlock()

	if messageState.currentPhase < PromisePhase {
		return xerrors.Errorf(messageNotInCorrectPhase + " the promise phase")
	}

	consensusInstance, ok := c.consensusInstances[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInstanceInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	consensusInstance.promises = append(consensusInstance.promises, *data)

	// if enough Promise messages are received, the proposer send a Propose message
	if len(consensusInstance.promises) < c.hub.GetServerNumber()/2+1 {
		return nil
	}

	if messageState.currentPhase != PromisePhase ||
		!messageState.proposer.Equal(c.hub.GetPubKeyOrg()) {
		return nil
	}

	highestAccepted := int64(-1)
	highestAcceptedValue := true
	for _, promise := range consensusInstance.promises {
		if promise.Value.AcceptedTry > highestAccepted {
			highestAccepted = promise.Value.AcceptedTry
			highestAcceptedValue = promise.Value.AcceptedValue
		}
	}

	byteMsg, err := c.createProposeMessage(data.InstanceID, data.MessageID,
		consensusInstance, highestAccepted, highestAcceptedValue)
	if err != nil {
		return xerrors.Errorf("failed to create consensus#propose message: %v", err)
	}

	consensusInstance.last_sent = "propose"
	err = c.publishNewMessage(consensusInstance, byteMsg)
	if err != nil {
		return err
	}

	return nil
}

// createProposeMessage creates the data for a new propose message
func (c *Channel) createProposeMessage(instanceID string, messageID string, consensusInstance *ConsensusInstance,
	highestAccepted int64, highestValue bool) ([]byte, error) {

	newData := messagedata.ConsensusPropose{
		Object:     "consensus",
		Action:     "propose",
		InstanceID: instanceID,
		MessageID:  messageID,

		CreatedAt: time.Now().Unix(),

		Value: messagedata.ValuePropose{
			ProposedValue: highestValue,
		},

		AcceptorSignatures: make([]string, 0),
	}

	if highestAccepted == -1 {
		newData.Value.ProposedTry = consensusInstance.proposed_try
	} else {
		newData.Value.ProposedTry = highestAccepted
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#propose message: %v", err)
	}

	return byteMsg, nil
}

// processConsensusPropose processes a propose action.
func (c *Channel) processConsensusPropose(_ message.Message, msgData interface{}) error {

	data, ok := msgData.(*messagedata.ConsensusPropose)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#propose message", msgData)
	}

	c.log.Info().Msg("received a consensus#propose message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#propose message: %v", err)
	}

	// check whether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf(messageNotReceived)
	}

	messageState, ok := c.messageStates[data.MessageID]
	if !ok {
		return xerrors.Errorf(messageStateInexistant, data.MessageID)
	}
	messageState.Lock()
	defer messageState.Unlock()

	if messageState.currentPhase < PromisePhase {
		return xerrors.Errorf(messageNotInCorrectPhase + " the promise phase")
	}

	messageState.currentPhase = AcceptPhase

	consensusInstance, ok := c.consensusInstances[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInstanceInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	// If the server has no client subscribed to the consensus channel, it
	// doesn't take part in it
	if c.sockets.Len() == 0 {
		return nil
	}

	if consensusInstance.promised_try > data.Value.ProposedTry {
		return nil
	}

	consensusInstance.accepted_try = data.Value.ProposedTry
	consensusInstance.accepted_value = data.Value.ProposedValue

	byteMsg, err := c.createAcceptMessage(data.InstanceID,
		data.MessageID, consensusInstance)
	if err != nil {
		return xerrors.Errorf("failed to create consensus#accept message: %v", err)
	}

	consensusInstance.last_sent = "accept"
	err = c.publishNewMessage(consensusInstance, byteMsg)
	if err != nil {
		return err
	}

	return nil
}

// createAcceptMessage creates the data for a new accept message
func (c *Channel) createAcceptMessage(instanceID string, messageID string,
	consensusInstance *ConsensusInstance) ([]byte, error) {

	newData := messagedata.ConsensusAccept{
		Object:     "consensus",
		Action:     "accept",
		InstanceID: instanceID,
		MessageID:  messageID,

		CreatedAt: time.Now().Unix(),

		Value: messagedata.ValueAccept{
			AcceptedTry:   consensusInstance.accepted_try,
			AcceptedValue: consensusInstance.accepted_value,
		},
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#accept message: %v", err)
	}

	return byteMsg, nil
}

// processConsensusAccept proccesses an accept action.
func (c *Channel) processConsensusAccept(_ message.Message, msgData interface{}) error {

	data, ok := msgData.(*messagedata.ConsensusAccept)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#accept message", msgData)
	}

	c.log.Info().Msg("received a consensus#accept message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#accept message: %v", err)
	}

	// check whether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf(messageNotReceived)
	}

	messageState, ok := c.messageStates[data.MessageID]
	if !ok {
		return xerrors.Errorf(messageStateInexistant, data.MessageID)
	}
	messageState.Lock()
	defer messageState.Unlock()

	if messageState.currentPhase < AcceptPhase {
		return xerrors.Errorf(messageNotInCorrectPhase + " the accept phase")
	}

	consensusInstance, ok := c.consensusInstances[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInstanceInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	if data.Value.AcceptedTry == consensusInstance.proposed_try &&
		data.Value.AcceptedValue == consensusInstance.proposed_value {
		consensusInstance.accepts = append(consensusInstance.accepts, *data)
	}

	if len(consensusInstance.accepts) < c.hub.GetServerNumber()/2+1 {
		return nil
	}
	if !messageState.proposer.Equal(c.hub.GetPubKeyOrg()) {
		return nil
	}

	if consensusInstance.decided {
		return nil
	}

	consensusInstance.decided = true
	consensusInstance.decision = consensusInstance.proposed_value

	byteMsg, err := c.createLearnMessage(data.InstanceID,
		data.MessageID, consensusInstance)
	if err != nil {
		return xerrors.Errorf("failed to create new consensus#learn message")
	}

	consensusInstance.last_sent = "learn"
	err = c.publishNewMessage(consensusInstance, byteMsg)
	if err != nil {
		return err
	}

	return nil
}

// createLearnMessage creates the data for a learn message
func (c *Channel) createLearnMessage(instanceID string, messageID string,
	consensusInstance *ConsensusInstance) ([]byte, error) {

	newData := messagedata.ConsensusLearn{
		Object:     "consensus",
		Action:     "learn",
		InstanceID: instanceID,
		MessageID:  messageID,

		CreatedAt: time.Now().Unix(),

		Value: messagedata.ValueLearn{
			Decision: consensusInstance.decision,
		},

		AcceptorSignatures: make([]string, 0),
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#promise message: %v", err)
	}

	return byteMsg, nil
}

// processConsensusLearn processes a learn action.
func (c *Channel) processConsensusLearn(_ message.Message, msgData interface{}) error {

	data, ok := msgData.(*messagedata.ConsensusLearn)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#learn message", msgData)
	}

	c.log.Info().Msg("received a consensus#learn message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#learn message: %v", err)
	}

	// check whether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any received message")
	}

	consensusInstance, ok := c.consensusInstances[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInstanceInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	if !consensusInstance.decided {
		consensusInstance.decided = true
		consensusInstance.decision = data.Value.Decision
	}

	return nil
}

// publishNewMessage send a publish message on the current channel
func (c *Channel) publishNewMessage(consensusInstance *ConsensusInstance, byteMsg []byte) error {

	encryptedMsg := base64.URLEncoding.EncodeToString(byteMsg)

	publicKey := c.hub.GetPubKeyServ()
	pkBuf, err := publicKey.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal the public key: %v", err)
	}

	encryptedKey := base64.URLEncoding.EncodeToString(pkBuf)

	signatureBuf, err := c.hub.Sign(byteMsg)
	if err != nil {
		return xerrors.Errorf("failed to sign the data: %v", err)
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	messageID := messagedata.Hash(encryptedMsg, signature)

	msg := message.Message{
		Data:              encryptedMsg,
		Sender:            encryptedKey,
		Signature:         signature,
		MessageID:         messageID,
		WitnessSignatures: make([]message.WitnessSignature, 0),
	}

	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},

		Params: struct {
			Channel string          "json:\"channel\""
			Message message.Message "json:\"message\""
		}{
			Channel: c.channelID,
			Message: msg,
		},
	}

	c.hub.SetMessageID(&publish)

	go consensusInstance.startTimer()
	err = c.hub.SendAndHandleMessage(publish)
	if err != nil {
		return xerrors.Errorf("failed to send new message: %v", err)
	}

	return nil
}
