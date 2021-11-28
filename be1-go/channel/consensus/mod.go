package consensus

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/channel"
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

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

const (
	msgID = "msg id"
)

// Channel defines a consensus channel
type Channel struct {
	clientSockets channel.Sockets
	serverSockets channel.Sockets

	inbox *inbox.Inbox

	// /root/<lao_id>/consensus
	channelID string

	hub channel.HubFunctionalities

	attendees map[string]struct{}

	log zerolog.Logger

	consensusInstances map[string]*ConsensusInstance
	messageStates      map[string]*MessageState
}

// Save the state of a consensus instance
type ConsensusInstance struct {
	sync.RWMutex
	id string

	proposed_try int64
	promised_try int64
	accepted_try int64

	accepted_value *bool

	decided        bool
	decision       *bool
	proposed_value *bool

	promises []messagedata.ConsensusPromise
	accepts  []messagedata.ConsensusAccept
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
	PreparePhase     Phase = 2
	PromisePhase     Phase = 3
	ProposePhase     Phase = 4
	AcceptPhase      Phase = 5
	LearnPhase       Phase = 6
	Finished         Phase = 7
)

// NewChannel returns a new initialized consensus channel
func NewChannel(channelID string, hub channel.HubFunctionalities, log zerolog.Logger) channel.Channel {
	inbox := inbox.NewInbox(channelID)

	log = log.With().Str("channel", "consensus").Logger()

	return &Channel{
		clientSockets:      channel.NewSockets(),
		serverSockets:      channel.NewSockets(),
		inbox:              inbox,
		channelID:          channelID,
		hub:                hub,
		attendees:          make(map[string]struct{}),
		log:                log,
		consensusInstances: make(map[string]*ConsensusInstance),
		messageStates:      make(map[string]*MessageState),
	}
}

// Subscribe is used to handle a subscribe message from the client
func (c *Channel) Subscribe(sock socket.Socket, msg method.Subscribe) error {
	c.log.Info().Str(msgID, strconv.Itoa(msg.ID)).Msg("received a subscribe")

	if sock.Type() == socket.ClientSocketType {
		c.clientSockets.Upsert(sock)

		if c.clientSockets.Number() == 1 {
			c.hub.SendSubscribeToServers(c.channelID)
		}
	} else {
		c.serverSockets.Upsert(sock)
	}

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *Channel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	c.log.Info().Str(msgID, strconv.Itoa(msg.ID)).Msg("received an unsubscribe")

	okClient := c.clientSockets.Delete(socketID)
	okServer := c.serverSockets.Delete(socketID)

	if !okClient && !okServer {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	if okClient && c.clientSockets.Number() == 0 {
		c.hub.SendUnsubscribeToServers(c.channelID)
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

	c.clientSockets.SendToAll(buf)
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

// ProcessConsensusObject processes a Consensus Object.
func (c *Channel) processConsensusObject(action string, msg message.Message) error {

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	// Unmarshal sender of the message, used to know who is the propose
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewErrorf(-4, "failed to unmarshal public key of the sender: %v", err)
	}

	switch action {
	case messagedata.ConsensusActionElect:
		var consensusElect messagedata.ConsensusElect

		err := msg.UnmarshalData(&consensusElect)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#elect: %v", err)
		}

		err = c.processConsensusElect(senderPoint, msg.MessageID, consensusElect)
		if err != nil {
			return xerrors.Errorf("failed to process elect action: %w", err)
		}
	case messagedata.ConsensusActionElectAccept:
		var consensusElectAccept messagedata.ConsensusElectAccept

		err := msg.UnmarshalData(&consensusElectAccept)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#elect-accept: %v", err)
		}

		err = c.processConsensusElectAccept(senderPoint, consensusElectAccept)
		if err != nil {
			return xerrors.Errorf("failed to process elect accept action: %w", err)
		}
	case messagedata.ConsensusActionPrepare:
		var consensusPrepare messagedata.ConsensusPrepare

		err := msg.UnmarshalData(&consensusPrepare)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#prepare: %v", err)
		}

		err = c.processConsensusPrepare(consensusPrepare)
		if err != nil {
			return xerrors.Errorf("failed to process prepare action: %w", err)
		}
	case messagedata.ConsensusActionPromise:
		var consensusPromise messagedata.ConsensusPromise

		err := msg.UnmarshalData(&consensusPromise)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#promise: %v", err)
		}

		err = c.processConsensusPromise(senderPoint, consensusPromise)
		if err != nil {
			return xerrors.Errorf("failed to process promise action: %w", err)
		}
	case messagedata.ConsensusActionPropose:
		var consensusPropose messagedata.ConsensusPropose

		err := msg.UnmarshalData(&consensusPropose)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#propose: %v", err)
		}

		err = c.processConsensusPropose(consensusPropose)
		if err != nil {
			return xerrors.Errorf("failed to process propose action: %w", err)
		}
	case messagedata.ConsensusActionAccept:
		var consensusAccept messagedata.ConsensusAccept

		err := msg.UnmarshalData(&consensusAccept)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#accept: %v", err)
		}

		err = c.processConsensusAccept(consensusAccept)
		if err != nil {
			return xerrors.Errorf("failed to process accept action: %w", err)
		}
	case messagedata.ConsensuisActionLearn:
		var consensusLearn messagedata.ConsensusLearn

		err := msg.UnmarshalData(&consensusLearn)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#learn: %v", err)
		}

		err = c.processConsensusLearn(consensusLearn)
		if err != nil {
			return xerrors.Errorf("failed to process learn action: %w", err)
		}
	default:
		return answer.NewInvalidActionError(action)
	}

	c.inbox.StoreMessage(msg)

	return nil
}

