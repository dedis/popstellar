package hub

import (
	"encoding/json"
	"fmt"
	"log"
	"sort"
	"student20_pop/message"
	"student20_pop/network/socket"
	"student20_pop/validation"
	"sync"
)

// baseChannel represent a generic channel and contains all the fields that are
// used in all channels
type baseChannel struct {
	hub *baseHub

	socketsMu sync.RWMutex
	sockets   map[string]socket.Socket

	inbox *inbox

	// /root/<ID>
	channelID string

	witnessMu sync.Mutex
	witnesses []message.PublicKey
}

type messageInfo struct {
	message    *message.Message
	storedTime message.Timestamp
}

// CreateBaseChannel return an instance of a `baseChannel`
func createBaseChannel(h *baseHub, channelID string) *baseChannel {
	return &baseChannel{
		hub:       h,
		channelID: channelID,
		sockets:   make(map[string]socket.Socket),
		inbox:     createInbox(channelID),
	}
}

// Subscribe is used to handle a subscribe message from the client.
func (c *baseChannel) Subscribe(socket socket.Socket, msg message.Subscribe) error {
	log.Printf("received a subscribe with id: %d", msg.ID)
	c.socketsMu.Lock()
	defer c.socketsMu.Unlock()

	c.sockets[socket.ID()] = socket

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *baseChannel) Unsubscribe(socketID string, msg message.Unsubscribe) error {
	log.Printf("received an unsubscribe with id: %d", msg.ID)

	c.socketsMu.Lock()
	defer c.socketsMu.Unlock()

	if _, ok := c.sockets[socketID]; !ok {
		return &message.Error{
			Code:        -2,
			Description: "client is not subscribed to this channel",
		}
	}

	delete(c.sockets, socketID)
	return nil
}

// Catchup is used to handle a catchup message.
func (c *baseChannel) Catchup(catchup message.Catchup) []message.Message {
	log.Printf("received a catchup with id: %d", catchup.ID)

	c.inbox.mutex.RLock()
	defer c.inbox.mutex.RUnlock()

	messages := make([]messageInfo, 0, len(c.inbox.msgs))
	// iterate over map and collect all the values (messageInfo instances)
	for _, msgInfo := range c.inbox.msgs {
		messages = append(messages, *msgInfo)
	}

	// sort.Slice on messages based on the timestamp
	sort.Slice(messages, func(i, j int) bool {
		return messages[i].storedTime < messages[j].storedTime
	})

	result := make([]message.Message, 0, len(c.inbox.msgs))

	// iterate and extract the messages[i].message field and
	// append it to the result slice
	for _, msgInfo := range messages {
		result = append(result, *msgInfo.message)
	}

	return result
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *baseChannel) broadcastToAllClients(msg message.Message) {
	c.socketsMu.RLock()
	defer c.socketsMu.RUnlock()

	query := message.Query{
		Broadcast: message.NewBroadcast(c.channelID, &msg),
	}

	buf, err := json.Marshal(query)
	if err != nil {
		log.Fatalf("failed to marshal broadcast query: %v", err)
	}

	for _, socket := range c.sockets {
		socket.Send(buf)
	}
}

// VerifyPublishMessage checks if a Publish message is valid
func (c *baseChannel) VerifyPublishMessage(publish message.Publish) error {
	log.Printf("received a publish with id: %d", publish.ID)

	// Check if the structure of the message is correct
	msg := publish.Params.Message

	// Verify the data
	err := c.hub.schemaValidator.VerifyJson(msg.RawData, validation.Data)
	if err != nil {
		return message.NewError("failed to validate the data", err)
	}

	// Unmarshal the data
	err = msg.VerifyAndUnmarshalData()
	if err != nil {
		// Return a error of type "-4 request data is invalid" for all the verifications and unmarshalling problems of the data
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to verify and unmarshal data: %v", err),
		}
	}

	// Check if the message already exists
	if _, ok := c.inbox.getMessage(msg.MessageID); ok {
		return &message.Error{
			Code:        -3,
			Description: "message already exists",
		}
	}

	return nil
}
