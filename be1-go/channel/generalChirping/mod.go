package generalChirping

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

	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
)

const msgID = "msg id"

// Channel is used to handle chirping messages w/o text.
type Channel struct {
	sockets channel.Sockets
	inbox   *inbox.Inbox
	hub     channel.HubFunctionalities

	// channel path
	channelPath string

	log      zerolog.Logger
	registry registry.MessageRegistry
}

// NewChannel returns a new initialized chirping channel
func NewChannel(channelPath string, hub channel.HubFunctionalities, log zerolog.Logger) *Channel {
	log = log.With().Str("channel", "general chirp").Logger()

	newChannel := &Channel{
		sockets:     channel.NewSockets(),
		inbox:       inbox.NewInbox(channelPath),
		channelPath: channelPath,
		hub:         hub,
		log:         log,
	}

	newChannel.registry = newChannel.NewGeneralChirpingRegistry()

	return newChannel
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
		Msg("received a unsubscribe")

	ok := c.sockets.Delete(socketID)
	if !ok {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Publish is used to handle a publish message.
func (c *Channel) Publish(msg method.Publish, socket socket.Socket) error {
	c.log.Error().
		Str(msgID, strconv.Itoa(msg.ID)).
		Msg("nothing should be published in the general")
	return xerrors.Errorf("nothing should be directly published in the general")
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(msg method.Catchup) []message.Message {
	c.log.Info().
		Str(msgID, strconv.Itoa(msg.ID)).
		Msg("received a catchup")
	return c.inbox.GetSortedMessages()
}

// Broadcast is used to handle broadcast messages.
func (c *Channel) Broadcast(broadcast method.Broadcast, socket socket.Socket) error {
	err := c.VerifyBroadcastMessage(broadcast)
	if err != nil {
		return xerrors.Errorf("failed to verify broadcast message on an "+
			"generalChirping channel: %w", err)
	}

	msg := broadcast.Params.Message

	err = c.registry.Process(msg, socket)
	if err != nil {
		return xerrors.Errorf("failed to process message: %w", err)
	}

	c.inbox.StoreMessage(msg)

	err = c.broadcastToAllClients(msg)
	if err != nil {
		return xerrors.Errorf("failed to broadcast to all clients: %v", err)
	}

	return nil
}

// ---
// Message handling
// ---

// NewGeneralChirpingRegistry creates a new registry for a general chirping
// channel and populates the registry with the actions of the channel.
func (c *Channel) NewGeneralChirpingRegistry() registry.MessageRegistry {
	newRegistry := registry.NewMessageRegistry()

	newRegistry.Register(messagedata.ChirpNotifyAdd{}, c.processAddChirp)
	newRegistry.Register(messagedata.ChirpNotifyDelete{}, c.processDeleteChirp)

	return newRegistry
}

// processAddChirp checks an add chirp message
func (c *Channel) processAddChirp(msg message.Message, msgData interface{}, _ socket.Socket) error {
	data, ok := msgData.(*messagedata.ChirpNotifyAdd)
	if !ok {
		return xerrors.Errorf("message %v isn't a chirp#notifyAdd message", msgData)
	}

	err := c.verifyNotifyChirp(msg, data)
	if err != nil {
		return xerrors.Errorf("failed to get and verify add chirp message: %v", err)
	}

	return nil
}

// processDeleteChirp checks a delete chirp message
func (c *Channel) processDeleteChirp(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.ChirpNotifyDelete)
	if !ok {
		return xerrors.Errorf("message %v isn't a chirp#notifyDelete message", msgData)
	}

	err := c.verifyNotifyChirp(msg, data)
	if err != nil {
		return xerrors.Errorf("failed to get and verify delete chirp message: %v", err)
	}

	return nil
}

// VerifyBroadcastMessage checks if a Broadcast message is valid
func (c *Channel) VerifyBroadcastMessage(broadcast method.Broadcast) error {
	c.log.Info().Msg("received broadcast")

	// Check if the structure of the message is correct
	msg := broadcast.Params.Message

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
	_, ok := c.inbox.GetMessage(msg.MessageID)
	if ok {
		return answer.NewError(-3, "message already exists")
	}

	return nil
}

func (c *Channel) verifyNotifyChirp(msg message.Message, chirpMsg messagedata.Verifiable) error {
	err := chirpMsg.Verify()
	if err != nil {
		return xerrors.Errorf("invalid chirp broadcast message: %v", err)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewError(-4, "invalid sender public key")
	}

	ok := c.hub.GetPubKeyServ().Equal(senderPoint)
	if !ok {
		return answer.NewError(-4, "only the server can broadcast the chirp messages")
	}

	return nil
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *Channel) broadcastToAllClients(msg message.Message) error {
	c.log.Info().
		Str(msgID, msg.MessageID).
		Msg("broadcast new chirp to all clients")

	rpcMessage := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodBroadcast,
		},
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			c.channelPath,
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

// GetChannelPath is a getter for the channel path
func (c *Channel) GetChannelPath() string {
	return c.channelPath
}