// ProcessConsensusElect processes an elect action.
func (c *Channel) processConsensusElect(sender kyber.Point, messageID string, data messagedata.ConsensusElect) error {

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#elect message: %v", err)
	}

	// Creates a consensus instance if there is none on the object
	if c.consensusInstances[data.InstanceID] == nil {
		c.consensusInstances[data.InstanceID] = &ConsensusInstance{

			id: data.InstanceID,

			proposed_try: 0,
			promised_try: -1,
			accepted_try: -1,

			accepted_value: nil,
			decided:        false,
			decision:       nil,
			proposed_value: nil,

			promises: make([]messagedata.ConsensusPromise, 0),
			accepts:  make([]messagedata.ConsensusAccept, 0),
		}
	}

	newMessageState := MessageState{
		currentPhase:      ElectAcceptPhase,
		proposer:          sender,
		electAcceptNumber: 0,
	}

	c.messageStates[messageID] = &newMessageState

	return nil
}

// ProcessConsensusElectAccept processes an elect accept action.
func (c *Channel) processConsensusElectAccept(sender kyber.Point, data messagedata.ConsensusElectAccept) error {

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#elect_accept message: %v", err)
	}

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any previously received message")
	}

	messageState := c.messageStates[data.MessageID]
	messageState.Lock()
	defer messageState.Unlock()
	messageState.electAcceptNumber += 1

	electAcceptNumber := messageState.electAcceptNumber

	// Once all Elect_Accept have been received, proposer creates new prepare
	// message
	if electAcceptNumber >= c.serverSockets.Number() &&
		messageState.currentPhase == ElectAcceptPhase &&
		messageState.proposer.Equal(c.hub.GetPubKeyOrg()) {

		messageState.currentPhase = PromisePhase

		consensusInstance := c.consensusInstances[data.InstanceID]
		consensusInstance.Lock()
		defer consensusInstance.Unlock()

		if consensusInstance.proposed_try >= consensusInstance.promised_try {
			consensusInstance.proposed_try += 1
		} else {
			consensusInstance.proposed_try = consensusInstance.promised_try + 1
		}

		newData := messagedata.ConsensusPrepare{
			Object:     "consensus",
			Action:     "prepare",
			InstanceID: data.InstanceID,
			MessageID:  data.MessageID,

			CreatedAt: time.Now().Unix(),

			Value: messagedata.ValuePrepare{
				ProposedTry: consensusInstance.proposed_try,
			},
		}
		byteMsg, err := json.Marshal(newData)
		if err != nil {
			return xerrors.Errorf("failed to marshal new consensus#prepare message: %v", err)
		}

		err = c.publishMessage(byteMsg)
		if err != nil {
			return xerrors.Errorf("failed to send new consensus#prepare message: %v", err)
		}
		return nil
	}

	return nil
}

