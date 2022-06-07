package consensus

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/channel"
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
	"sync"
	"time"

	"github.com/benbjohnson/clock"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
)

const (
	msgID = "msg id"

	messageNotReceived  = "message doesn't correspond to any previously received message"
	consensusInexistant = "consensusInstance with ID %s inexistant"
	consensusFinished   = "consensus with id %s already finished"

	failedFailureCreation = "failed to create failure message"
	failedFailureSending  = "failed to send failure message"

	proposerRole = "proposer"
	acceptorRole = "acceptor"

	fastTimeout = 3 * time.Second
	// timeout fot the elect-accept messages
	slowTimeout = 5 * time.Minute
)

// Channel defines a consensus channel
type Channel struct {
	sync.Mutex

	clock clock.Clock

	sockets channel.Sockets

	inbox *inbox.Inbox

	// /root/<lao_id>/consensus
	channelID string

	organizerPubKey kyber.Point

	hub channel.HubFunctionalities

	attendees map[string]struct{}

	log zerolog.Logger

	registry registry.MessageRegistry

	consensusInstances struct {
		sync.Mutex
		m map[string]*ConsensusInstance
	}
}

// ConsensusInstance saves the state of a consensus instance
type ConsensusInstance struct {
	sync.RWMutex
	id string

	role string

	lastSent string

	proposedTry int64
	promisedTry int64
	acceptedTry int64

	proposedValue bool
	acceptedValue bool

	decided  bool
	decision bool

	promises map[string]messagedata.ConsensusPromise
	accepts  map[string]messagedata.ConsensusAccept

	electInstances map[string]*ElectInstance
}

// ElectInstance stores the state of a consensus dependent on the elect message
type ElectInstance struct {
	timeoutChan chan string

	acceptorNumber int

	failed bool

	positiveAcceptors map[string]int
	negativeAcceptors map[string]int
}

// NewChannel returns a new initialized consensus channel
func NewChannel(channelID string, hub channel.HubFunctionalities,
	log zerolog.Logger, organizerPubKey kyber.Point) channel.Channel {

	inbox := inbox.NewInbox(channelID)

	log = log.With().Str("channel", "consensus").Logger()

	newChannel := &Channel{
		clock:           clock.New(),
		sockets:         channel.NewSockets(),
		inbox:           inbox,
		channelID:       channelID,
		organizerPubKey: organizerPubKey,
		hub:             hub,
		attendees:       make(map[string]struct{}),
		log:             log,
		consensusInstances: struct {
			sync.Mutex
			m map[string]*ConsensusInstance
		}{
			sync.Mutex{},
			make(map[string]*ConsensusInstance),
		},
	}

	newChannel.registry = newChannel.NewConsensusRegistry()

	return newChannel
}

// ---
// Publish-subscribe / channel.Channel implementation
// ---

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

// Publish handles publish messages for the consensus channel
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
	c.log.Info().Str(msgID, strconv.Itoa(catchup.ID)).Msg("received a catchup")

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

// NewConsensusRegistry creates a new registry for the consensus channel
func (c *Channel) NewConsensusRegistry() registry.MessageRegistry {
	registry := registry.NewMessageRegistry()

	registry.Register(messagedata.ConsensusElect{}, c.processConsensusElect)
	registry.Register(messagedata.ConsensusElectAccept{}, c.processConsensusElectAccept)
	registry.Register(messagedata.ConsensusPrepare{}, c.processConsensusPrepare)
	registry.Register(messagedata.ConsensusPromise{}, c.processConsensusPromise)
	registry.Register(messagedata.ConsensusPropose{}, c.processConsensusPropose)
	registry.Register(messagedata.ConsensusAccept{}, c.processConsensusAccept)
	registry.Register(messagedata.ConsensusLearn{}, c.processConsensusLearn)
	registry.Register(messagedata.ConsensusFailure{}, c.processConsensusFailure)

	return registry
}

