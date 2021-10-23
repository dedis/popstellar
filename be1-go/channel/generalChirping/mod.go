package generalChriping

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"golang.org/x/xerrors"
	"log"
	"popstellar/channel"
	"popstellar/channel/inbox"
	"popstellar/crypto"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
)


// NewChannel returns a new initialized chirping channel
func NewChannel(channelPath string, hub channel.HubFunctionalities) Channel {

	// Saving on election channel too so it self-contains the entire election history
	// electionCh.inbox.storeMessage(msg)
	return Channel{
		sockets:   channel.NewSockets(),
		inbox:     inbox.NewInbox(channelPath),
		channelID: channelPath,
		hub: hub,
	}
}

// Channel is used to handle election messages.
type Channel struct {
	sockets   channel.Sockets
	inbox     *inbox.Inbox
	channelID string
	hub channel.HubFunctionalities
}

// question represents a question in an election.
type chirp struct {

}


// Publish is used to handle a publish message.
func (c *Channel) Publish(msg method.Publish) error {
	log.Printf("nothing should be directly published in the general")
	return xerrors.Errorf("nothing should be directly published in the general")
}


// Broadcast is used to handle publish messages in the election channel.
func (c *Channel) Broadcast(broadcast method.Broadcast) error {
	err := c.VerifyBroadcastMessage(broadcast)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on an "+
			"election channel: %w", err)
	}

	msg := broadcast.Params.Message

	data := msg.Data

	jsonData, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)

	if object == messagedata.ChirpObject {

		switch action {
		case messagedata.ChirpActionAdd:
			err := c.AddChirp(msg)
			if err != nil {
				return xerrors.Errorf("failed to add a chirp to general: %v", err)
			}
		case messagedata.ChirpActionDelete:
			err := c.DeleteChirp(msg)
			if err != nil {
				return xerrors.Errorf("failed to delete the chirp from general: %v", err)
			}
		default:
			return answer.NewInvalidActionError(action)
		}
	}

	if err != nil {
		return xerrors.Errorf("failed to process %q action: %w", action, err)
	}

	c.broadcastToAllClients(msg)

	return nil
}

// Subscribe is used to handle a subscribe message from the client.
func (c *Channel) Subscribe(socket socket.Socket, msg method.Subscribe) error {
	log.Printf("received a subscribe with id: %d", msg.ID)
	c.sockets.Upsert(socket)

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *Channel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	log.Printf("received an unsubscribe with id: %d", msg.ID)

	ok := c.sockets.Delete(socketID)

	if !ok {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	log.Printf("received a catchup with id: %d", catchup.ID)

	return c.inbox.GetSortedMessages()
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *Channel) broadcastToAllClients(msg message.Message) {
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
		log.Printf("failed to marshal broadcast query: %v", err)
	}

	c.sockets.SendToAll(buf)
}

// VerifyBroadcastMessage checks if a Broadcast message is valid
func (c *Channel) VerifyBroadcastMessage(broadcast method.Broadcast) error {
	fmt.Printf("received a brodcast msg")

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
	if _, ok := c.inbox.GetMessage(msg.MessageID); ok {
		return answer.NewError(-3, "message already exists")
	}

	return nil
}


func (c *Channel) AddChirp(msg message.Message) error {
	err := c.getAndVerifyAddChirpMessage(msg)
	if err != nil {
		return xerrors.Errorf("failed to get and verify vote message: %v", err)
	}
	c.inbox.StoreMessage(msg)

	return nil
}

func (c *Channel) DeleteChirp(msg message.Message) error {
	//todo
	return nil
}


func (c *Channel) getAndVerifyAddChirpMessage(msg message.Message) error {
	var chirpMsg messagedata.ChirpAddBroadcast

	err := msg.UnmarshalData(&chirpMsg)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal cast vote: %v", err)
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

	ok := c.hub.GetPubkey().Equal(senderPoint)
	if !ok {
		return answer.NewError(-4, "only organizer can broadcast the chirp messages")
	}

	return nil
}