// ProcessConsensusPrepare processes a prepare action.
func (c *Channel) processConsensusPrepare(data messagedata.ConsensusPrepare) error {

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)
	if !valid {
		return xerrors.Errorf("message doesn't correspond to any previously received message")
	}

	// check wether the consensus finished the elect accept phase
	if c.messageStates[data.MessageID].currentPhase <= ElectAcceptPhase {
		return xerrors.Errorf("consensus corresponding to the message hasn't " +
			"finished the elect_accept phase")
	}

	// If the server has no client subscribed to the consensus channel, it
	// doesn't take part in it
	if c.clientSockets.Number() == 0 {
		return nil
	}

	consensusInstance := c.consensusInstances[data.InstanceID]
	if consensusInstance.proposed_try > consensusInstance.promised_try {
		consensusInstance.promised_try = consensusInstance.proposed_try

		newData := messagedata.ConsensusPromise{
			Object:     "consensus",
			Action:     "prepare",
			InstanceID: data.InstanceID,
			MessageID:  data.MessageID,

			CreatedAt: time.Now().Unix(),

			Value: messagedata.ValuePromise{
				AcceptedTry:   consensusInstance.accepted_try,
				AcceptedValue: *consensusInstance.accepted_value,
				PromisedTry:   consensusInstance.promised_try,
			},
		}
		byteMsg, err := json.Marshal(newData)
		if err != nil {
			return xerrors.Errorf("failed to marshal new consensus#promise message: %v", err)
		}

		err = c.publishMessage(byteMsg)
		if err != nil {
			return err
		}
	}

	return nil
}

// ProcessConsensusPromise processes a promise action.
func (c *Channel) processConsensusPromise(sender kyber.Point, data messagedata.ConsensusPromise) error {

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)
	if !valid {
		return xerrors.Errorf("message doesn't correspond to any previously received message")
	}

	messageState := c.messageStates[data.MessageID]

	if messageState.currentPhase < PromisePhase {
		return xerrors.Errorf("consensus corresponding to the message hasn't" +
			" entered the promise phase")
	}

	consensusInstance := c.consensusInstances[data.InstanceID]
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	consensusInstance.promises = append(consensusInstance.promises, data)

	// if enough Promise messages are received, the proposer send a Propose message
	if len(consensusInstance.promises) >= c.serverSockets.Number()/2+1 &&
		messageState.currentPhase == PromisePhase &&
		messageState.proposer.Equal(c.hub.GetPubKeyOrg()) {

		highestAccepted := int64(-1)
		for _, promise := range consensusInstance.promises {
			if promise.Value.AcceptedTry > highestAccepted {
				highestAccepted = promise.Value.AcceptedTry
			}
		}

		newData := messagedata.ConsensusPropose{
			Object:     "consensus",
			Action:     "propose",
			InstanceID: data.InstanceID,
			MessageID:  data.MessageID,

			CreatedAt: time.Now().Unix(),

			Value: messagedata.ValuePropose{
				ProposedValue: true,
			},
		}

		if highestAccepted == -1 {
			newData.Value.ProposedTry = consensusInstance.proposed_try
			byteMsg, err := json.Marshal(newData)
			if err != nil {
				return xerrors.Errorf("failed to marshal new consensus#propose message: %v", err)
			}

			err = c.publishMessage(byteMsg)
			if err != nil {
				return err
			}
		} else {
			newData.Value.ProposedTry = highestAccepted
			byteMsg, err := json.Marshal(newData)
			if err != nil {
				return xerrors.Errorf("failed to marshal new consensus#propose message: %v", err)
			}

			err = c.publishMessage(byteMsg)
			if err != nil {
				return err
			}
		}
	}

	return nil
}

