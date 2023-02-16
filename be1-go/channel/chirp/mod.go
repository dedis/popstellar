package chirp

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
	"popstellar/persistence"
	"popstellar/validation"
	"strconv"

	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
)

const (
	msgID              = "msg id"
	failedToDecodeData = "failed to decode message data: %v"
)

// Channel is used to handle chirp messages.
type Channel struct {
	sockets        channel.Sockets
	inbox          *inbox.Inbox
	generalChannel channel.Broadcastable
	// channel path
	channelID string
	owner     string
	hub       channel.HubFunctionalities
	log       zerolog.Logger
	registry  registry.MessageRegistry
}

// NewChannel returns a new initialized individual chirping channel
func NewChannel(channelPath string, ownerKey string, hub channel.HubFunctionalities,
	generalChannel channel.Broadcastable, log zerolog.Logger) *Channel {

	log = log.With().Str("channel", "chirp").Logger()

	newChannel := &Channel{
		sockets:        channel.NewSockets(),
		inbox:          inbox.NewInbox(channelPath),
		channelID:      channelPath,
		generalChannel: generalChannel,
		owner:          ownerKey,
		hub:            hub,
		log:            log,
	}

	newChannel.registry = newChannel.NewChirpRegistry()

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
		Msg("received an unsubscribe")

	ok := c.sockets.Delete(socketID)

	if !ok {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Publish is used to handle publish messages in the chirp channel.
func (c *Channel) Publish(publish method.Publish, socket socket.Socket) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	err := c.verifyMessage(publish.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on a "+
			"chirping channel: %w", err)
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
		return xerrors.Errorf("failed to verify broadcast message on a "+
			"chirping channel: %w", err)
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
		return xerrors.Errorf("failed to broadcast to all clients: %v", err)
	}

	err = c.broadcastViaGeneral(msg)
	if err != nil {
		return xerrors.Errorf("failed to broadcast the chirp message via general : %v", err)
	}

	return nil
}

// NewChirpRegistry creates a new registry for a general chirping channel and
// populates the registry with the actions of the channel.
func (c *Channel) NewChirpRegistry() registry.MessageRegistry {
	newRegistry := registry.NewMessageRegistry()

	newRegistry.Register(messagedata.ChirpAdd{}, c.processAddChirp)
	newRegistry.Register(messagedata.ChirpDelete{}, c.processDeleteChirp)

	return newRegistry
}

func (c *Channel) processAddChirp(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.ChirpAdd)
	if !ok {
		return xerrors.Errorf("message %v isn't a chirp#add message", msgData)
	}

	err := c.verifyChirpMessage(msg, data)
	if err != nil {
		return xerrors.Errorf("failed to verify add chirp message: %v", err)
	}

	return nil
}

func (c *Channel) processDeleteChirp(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.ChirpDelete)
	if !ok {
		return xerrors.Errorf("message %v isn't a chirp#delete message", msgData)
	}

	err := c.verifyChirpMessage(msg, data)
	if err != nil {
		return xerrors.Errorf("failed to verify delete chirp message: %v", err)
	}

	_, b := c.inbox.GetMessage(data.ChirpID)
	if !b {
		return xerrors.Errorf("the message to be deleted was not found")
	}

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
	_, ok := c.inbox.GetMessage(msg.MessageID)
	if ok {
		return answer.NewError(-3, "message already exists")
	}

	return nil
}

func (c *Channel) verifyChirpMessage(msg message.Message, chirpMsg messagedata.Verifiable) error {
	err := chirpMsg.Verify()
	if err != nil {
		return xerrors.Errorf("invalid chirp message: %v", err)
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

	if msg.Sender != c.owner {
		return answer.NewError(-4, "only the owner of the channel can post chirps")
	}

	return nil
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *Channel) broadcastToAllClients(msg message.Message) error {
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
			c.channelID,
			msg,
		},
	}

	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		return xerrors.Errorf("failed to marshal broadcast: %v", err)
	}

	c.sockets.SendToAll(buf)

	return nil
}

func (c *Channel) broadcastViaGeneral(msg message.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode the data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	action = "notify_" + action
	if err != nil {
		return xerrors.Errorf("failed to read the message data: %v", err)
	}

	time, err := messagedata.GetTime(jsonData)
	if err != nil {
		return xerrors.Errorf("failed to read the message data: %v", err)
	}

	newData := messagedata.ChirpBroadcast{
		Object:    object,
		Action:    action,
		ChirpID:   msg.MessageID,
		Channel:   c.generalChannel.GetChannelPath(),
		Timestamp: time,
	}

	dataBuf, err := json.Marshal(newData)
	if err != nil {
		return xerrors.Errorf("failed to marshal the data: %v", err)
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	pk := c.hub.GetPubKeyServ()
	pkBuf, err := pk.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal the public key: %v", err)
	}

	// Sign the data
	signatureBuf, err := c.hub.Sign(dataBuf)
	if err != nil {
		return xerrors.Errorf("failed to sign the data: %v", err)
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

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
			message.Message{
				Data:              newData64,
				Sender:            base64.URLEncoding.EncodeToString(pkBuf),
				Signature:         signature,
				MessageID:         msg.MessageID,
				WitnessSignatures: msg.WitnessSignatures,
			},
		},
	}

	err = c.generalChannel.Broadcast(rpcMessage, nil)
	if err != nil {
		return xerrors.Errorf("the general channel failed to broadcast the chirp message: %v", err)
	}

	return nil
}

// NewChannelFromState returns a new channel initialized from a previous state
func NewChannelFromState(state persistence.ChirpState, hub channel.HubFunctionalities,
	generalChannel channel.Broadcastable, log zerolog.Logger) *Channel {

	log = log.With().Str("channel", "chirp").Logger()

	newChannel := &Channel{
		sockets:        channel.NewSockets(),
		inbox:          inbox.NewInboxFromState(state.Inbox),
		generalChannel: generalChannel,
		channelID:      state.ChannelPath,
		owner:          state.OwnerKey,
		hub:            hub,
		log:            log,
	}

	newChannel.registry = newChannel.NewChirpRegistry()

	return newChannel
}

// GetChannelState returns the state of the channel in a JSON object
func (c *Channel) GetChannelState() persistence.ChirpState {
	return persistence.ChirpState{
		ChannelPath: c.channelID,
		OwnerKey:    c.owner,
		Inbox:       c.inbox.GetInboxState(),
	}
}
