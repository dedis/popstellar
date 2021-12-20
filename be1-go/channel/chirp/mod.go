package chirp

import (
	"encoding/base64"
	"encoding/json"
	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
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
)

const msgID = "msg id"

// NewChannel returns a new initialized individual chirping channel
func NewChannel(channelPath string, ownerKey string, hub channel.HubFunctionalities,
	generalChannel channel.Broadcastable, log zerolog.Logger) Channel {

	log = log.With().Str("channel", "chirp").Logger()

	return Channel{
		sockets:        channel.NewSockets(),
		inbox:          inbox.NewInbox(channelPath),
		channelID:      channelPath,
		generalChannel: generalChannel,
		owner:          ownerKey,
		hub:            hub,
		log:            log,
	}
}

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
}

// Publish is used to handle publish messages in the chirp channel.
func (c *Channel) Publish(publish method.Publish, socket socket.Socket) error {
	err := c.verifyPublishMessage(publish)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on a "+
			"chirping channel: %w", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	jsonData, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return xerrors.Errorf("failed to get object and action from message data: %v", err)
	}

	if object != messagedata.ChirpObject {
		return xerrors.Errorf("object should be 'chirp' but is %s", object)
	}

	switch action {
	case messagedata.ChirpActionAdd:
		err := c.publishAddChirp(msg)
		if err != nil {
			return xerrors.Errorf("failed to publish chirp: %v", err)
		}
	case messagedata.ChirpActionDelete:
		err := c.publishDeleteChirp(msg)
		if err != nil {
			return xerrors.Errorf("failed to delete chirp: %v", err)
		}
	default:
		return answer.NewInvalidActionError(action)
	}

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

func (c *Channel) broadcastViaGeneral(msg message.Message) error {

	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode the data: %v", err)
	}

	object, _, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return xerrors.Errorf("failed to read the message data: %v", err)
	}

	time, err := messagedata.GetTime(jsonData)
	if err != nil {
		return xerrors.Errorf("failed to read the message data: %v", err)
	}

	newData := messagedata.ChirpBroadcast{
		Object:    object,
		Action:    "add_broadcast",
		ChirpId:   msg.MessageID,
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

	err = c.generalChannel.Broadcast(rpcMessage)
	if err != nil {
		return xerrors.Errorf("the general channel failed to broadcast the chirp message: %v", err)
	}

	return nil
}

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

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	c.log.Info().
		Str(msgID, strconv.Itoa(catchup.ID)).
		Msg("received a catchup")

	return c.inbox.GetSortedMessages()
}

// Broadcast is used to handle a broadcast message.
func (c *Channel) Broadcast(msg method.Broadcast) error {
	c.log.Error().
		Str(msgID, msg.Params.Message.MessageID).
		Msg("chirp channel should not need to broadcast")
	return xerrors.Errorf("chirp channel should not need to broadcast")
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

// VerifyPublishMessage checks if a Publish message is valid
func (c *Channel) verifyPublishMessage(publish method.Publish) error {
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
	_, ok := c.inbox.GetMessage(msg.MessageID)
	if ok {
		return answer.NewError(-3, "message already exists")
	}

	return nil
}

func (c *Channel) publishAddChirp(msg message.Message) error {
	err := c.verifyAddChirpMessage(msg)
	if err != nil {
		return xerrors.Errorf("failed to verify add chirp message: %v", err)
	}
	c.inbox.StoreMessage(msg)
	return nil
}

func (c *Channel) publishDeleteChirp(msg message.Message) error {
	err := c.verifyDeleteChirpMessage(msg)
	if err != nil {
		return xerrors.Errorf("failed to verify delete chirp message: %v", err)
	}
	c.inbox.StoreMessage(msg)
	return nil
}

func (c *Channel) verifyAddChirpMessage(msg message.Message) error {
	var chirpMsg messagedata.ChirpAdd

	err := msg.UnmarshalData(&chirpMsg)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal: %v", err)
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

func (c *Channel) verifyDeleteChirpMessage(msg message.Message) error {
	var chirpMsg messagedata.ChirpDelete

	err := msg.UnmarshalData(&chirpMsg)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal: %v", err)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	_, b := c.inbox.GetMessage(chirpMsg.ChirpId)
	if !b {
		return xerrors.Errorf("the message to be deleted was not found")
	}

	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewError(-4, "invalid sender public key")
	}

	if msg.Sender != c.owner {
		return answer.NewError(-4, "only the owner of the channel can delete chirps")
	}

	return nil
}
