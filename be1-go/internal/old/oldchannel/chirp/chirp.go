package chirp

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/crypto"
	"popstellar/internal/handler/answer/manswer"
	"popstellar/internal/handler/channel"
	mchirp2 "popstellar/internal/handler/channel/chirp/mchirp"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/broadcast/mbroadcast"
	"popstellar/internal/handler/method/catchup/mcatchup"
	"popstellar/internal/handler/method/publish/mpublish"
	"popstellar/internal/handler/method/subscribe/msubscribe"
	method2 "popstellar/internal/handler/method/unsubscribe/munsubscribe"
	"popstellar/internal/handler/query/mquery"
	"popstellar/internal/network/socket"
	"popstellar/internal/old/inbox"
	"popstellar/internal/old/oldchannel"
	"popstellar/internal/old/oldchannel/registry"
	"popstellar/internal/validation"
	"strconv"
	"time"

	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
)

const (
	msgID              = "msg id"
	failedToDecodeData = "failed to decode message data: %v"
	retryDelay         = time.Second
)

// Channel is used to handle chirp messages.
type Channel struct {
	sockets        oldchannel.Sockets
	inbox          *inbox.Inbox
	generalChannel oldchannel.Broadcastable
	// oldchannel path
	channelID string
	owner     string
	hub       oldchannel.HubFunctionalities
	log       zerolog.Logger
	registry  registry.MessageRegistry
}

// NewChannel returns a new initialized individual chirping oldchannel
func NewChannel(channelPath string, ownerKey string, hub oldchannel.HubFunctionalities,
	generalChannel oldchannel.Broadcastable, log zerolog.Logger) *Channel {

	log = log.With().Str("oldchannel", "chirp").Logger()

	newChannel := &Channel{
		sockets:        oldchannel.NewSockets(),
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
// Publish-subscribe / oldchannel.Channel implementation
// ---

// Subscribe is used to handle a subscribe message from the client.
func (c *Channel) Subscribe(socket socket.Socket, msg msubscribe.Subscribe) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(msg.ID)).
		Msg("received a subscribe")
	c.sockets.Upsert(socket)

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *Channel) Unsubscribe(socketID string, msg method2.Unsubscribe) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(msg.ID)).
		Msg("received an unsubscribe")

	ok := c.sockets.Delete(socketID)

	if !ok {
		return manswer.NewError(-2, "client is not subscribed to this oldchannel")
	}

	return nil
}

// Publish is used to handle publish messages in the chirp oldchannel.
func (c *Channel) Publish(publish mpublish.Publish, socket socket.Socket) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	err := c.verifyMessage(publish.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on a "+
			"chirping oldchannel: %w", err)
	}

	err = c.handleMessage(publish.Params.Message, socket)
	if err != nil {
		return xerrors.Errorf("failed to handle publish message: %v", err)
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup mcatchup.Catchup) []mmessage.Message {
	c.log.Info().
		Str(msgID, strconv.Itoa(catchup.ID)).
		Msg("received a catchup")

	return c.inbox.GetSortedMessages()
}

// Broadcast is used to handle a broadcast message.
func (c *Channel) Broadcast(broadcast mbroadcast.Broadcast, socket socket.Socket) error {
	c.log.Info().Msg("received a broadcast")

	err := c.verifyMessage(broadcast.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify broadcast message on a "+
			"chirping oldchannel: %w", err)
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
func (c *Channel) handleMessage(msg mmessage.Message, socket socket.Socket) error {
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

// NewChirpRegistry creates a new registry for a general chirping oldchannel and
// populates the registry with the actions of the oldchannel.
func (c *Channel) NewChirpRegistry() registry.MessageRegistry {
	newRegistry := registry.NewMessageRegistry()

	newRegistry.Register(mchirp2.ChirpAdd{}, c.processAddChirp)
	newRegistry.Register(mchirp2.ChirpDelete{}, c.processDeleteChirp)

	return newRegistry
}

func (c *Channel) processAddChirp(msg mmessage.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*mchirp2.ChirpAdd)
	if !ok {
		return xerrors.Errorf("message %v isn't a chirp#add message", msgData)
	}

	err := c.verifyChirpMessage(msg, data)
	if err != nil {
		return xerrors.Errorf("failed to verify add chirp message: %v", err)
	}

	return nil
}

func (c *Channel) processDeleteChirp(msg mmessage.Message, msgData interface{},
	_ socket.Socket) error {

	return c.helperProcessDeleteChirp(msg, msgData, true)
}

func (c *Channel) helperProcessDeleteChirp(msg mmessage.Message, msgData interface{}, retry bool) error {
	data, ok := msgData.(*mchirp2.ChirpDelete)
	if !ok {
		return xerrors.Errorf("message %v isn't a chirp#delete message", msgData)
	}

	err := c.verifyChirpMessage(msg, data)
	if err != nil {
		return xerrors.Errorf("failed to verify delete chirp message: %v", err)
	}

	_, b := c.inbox.GetMessage(data.ChirpID)
	if !b {
		// we only allow one retry if for some reason the message to be deleted was not
		// yet added.
		if retry {
			c.log.Info().Msg("message to be deleted was not found, retrying after some delay")
			time.Sleep(retryDelay)
			// process again, without retry
			return c.helperProcessDeleteChirp(msg, msgData, false)
		}
		return xerrors.Errorf("the message to be deleted was not found")
	}
	return nil
}

// verifyMessage checks if a message in a Publish or Broadcast method is valid
func (c *Channel) verifyMessage(msg mmessage.Message) error {
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
		return manswer.NewError(-3, "message already exists")
	}

	return nil
}

func (c *Channel) verifyChirpMessage(msg mmessage.Message, chirpMsg channel.Verifiable) error {
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
		return manswer.NewError(-4, "invalid sender public key")
	}

	if msg.Sender != c.owner {
		return manswer.NewError(-4, "only the owner of the oldchannel can post chirps")
	}

	return nil
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *Channel) broadcastToAllClients(msg mmessage.Message) error {
	rpcMessage := mbroadcast.Broadcast{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodBroadcast,
		},
		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

func (c *Channel) broadcastViaGeneral(msg mmessage.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode the data: %v", err)
	}

	object, action, err := channel.GetObjectAndAction(jsonData)
	action = "notify_" + action
	if err != nil {
		return xerrors.Errorf("failed to read the message data: %v", err)
	}

	timestamp, err := channel.GetTime(jsonData)
	if err != nil {
		return xerrors.Errorf("failed to read the message data: %v", err)
	}

	newData := mchirp2.ChirpBroadcast{
		Object:    object,
		Action:    action,
		ChirpID:   msg.MessageID,
		Channel:   c.generalChannel.GetChannelPath(),
		Timestamp: timestamp,
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

	rpcMessage := mbroadcast.Broadcast{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "broadcast",
		},
		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
		}{
			c.channelID,
			mmessage.Message{
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
		return xerrors.Errorf("the general oldchannel failed to broadcast the chirp message: %v", err)
	}

	return nil
}