// processConsensusElect processes an elect action.
func (c *Channel) processConsensusElect(message message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.ConsensusElect)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#elect message", msgData)
	}

	c.log.Info().Msg("received a consensus#elect message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#elect message: %v", err)
	}

	sender, err := getSender(message)
	if err != nil {
		return xerrors.Errorf("failed to get consensus#elect message sender: %v", err)
	}

	// Creates a consensus instance if it doesn't exist yet
	c.consensusInstances.Lock()
	consensusInstance, ok := c.consensusInstances.m[data.InstanceID]
	if !ok {
		consensusInstance = c.createConsensusInstance(data.InstanceID)
	}
	c.consensusInstances.Unlock()

	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	consensusInstance.createElectInstance(message.MessageID, c.hub.GetServerNumber())

	// TODO
	if sender.Equal(c.organizerPubKey) {
		consensusInstance.role = proposerRole
	}

	consensusInstance.lastSent = messagedata.ConsensusActionElectAccept

	// Start a channel linked to the message id
	go c.startTimer(consensusInstance, message.MessageID)

	return nil
}

// processConsensusElectAccept processes an elect accept action.
func (c *Channel) processConsensusElectAccept(message message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.ConsensusElectAccept)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#elect_accept message", msgData)
	}

	c.log.Info().Msg("received a consensus#elect_accept message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#elect_accept message: %v", err)
	}

	// check whether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)
	if !valid {
		return xerrors.Errorf(messageNotReceived)
	}

	consensusInstance, ok := c.consensusInstances.m[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	electInstance := consensusInstance.electInstances[data.MessageID]

	if consensusInstance.decided || electInstance.failed {
		return xerrors.Errorf(consensusFinished, data.InstanceID)
	}

	err = electInstance.updateAcceptors(message.Sender, data.Accept)
	if err != nil {
		return xerrors.Errorf("failed to update acceptors: %v", err)
	}

	nextMessage := c.nextMessage(consensusInstance, data.MessageID)
	if nextMessage == "" {
		return nil
	}

	if nextMessage == messagedata.ConsensusActionFailure {
		return c.electAcceptFailure(consensusInstance, data.MessageID)
	}

	electInstance.timeoutChan <- messagedata.ConsensusActionElectAccept

	if consensusInstance.proposedTry >= consensusInstance.promisedTry {
		consensusInstance.proposedTry++
	} else {
		consensusInstance.proposedTry = consensusInstance.promisedTry + 1
	}

	// For now the consensus always accept a true if it complete
	consensusInstance.proposedValue = true

	byteMsg, err := c.createPrepareMessage(consensusInstance, data.MessageID)
	if err != nil {
		return xerrors.Errorf("failed to create consensus#prepare message: %v", err)
	}

	consensusInstance.lastSent = messagedata.ConsensusActionPrepare

	err = c.publishNewMessage(byteMsg)
	if err != nil {
		return xerrors.Errorf("failed to send new consensus#prepare message: %v", err)
	}

	return nil
}

