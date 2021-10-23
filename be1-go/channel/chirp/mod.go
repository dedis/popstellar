package chirp

import (
	"encoding/base64"
	"encoding/json"
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
func NewChannel(channelPath string,
	ownerKey string, hub channel.HubFunctionalities, generalChannel channel.Channel) Channel {

	// Saving on election channel too so it self-contains the entire election history
	// electionCh.inbox.storeMessage(msg)
	return Channel{
		sockets:   channel.NewSockets(),
		inbox:     inbox.NewInbox(channelPath),
		channelID: channelPath,
		generalChannel: generalChannel,
		owner: ownerKey,
		hub: hub,
	}
}

// Channel is used to handle election messages.
type Channel struct {
	sockets   channel.Sockets
	inbox     *inbox.Inbox
	channelID string
	generalChannel channel.Channel
	owner string
	hub channel.HubFunctionalities
}


// Publish is used to handle publish messages in the election channel.
func (c *Channel) Publish(publish method.Publish) error {
	err := c.VerifyPublishMessage(publish)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on an "+
			"election channel: %w", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	jsonData, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)

	if object == messagedata.ChirpObject {

		switch action {
		case messagedata.ChirpActionAdd:
			err := c.publishAddChirp(msg)
			if err != nil {
				return xerrors.Errorf("failed to cast vote: %v", err)
			}
		case messagedata.ChirpActionDelete:
			err := c.publishDeleteChirp(msg)
			if err != nil {
				return xerrors.Errorf("failed to end election: %v", err)
			}

		default:
			return answer.NewInvalidActionError(action)
		}
	}

	if err != nil {
		return xerrors.Errorf("failed to process %q action: %w", action, err)
	}

	c.broadcastViaGeneral(msg)

	return nil
}

func (c *Channel) broadcastViaGeneral(msg message.Message) error {

	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}


	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return xerrors.Errorf("failed to read the message data", err)
	}

	time, err := messagedata.GetTime(jsonData)
	if err != nil {
		return xerrors.Errorf("failed to read the message data", err)
	}


	newData := messagedata.ChirpAddBroadcast{
		Object:    object,
		Action:    action,
		PostId:    msg.MessageID,
		Channel:   c.channelID,
		Timestamp: time,
	}

	dataBuf, err := json.Marshal(newData)
	if err != nil {
		return xerrors.Errorf("failed to marshal the data", err)
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

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
				Data: newData64,
				Sender: msg.Sender,
				Signature: msg.Signature,
				MessageID: msg.MessageID,
				WitnessSignatures: msg.WitnessSignatures,
			},
		},
	}

	c.generalChannel.Broadcast(rpcMessage)

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

// Broadcast is used to handle a broadcast message.
func (c *Channel) Broadcast(msg method.Broadcast) error {
	log.Printf("a lao shouldn't need to broadcast a message")
	return xerrors.Errorf("a lao shouldn't need to broadcast a message")
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

// VerifyPublishMessage checks if a Publish message is valid
func (c *Channel) VerifyPublishMessage(publish method.Publish) error {
	log.Printf("received a publish with id: %d", publish.ID)

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

func (c *Channel) publishAddChirp(msg message.Message) error {
	err := c.getAndVerifyAddChirpMessage(msg)
	if err != nil {
		return xerrors.Errorf("failed to get and verify vote message: %v", err)
	}
	c.inbox.StoreMessage(msg)
	return nil
}

func (c *Channel) publishDeleteChirp(msg message.Message) error {
	//todo
	return nil
}


func (c *Channel) getAndVerifyAddChirpMessage(msg message.Message) error {
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
	//TODO
	//ok := c.owner == senderPoint
	//if !ok {
	//return answer.NewError(-4, "only organizer can broadcast the chirp messages")
	//}

	return nil
}
