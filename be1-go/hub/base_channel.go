package hub

import (
	"encoding/base64"
	"encoding/json"
	"log"
	"sort"
	"student20_pop/message"
	"student20_pop/message2"
	"student20_pop/message2/query"
	"student20_pop/message2/query/method"
	messageX "student20_pop/message2/query/method/message"
	"student20_pop/network/socket"
	"student20_pop/validation"
	"sync"

	"golang.org/x/xerrors"
)

type sockets struct {
	sync.RWMutex
	store map[string]socket.Socket
}

// Upsert upserts a socket into the sockets store.
func (s *sockets) Upsert(socket socket.Socket) {
	s.Lock()
	defer s.Unlock()

	s.store[socket.ID()] = socket
}

// Delete deletes a socket from the store. Returns false
// if the socket is not present in the store and true
// on success.
func (s *sockets) Delete(ID string) bool {
	s.Lock()
	defer s.Unlock()

	_, ok := s.store[ID]
	if !ok {
		return false
	}

	delete(s.store, ID)
	return true
}

// baseChannel represent a generic channel and contains all the fields that are
// used in all channels
type baseChannel struct {
	hub *baseHub

	sockets sockets

	inbox *inbox

	// /root/<ID>
	channelID string

	witnessMu sync.Mutex
	witnesses []message.PublicKey
}

type messageInfo struct {
	message    messageX.Message
	storedTime message.Timestamp
}

// CreateBaseChannel return an instance of a `baseChannel`
func createBaseChannel(h *baseHub, channelID string) *baseChannel {
	return &baseChannel{
		hub:       h,
		channelID: channelID,
		sockets: sockets{
			store: make(map[string]socket.Socket),
		},
		inbox: createInbox(channelID),
	}
}

// Subscribe is used to handle a subscribe message from the client.
func (c *baseChannel) Subscribe(socket socket.Socket, msg method.Subscribe) error {
	log.Printf("received a subscribe with id: %d", msg.ID)
	c.sockets.Upsert(socket)

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *baseChannel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	log.Printf("received an unsubscribe with id: %d", msg.ID)

	ok := c.sockets.Delete(socketID)

	if !ok {
		return message.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *baseChannel) Catchup(catchup method.Catchup) []messageX.Message {
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

	result := make([]messageX.Message, 0, len(c.inbox.msgs))

	// iterate and extract the messages[i].message field and
	// append it to the result slice
	for _, msgInfo := range messages {
		result = append(result, msgInfo.message)
	}

	return result
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *baseChannel) broadcastToAllClients(msg messageX.Message) {
	// query := message.Query{
	// 	Broadcast: message.NewBroadcast(c.channelID, msg),
	// }

	rpcMessage := message2.JSONRPC{
		JSONRPC: "2.0",
		Query: query.Query{
			Method: "broadcast",
			Broadcast: method.Broadcast{
				Params: struct {
					Channel string           `json:"channel"`
					Message messageX.Message `json:"message"`
				}{
					Channel: c.channelID,
					Message: msg,
				},
			},
		},
	}

	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		log.Printf("failed to marshal broadcast query: %v", err)
	}

	c.sockets.RLock()
	defer c.sockets.RUnlock()
	for _, socket := range c.sockets.store {
		socket.Send(buf)
	}
}

// VerifyPublishMessage checks if a Publish message is valid
func (c *baseChannel) VerifyPublishMessage(publish method.Publish) error {
	log.Printf("received a publish with id: %d", publish.ID)

	// Check if the structure of the message is correct
	msg := publish.Params.Message

	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	// Verify the data
	err = c.hub.schemaValidator.VerifyJson(jsonData, validation.Data)
	if err != nil {
		return xerrors.Errorf("failed to verify json schema: %w", err)
	}

	// Unmarshal the data
	// laoID := c.channelID[6:]
	// err = msg.VerifyAndUnmarshalData(laoID)
	// if err != nil {
	// 	// Return a error of type "-4 request data is invalid" for all the verifications and unmarshalling problems of the data
	// 	return message.NewErrorf(-4, "failed to verify and unmarshal data: %v", err)
	// }

	// Check if the message already exists
	if _, ok := c.inbox.getMessage(msg.MessageID); ok {
		return message.NewError(-3, "message already exists")
	}

	return nil
}
