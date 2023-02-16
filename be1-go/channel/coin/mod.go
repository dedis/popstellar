package coin

import (
	"encoding/base64"
	"encoding/json"
	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
	"popstellar/channel"
	"popstellar/channel/registry"
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
)

const (
	msgID = "msg id"
)

// Channel is used to handle election messages.
type Channel struct {
	sockets   channel.Sockets
	inbox     *inbox.Inbox
	hub       channel.HubFunctionalities
	channelID string
	registry  registry.MessageRegistry

	log zerolog.Logger
}

// NewChannel returns a new initialized digitalCash channel
func NewChannel(channelID string, hub channel.HubFunctionalities,
	log zerolog.Logger) channel.Channel {

	box := inbox.NewInbox(channelID)

	log = log.With().Str("channel", "coin").Logger()

	// Saving on Digital Cash channel too so it self-contains the entire cash history
	retChannel := &Channel{
		sockets:   channel.NewSockets(),
		inbox:     box,
		channelID: channelID,
		hub:       hub,
		log:       log,
	}

	retChannel.registry = retChannel.NewDigitalCashRegistry()

	return retChannel
}

// ---
// Publish-subscribe / channel.Channel implementation
// ---

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

// Publish handles publish messages for the consensus channel
func (c *Channel) Publish(publish method.Publish, socket socket.Socket) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	err := c.verifyMessage(publish.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message: %v", err)
	}

	err = c.handleMessage(publish.Params.Message, socket)
	if err != nil {
		return xerrors.Errorf("failed to handle a publish message: %v", err)
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

// NewDigitalCashRegistry creates a new registry for the digital cash channel
func (c *Channel) NewDigitalCashRegistry() registry.MessageRegistry {
	registry := registry.NewMessageRegistry()

	registry.Register(messagedata.PostTransaction{}, c.processPostTransaction)

	return registry
}

// broadcastToAllClients is a helper message to broadcast a message to all clients.
func (c *Channel) broadcastToAllClients(msg message.Message) error {
	c.log.Info().Str(msgID, msg.MessageID).Msg("broadcasting message to all")

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

// processPostTransaction handles a message object.
func (c *Channel) processPostTransaction(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.PostTransaction)
	if !ok {
		return xerrors.Errorf("message %T isn't a transaction#post message", msgData)
	}

	err := data.Verify()
	if err != nil {
		return xerrors.Errorf("invalid coin#postTransaction message: %v", err)
	}

	return nil
}