// processConsensusPrepare processes a prepare action.
func (c *Channel) processConsensusPrepare(_ message.Message, msgData interface{},
	_ socket.Socket) error {

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

	consensusInstance, ok := c.consensusInstances.m[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	electInstance := consensusInstance.electInstances[data.MessageID]

	if consensusInstance.decided || electInstance.failed {
		return xerrors.Errorf(consensusFinished, data.InstanceID)
	}

	electInstance.timeoutChan <- messagedata.ConsensusActionPrepare

	if consensusInstance.promisedTry >= data.Value.ProposedTry {
		return nil
	}

	consensusInstance.promisedTry = data.Value.ProposedTry

	byteMsg, err := c.createPromiseMessage(consensusInstance, data.MessageID)
	if err != nil {
		return xerrors.Errorf("failed to create consensus#promise message, %v", err)
	}

	// The proposer will timeout as if having last sent a prepare message
	if consensusInstance.role == acceptorRole {
		consensusInstance.lastSent = messagedata.ConsensusActionPromise
	}

	err = c.publishNewMessage(byteMsg)
	if err != nil {
		return err
	}

	return nil
}

// processConsensusPromise processes a promise action.
func (c *Channel) processConsensusPromise(msg message.Message, msgData interface{},
	_ socket.Socket) error {

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

	consensusInstance, ok := c.consensusInstances.m[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	electInstance := consensusInstance.electInstances[data.MessageID]

	if consensusInstance.decided || electInstance.failed {
		return xerrors.Errorf(consensusFinished, data.InstanceID)
	}

	consensusInstance.promises[msg.Signature] = *data

	// if enough Promise messages are received, the proposer send a Propose message
	if len(consensusInstance.promises) < electInstance.acceptorNumber/2+1 {
		return nil
	}

	if consensusInstance.role != proposerRole {
		return nil
	}

	if consensusInstance.lastSent != messagedata.ConsensusActionPrepare {
		return nil
	}

	electInstance.timeoutChan <- messagedata.ConsensusActionPromise

	highestAccepted := int64(-1)
	highestAcceptedValue := true
	for _, promise := range consensusInstance.promises {
		if promise.Value.AcceptedTry > highestAccepted {
			highestAccepted = promise.Value.AcceptedTry
			highestAcceptedValue = promise.Value.AcceptedValue
		}
	}

	byteMsg, err := c.createProposeMessage(consensusInstance, data.MessageID,
		highestAccepted, highestAcceptedValue)
	if err != nil {
		return xerrors.Errorf("failed to create consensus#propose message: %v", err)
	}

	consensusInstance.lastSent = messagedata.ConsensusActionPropose

	err = c.publishNewMessage(byteMsg)
	if err != nil {
		return err
	}

	return nil
}

// processConsensusPropose processes a propose action.
func (c *Channel) processConsensusPropose(_ message.Message, msgData interface{},
	_ socket.Socket) error {

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

	consensusInstance, ok := c.consensusInstances.m[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	electInstance := consensusInstance.electInstances[data.MessageID]

	if consensusInstance.decided || electInstance.failed {
		return xerrors.Errorf(consensusFinished, data.InstanceID)
	}

	electInstance.timeoutChan <- messagedata.ConsensusActionPropose

	// If the server has no client subscribed to the consensus channel, it
	// doesn't take part in it
	if c.sockets.Len() == 0 {
		return nil
	}

	if consensusInstance.promisedTry > data.Value.ProposedTry {
		return nil
	}

	consensusInstance.acceptedTry = data.Value.ProposedTry
	consensusInstance.acceptedValue = data.Value.ProposedValue

	byteMsg, err := c.createAcceptMessage(consensusInstance, data.MessageID)
	if err != nil {
		return xerrors.Errorf("failed to create consensus#accept message: %v", err)
	}

	// The proposer will timeout as if having last sent a propose message
	if consensusInstance.role == acceptorRole {
		consensusInstance.lastSent = messagedata.ConsensusActionAccept
	}

	err = c.publishNewMessage(byteMsg)
	if err != nil {
		return err
	}

	return nil
}

// processConsensusAccept proccesses an accept action.
func (c *Channel) processConsensusAccept(msg message.Message, msgData interface{},
	_ socket.Socket) error {

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

	consensusInstance, ok := c.consensusInstances.m[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	electInstance := consensusInstance.electInstances[data.MessageID]

	if consensusInstance.decided || electInstance.failed {
		return xerrors.Errorf(consensusFinished, data.InstanceID)
	}

	if data.Value.AcceptedTry == consensusInstance.proposedTry &&
		data.Value.AcceptedValue == consensusInstance.proposedValue {
		consensusInstance.accepts[msg.Signature] = *data
	}

	if len(consensusInstance.accepts) < electInstance.acceptorNumber/2+1 {
		return nil
	}

	if consensusInstance.role != proposerRole {
		return nil
	}

	if consensusInstance.decided {
		return nil
	}

	if consensusInstance.lastSent != messagedata.ConsensusActionPropose {
		return nil
	}

	electInstance.timeoutChan <- messagedata.ConsensusActionAccept

	consensusInstance.decided = true
	consensusInstance.decision = consensusInstance.proposedValue

	byteMsg, err := c.createLearnMessage(consensusInstance, data.MessageID)
	if err != nil {
		return xerrors.Errorf("failed to create new consensus#learn message")
	}

	consensusInstance.lastSent = messagedata.ConsensusActionLearn
	err = c.publishNewMessage(byteMsg)
	if err != nil {
		return err
	}

	return nil
}

// processConsensusLearn processes a learn action.
func (c *Channel) processConsensusLearn(_ message.Message, msgData interface{},
	_ socket.Socket) error {

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

	consensusInstance, ok := c.consensusInstances.m[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	electInstance := consensusInstance.electInstances[data.MessageID]

	if consensusInstance.decided || electInstance.failed {
		return nil
	}
	electInstance.timeoutChan <- messagedata.ConsensusActionLearn

	consensusInstance.decided = true
	consensusInstance.decision = data.Value.Decision

	return nil
}

// processConsensusFailure processes a failure action
func (c *Channel) processConsensusFailure(_ message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.ConsensusFailure)
	if !ok {
		return xerrors.Errorf("message %v isn't a consensus#failure message", msgData)
	}

	c.log.Info().Msg("received a consensus#failure message")

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#failure message: %v", err)
	}

	// check whether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any received message")
	}

	consensusInstance, ok := c.consensusInstances.m[data.InstanceID]
	if !ok {
		return xerrors.Errorf(consensusInexistant, data.InstanceID)
	}
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	electInstance := consensusInstance.electInstances[data.MessageID]

	if electInstance.failed || consensusInstance.decided {
		return nil
	}

	electInstance.timeoutChan <- messagedata.ConsensusActionFailure

	electInstance.failed = true

	return nil
}

// verifyMessage checks if a message in a Publish or Broadcast method is valid
func (c *Channel) verifyMessage(msg message.Message) error {
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

// broadcastToAllClients is a helper message to broadcast a message to all
// clients.
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

// nextMessage verifies if a prepare or failure message should be sent
func (c *Channel) nextMessage(i *ConsensusInstance, messageID string) string {
	electInstance := i.electInstances[messageID]

	if i.role != proposerRole {
		return ""
	}

	// If enough rejection, failure of the consensus
	if len(electInstance.negativeAcceptors) >= (electInstance.acceptorNumber/2 + 1) {
		return messagedata.ConsensusActionFailure
	}

	// If enough acception, go to next step of the consensus
	if len(electInstance.positiveAcceptors) >= (electInstance.acceptorNumber/2 + 1) {
		return messagedata.ConsensusActionPrepare
	}

	return ""
}

// publishNewMessage send a publish message on the current channel
func (c *Channel) publishNewMessage(byteMsg []byte) error {
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

	broadcast := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "broadcast",
		},

		Params: struct {
			Channel string          "json:\"channel\""
			Message message.Message "json:\"message\""
		}{
			Channel: c.channelID,
			Message: msg,
		},
	}

	err = c.hub.SendAndHandleMessage(broadcast)
	if err != nil {
		return xerrors.Errorf("failed to send new message: %v", err)
	}

	return nil
}

// createElectInstance creates the state of the consensus for a specific elect
// message
func (i *ConsensusInstance) createElectInstance(messageID string, acceptorNumber int) {
	i.electInstances[messageID] = &ElectInstance{
		timeoutChan: make(chan string),

		failed: false,

		positiveAcceptors: make(map[string]int),
		negativeAcceptors: make(map[string]int),

		acceptorNumber: acceptorNumber,
	}
}

// createConsensusInstance adds a new consensus instance to the
// consensusInstances array
func (c *Channel) createConsensusInstance(instanceID string) *ConsensusInstance {
	consensusInstance := &ConsensusInstance{

		id: instanceID,

		role: acceptorRole,

		lastSent: "",

		proposedTry: 0,
		promisedTry: -1,
		acceptedTry: -1,

		acceptedValue: false,
		proposedValue: false,

		decided:  false,
		decision: false,

		promises: make(map[string]messagedata.ConsensusPromise),
		accepts:  make(map[string]messagedata.ConsensusAccept),

		electInstances: make(map[string]*ElectInstance),
	}

	c.consensusInstances.m[instanceID] = consensusInstance

	return consensusInstance
}

// updateAcceptors updates the acceptors of an electInstance
func (e *ElectInstance) updateAcceptors(sender string, accept bool) error {
	_, ok := e.positiveAcceptors[sender]
	if ok {
		return xerrors.Errorf("Acceptor %s already accepted this value", sender)
	}

	_, ok = e.negativeAcceptors[sender]
	if ok {
		return xerrors.Errorf("Acceptor %s already refused this value", sender)
	}

	// Update the elect state
	if accept {
		e.positiveAcceptors[sender] = 0
	} else {
		e.negativeAcceptors[sender] = 0
	}

	return nil
}

// electAcceptFailure sends a failure message when consensus is refused
func (c *Channel) electAcceptFailure(instance *ConsensusInstance, messageID string) error {
	byteMsg, err := c.createFailureMessage(instance, messageID)
	if err != nil {
		return xerrors.Errorf("failed to create consensus#failure message: %v", err)
	}

	err = c.publishNewMessage(byteMsg)
	if err != nil {
		return xerrors.Errorf("failed to send new consensus#failure message: %v", err)
	}

	return nil
}

// startTimer starts the timeout logic for the consensus
func (c *Channel) startTimer(instance *ConsensusInstance, messageID string) {
	timeoutChan := instance.electInstances[messageID].timeoutChan

	for {
		select {
		case action := <-timeoutChan:
			switch action {
			// Stop the timer when receiving one action finishing it
			case messagedata.ConsensusActionLearn,
				messagedata.ConsensusActionFailure:
				return

			case messagedata.ConsensusActionElectAccept,
				messagedata.ConsensusActionPrepare,
				messagedata.ConsensusActionPromise,
				messagedata.ConsensusActionPropose,
				messagedata.ConsensusActionAccept:

			default:
				c.log.Error().Msgf("action %s isn't a recognised action", action)
				return
			}

		case <-c.clock.After(fastTimeout):
			switch instance.lastSent {
			case messagedata.ConsensusActionElectAccept:
				select {
				case action := <-timeoutChan:
					// Stop the timer when receiving one action finishing it
					if action == messagedata.ConsensusActionLearn ||
						action == messagedata.ConsensusActionFailure {
						return
					}

				case <-c.clock.After(slowTimeout):
					c.timeoutFailure(instance, messageID)
					return
				}

			case messagedata.ConsensusActionPrepare,
				messagedata.ConsensusActionPropose:

				instance.role = acceptorRole

				select {
				case action := <-timeoutChan:
					// Stop the timer when receiving one action finishing it
					if action == messagedata.ConsensusActionLearn ||
						action == messagedata.ConsensusActionFailure {
						return
					}

				case <-c.clock.After(fastTimeout):
					c.timeoutFailure(instance, messageID)
					return
				}

			case messagedata.ConsensusActionPromise,
				messagedata.ConsensusActionAccept:

				c.timeoutFailure(instance, messageID)
				return

			case messagedata.ConsensusActionLearn,
				messagedata.ConsensusActionFailure:
				return
			}
		}
	}
}

// timeoutFailure sends a failure message during a timeout
func (c *Channel) timeoutFailure(instance *ConsensusInstance, messageID string) {
	byteMsg, err := c.createFailureMessage(instance, messageID)
	if err != nil {
		c.log.Err(err).Msg(failedFailureCreation)
		return
	}

	err = c.publishNewMessage(byteMsg)
	if err != nil {
		c.log.Err(err).Msg(failedFailureSending)
	}
}
