package reaction

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

	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
)

const (
	msgID              = "msg id"
	failedToDecodeData = "failed to decode message data: %v"
	retryDelay         = 100 * time.Millisecond
)

// Channel is used to handle reaction messages.
type Channel struct {
	sockets   channel.Sockets
	inbox     *inbox.Inbox
	attendees *attendees

	// channel path
	channelID string

	hub channel.HubFunctionalities
	log zerolog.Logger

	registry registry.MessageRegistry
}

// NewChannel returns a new initialized reaction channel
func NewChannel(channelPath string, hub channel.HubFunctionalities, log zerolog.Logger) *Channel {
	log = log.With().Str("channel", "reaction").Logger()

	newChannel := &Channel{
		sockets:   channel.NewSockets(),
		inbox:     inbox.NewInbox(channelPath),
		channelID: channelPath,
		attendees: newAttendees(),
		hub:       hub,
		log:       log,
	}

	newChannel.registry = newChannel.NewReactionRegistry()

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

// Publish is used to handle publish messages in the reaction channel.
func (c *Channel) Publish(publish method.Publish, socket socket.Socket) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	err := c.verifyMessage(publish.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on a "+
			"reaction channel: %w", err)
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
			"reaction channel: %w", err)
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

// NewReactionRegistry creates a new registry for the consensus channel
func (c *Channel) NewReactionRegistry() registry.MessageRegistry {
	registry := registry.NewMessageRegistry()

	registry.Register(messagedata.ReactionAdd{}, c.processReactionAdd)
	registry.Register(messagedata.ReactionDelete{}, c.processReactionDelete)

	return registry
}

// processReactionAdd is the callback that processes reaction#add messages
func (c *Channel) processReactionAdd(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	err := c.verifyAddReactionMessage(msg)
	if err != nil {
		return xerrors.Errorf("failed to verify add reaction message: %v", err)
	}

	return nil
}

// processReactionDelete is the callback that processes reaction#delete messages
func (c *Channel) processReactionDelete(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	err := c.verifyDeleteReactionMessage(msg, true)
	if err != nil {
		return xerrors.Errorf("failed to verify delete reaction message: %v", err)
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

func (c *Channel) verifyAddReactionMessage(msg message.Message) error {
	var reactMsg messagedata.ReactionAdd

	err := msg.UnmarshalData(&reactMsg)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal: %v", err)
	}

	err = reactMsg.Verify()
	if err != nil {
		return xerrors.Errorf("invalid add reaction message: %v", err)
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

	if !c.attendees.isPresent(msg.Sender) {
		return answer.NewError(-4, "the sender's PoP token was not verified in a roll-call")
	}

	return nil
}

func (c *Channel) verifyDeleteReactionMessage(msg message.Message, retry bool) error {
	var delReactMsg messagedata.ReactionDelete

	err := msg.UnmarshalData(&delReactMsg)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal: %v", err)
	}

	err = delReactMsg.Verify()
	if err != nil {
		return xerrors.Errorf("invalid delete reaction message: %v", err)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	react, b := c.inbox.GetMessage(delReactMsg.ReactionID)
	if !b {
		if retry {
			time.Sleep(retryDelay)
			return c.verifyDeleteReactionMessage(msg, false)
		}
		return xerrors.Errorf("the message to be deleted was not found")
	}

	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewError(-4, "invalid sender public key")
	}

	if msg.Sender != react.Sender {
		return answer.NewError(-4, "only the owner of the reaction can delete it")
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

// ---
// Others
// ---

// AddAttendee adds an attendee to the reaction channel.
func (c *Channel) AddAttendee(key string) {
	c.attendees.add(key)
}

// attendees represents the attendees to a roll-call.
type attendees struct {
	sync.Mutex
	store map[string]struct{}
}

// newAttendees returns a new instance of attendees.
func newAttendees() *attendees {
	return &attendees{
		store: make(map[string]struct{}),
	}
}

// isPresent checks if a key representing a user is present in
// the list of attendees.
func (a *attendees) isPresent(key string) bool {
	a.Lock()
	defer a.Unlock()

	_, ok := a.store[key]

	return ok
}

// add adds an attendee to the list of attendees.
func (a *attendees) add(key string) {
	a.Lock()
	defer a.Unlock()

	a.store[key] = struct{}{}
}
