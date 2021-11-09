package serverInbox

import (
	"encoding/json"
	"popstellar/message/query/method"
	"sort"
	"sync"
	"time"
)

// messageInfo wraps a message with a stored time for sorting.
type messageInfo struct {
	message    method.Publish
	storedTime int64
}

// ServerInbox represents an in-memory data store to record incoming messages.
type ServerInbox struct {
	mutex sync.RWMutex
	msgs  map[string]*messageInfo
}

// NewServerInbox returns a new initialized inbox
func NewServerInbox() *ServerInbox {
	return &ServerInbox{
		mutex: sync.RWMutex{},
		msgs:  make(map[string]*messageInfo),
	}
}

// StoreMessage stores a message inside the inbox
func (i *ServerInbox) StoreMessage(publish method.Publish) {
	//	log := be1_go.Logger

	i.mutex.Lock()
	defer i.mutex.Unlock()

	storedTime := time.Now().UnixNano()

	messageInfo := &messageInfo{
		message:    publish,
		storedTime: storedTime,
	}

	i.msgs[publish.Params.Message.MessageID] = messageInfo

	/**
	if sqlite.GetDBPath() != "" {
		log.Info().Msg("storing message into db")

		err := i.storeMessageInDB(messageInfo)
		if err != nil {
			log.Err(err).Msg("failed to store message into db")
		}
	}
	*/
}

// GetSortedMessages returns all messages stored sorted by stored time.
func (i *ServerInbox) GetSortedMessages() []string {
	i.mutex.RLock()
	defer i.mutex.RUnlock()

	messages := make([]messageInfo, 0, len(i.msgs))
	// iterate over map and collect all the values (messageInfo instances)
	for _, msgInfo := range i.msgs {
		messages = append(messages, *msgInfo)
	}

	// sort.Slice on messages based on the timestamp
	sort.Slice(messages, func(i, j int) bool {
		return messages[i].storedTime < messages[j].storedTime
	})

	result := make([]string, len(messages))

	// iterate and extract the messages[i].message field and
	// append it to the result slice
	for i, msgInfo := range messages {
		buf, err := json.Marshal(msgInfo.message)
		if err == nil {
			result[i] = string(buf)
		}
	}

	return result
}

// GetMessage returns the message of messageID if it exists. We need a pointer
// on message to add witness signatures.
func (i *ServerInbox) GetMessage(messageID string) (*method.Publish, bool) {
	i.mutex.Lock()
	defer i.mutex.Unlock()

	msgInfo, ok := i.msgs[messageID]
	if !ok {
		return nil, false
	}
	return &msgInfo.message, true
}

// TODO adapt database oprations to this inbox