// ProcessConsensusPropose processes a propose action.
func (c *Channel) processConsensusPropose(data messagedata.ConsensusPropose) error {

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any previously received message")
	}

	consensusInstance := c.consensusInstances[data.InstanceID]

	// If the server has no client subscribed to the consensus channel, it
	// doesn't take part in it
	if c.clientSockets.Number() == 0 {
		return nil
	}

	if consensusInstance.promised_try <= data.Value.ProposedTry {
		consensusInstance.accepted_try = data.Value.ProposedTry
		consensusInstance.accepted_value = &data.Value.ProposedValue

		newData := messagedata.ConsensusAccept{
			Object:     "consensus",
			Action:     "accept",
			InstanceID: data.InstanceID,
			MessageID:  data.MessageID,

			CreatedAt: time.Now().Unix(),

			Value: messagedata.ValueAccept{
				AcceptedTry:   consensusInstance.accepted_try,
				AcceptedValue: *consensusInstance.accepted_value,
			},
		}
		byteMsg, err := json.Marshal(newData)
		if err != nil {
			return xerrors.Errorf("failed to marshal new consensus#accept message: %v", err)
		}

		return c.publishMessage(byteMsg)
	}

	return nil
}

// ProcessConsensusAccept proccesses an accept action.
func (c *Channel) processConsensusAccept(data messagedata.ConsensusAccept) error {

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any previously received message")
	}

	consensusInstance := c.consensusInstances[data.InstanceID]
	consensusInstance.Lock()
	defer consensusInstance.Unlock()

	consensusInstance.accepts = append(consensusInstance.accepts, data)

	if len(consensusInstance.accepts) >= c.serverSockets.Number()/2+1 &&
		c.messageStates[data.MessageID].proposer.Equal(c.hub.GetPubKeyOrg()) {

		if !consensusInstance.decided {
			consensusInstance.decided = true
			consensusInstance.decision = consensusInstance.proposed_value

			newData := messagedata.ConsensusLearn{
				Object:     "consensus",
				Action:     "learn",
				InstanceID: data.InstanceID,
				MessageID:  data.MessageID,

				CreatedAt: time.Now().Unix(),

				Value: messagedata.ValueLearn{
					Decision: *consensusInstance.decision,
				},
			}
			byteMsg, err := json.Marshal(newData)
			if err != nil {
				return xerrors.Errorf("failed to marshal new consensus#promise message: %v", err)
			}

			return c.publishMessage(byteMsg)
		}
	}

	return nil
}

// ProcessConsensusLearn processes a learn action.
func (c *Channel) processConsensusLearn(data messagedata.ConsensusLearn) error {

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid consensus#learn message: %v", err)
	}

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any received message")
	}

	consensusInstance := c.consensusInstances[data.InstanceID]
	if !consensusInstance.decided {
		consensusInstance.decided = true
		consensusInstance.decision = &data.Value.Decision
	}

	return nil
}

// publishMessage send a publish message on the current channel
func (c *Channel) publishMessage(byteMsg []byte) error {

	encryptedMsg := base64.URLEncoding.EncodeToString(byteMsg)

	publicKey := c.hub.GetPubKeyServ()
	pkBuf, err := publicKey.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal the public key: %v", err)
	}

	encryptedKey := base64.URLEncoding.EncodeToString(pkBuf)

	privateKey := c.hub.GetSecKeyServ()
	signatureBuf, err := schnorr.Sign(crypto.Suite, privateKey, byteMsg)
	if err != nil {
		return xerrors.Errorf("failed to sign the data: %v", err)
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	messageID := messagedata.Hash(encryptedKey, encryptedKey)

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
		},

		Params: struct {
			Channel string          "json:\"channel\""
			Message message.Message "json:\"message\""
		}{
			Channel: c.channelID,
			Message: msg,
		},
	}

	bufPublish, err := json.Marshal(publish)
	if err != nil {
		return xerrors.Errorf("failed to marshal publish message: %v", err)
	}

	c.serverSockets.SendToAll(bufPublish)
	return nil
}
