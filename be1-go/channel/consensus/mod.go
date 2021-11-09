package consensus

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/channel"
	"popstellar/channel/inbox"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"strconv"

	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
)

const (
	msgID = "msg id"
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
}

// NewChannel returns a new initialized consensus channel
func NewChannel(channelID string, hub channel.HubFunctionalities, log zerolog.Logger) Channel {
	inbox := inbox.NewInbox(channelID)

	log = log.With().Str("channel", "consensus").Logger()

	return Channel{
		sockets:   channel.NewSockets(),
		inbox:     inbox,
		channelID: channelID,
		hub:       hub,
		attendees: make(map[string]struct{}),
		log:       log,
	}
}

// Subscribe is used to handle a subscribe message from the client
func (c *Channel) Subscribe(socket socket.Socket, msg method.Subscribe) error {
	c.log.Info().Str(msgID, strconv.Itoa(msg.ID)).Msg("received a subscribe")
	c.sockets.Upsert(socket)

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

// BroadcastToAllWitnesses is a helper message to broadcast a message to all
// witnesses.
func (c *Channel) BroadcastToAllWitnesses(msg message.Message) error {
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
func (c *Channel) Publish(publish method.Publish) error {
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

	err = c.BroadcastToAllWitnesses(msg)
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
	switch action {
	case messagedata.ConsensusActionElect:
		var consensusElect messagedata.ConsensusElect

		err := msg.UnmarshalData(&consensusElect)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#elect: %v", err)
		}

		err = c.processConsensusElect(consensusElect)
		if err != nil {
			return xerrors.Errorf("failed to process elect action: %w", err)
		}
	case messagedata.ConsensusActionElectAccept:
		var consensusElectAccept messagedata.ConsensusElectAccept

		err := msg.UnmarshalData(&consensusElectAccept)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#elect-accept: %v", err)
		}

		err = c.processConsensusElectAccept(consensusElectAccept)
		if err != nil {
			return xerrors.Errorf("failed to process elect accept action: %w", err)
		}
	case messagedata.ConsensusActionLearn:
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
func (c *Channel) processConsensusElect(data messagedata.ConsensusElect) error {

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("failed to process consensus#elect message: %v", err)
	}

	return nil
}

// ProcessConsensusElectAccept processes an elect accept action.
func (c *Channel) processConsensusElectAccept(data messagedata.ConsensusElectAccept) error {

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any received message")
	}
	return nil
}

// ProcessConsensusElectAccept processes a elect accept action.
func (c *Channel) processConsensusLearn(data messagedata.ConsensusLearn) error {

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any received message")
	}
	return nil
}
