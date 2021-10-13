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

// Channel defins a consensus channel
type Channel struct {
	sockets channel.Sockets

	inbox *inbox.Inbox

	// /root/<lao_id>/<id>
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

// BroadcastToAllWitnesses is a helper message to broadcast a message to all
// witnesses.
func (c *Channel) BroadcastToAllWitnesses(msg message.Message) {
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
		c.log.Err(err).Msg("failed to marshal broadcast query")
	}

	c.sockets.SendToAll(buf)
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

	switch object {
	case messagedata.ConsensusObject:
		err = c.processConsensusObject(action, msg)
	}

	if err != nil {
		return xerrors.Errorf("failed to process %q object: %w", object, err)
	}

	c.BroadcastToAllWitnesses(msg)
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
	case messagedata.ConsensusActionPhase1Elect:
		var consensusPhase1Elect messagedata.ConsensusPhase1Elect

		err := msg.UnmarshalData(&consensusPhase1Elect)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#phase-1-elect: %v", err)
		}

		err = c.processConsensusPhase1Elect(consensusPhase1Elect)
		if err != nil {
			return xerrors.Errorf("failed to process phase 1 elect action: %w", err)
		}
	case messagedata.ConsensusActionPhase1ElectAccept:
		var consensusPhase1ElectAccept messagedata.ConsensusPhase1ElectAccept

		err := msg.UnmarshalData(&consensusPhase1ElectAccept)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#phase-1-elect-accept: %v", err)
		}

		err = c.processConsensusPhase1ElectAccept(consensusPhase1ElectAccept)
		if err != nil {
			return xerrors.Errorf("failed to process phase 1 elect accept action: %w", err)
		}
	case messagedata.ConsensuisActionPhase1Learn:
		var consensusPhase1Learn messagedata.ConsensusPhase1Learn

		err := msg.UnmarshalData(&consensusPhase1Learn)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal consensus#phase-1-learn: %v", err)
		}

		err = c.processConsensusPhase1Learn(consensusPhase1Learn)
		if err != nil {
			return xerrors.Errorf("failed to process phase 1 learn action: %w", err)
		}
	default:
		return answer.NewInvalidActionError(action)
	}

	return nil
}

// process ConsensusPhase1Elect processes a phase 1 elect action.
func (c *Channel) processConsensusPhase1Elect(data messagedata.ConsensusPhase1Elect) error {

	err := data.Verify()

	return err
}

// process ConsensusPhase1ElectAccept processes a phase 1 elect accept action.
func (c *Channel) processConsensusPhase1ElectAccept(data messagedata.ConsensusPhase1ElectAccept) error {

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any received message")
	}
	return nil
}

// process ConsensusPhase1ElectAccept processes a phase 1 elect accept action.
func (c *Channel) processConsensusPhase1Learn(data messagedata.ConsensusPhase1Learn) error {

	// check wether a message with the correct ID was received previously
	_, valid := c.inbox.GetMessage(data.MessageID)

	if !valid {
		return xerrors.Errorf("message doesn't correspond to any received message")
	}
	return nil
}
