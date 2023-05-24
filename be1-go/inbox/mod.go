package inbox

import (
	"sort"
	"sync"
	"time"

	"popstellar/message/query/method/message"
)

// messageInfo wraps a message with a stored time for sorting.
type messageInfo struct {
	message    message.Message
	storedTime int64
}

// Inbox represents an in-memory data store to record incoming messages.
type Inbox struct {
	mutex             sync.RWMutex
	msgsMap           map[string]*messageInfo
	msgsArray         []*messageInfo
	channelID         string
	pendingSignatures map[string][]message.WitnessSignature
}

// NewInbox returns a new initialized inbox
func NewInbox(channelID string) *Inbox {
	return &Inbox{
		mutex:             sync.RWMutex{},
		msgsMap:           make(map[string]*messageInfo),
		msgsArray:         make([]*messageInfo, 0),
		channelID:         channelID,
		pendingSignatures: make(map[string][]message.WitnessSignature),
	}
}

// AddWitnessSignature adds a signature of witness to a message of ID
// `messageID`. If the message was not yet received, the signature is added
// to the pending signatures map.
func (i *Inbox) AddWitnessSignature(messageID string, public string, signature string) {
	msg, ok := i.GetMessage(messageID)
	i.mutex.Lock()
	defer i.mutex.Unlock()
	if !ok {
		// Add the signature to the pending signatures
		i.pendingSignatures[messageID] = append(i.pendingSignatures[messageID], message.WitnessSignature{
			Witness:   public,
			Signature: signature,
		})
	} else {
		msg.WitnessSignatures = append(msg.WitnessSignatures, message.WitnessSignature{
			Witness:   public,
			Signature: signature,
		})
	}
}

// StoreMessage stores a message inside the inbox
func (i *Inbox) StoreMessage(msg message.Message) {
	i.mutex.Lock()
	defer i.mutex.Unlock()

	storedTime := time.Now().UnixNano()

	// Check if we have pending signatures for this message and add them
	if pendingSignatures, exist := i.pendingSignatures[msg.MessageID]; exist {
		msg.WitnessSignatures = append(msg.WitnessSignatures, pendingSignatures...)
		delete(i.pendingSignatures, msg.MessageID)
	}

	messageInfo := &messageInfo{
		message:    msg,
		storedTime: storedTime,
	}

	i.msgsMap[msg.MessageID] = messageInfo
	i.msgsArray = append(i.msgsArray, messageInfo)
}

// GetSortedMessages returns all messages stored sorted by stored time.
func (i *Inbox) GetSortedMessages() []message.Message {
	i.mutex.RLock()
	defer i.mutex.RUnlock()

	// sort.Slice on messages based on the timestamp
	sort.SliceStable(i.msgsArray, func(k, l int) bool {
		return i.msgsArray[k].storedTime < i.msgsArray[l].storedTime
	})

	result := make([]message.Message, len(i.msgsArray))

	// iterate and extract the messages[i].message field and
	// append it to the result slice
	for i, msgInfo := range i.msgsArray {
		result[i] = msgInfo.message
	}

	return result
}

// GetMessage returns the message of messageID if it exists. We need a pointer
// on message to add witness signatures.
func (i *Inbox) GetMessage(messageID string) (*message.Message, bool) {
	i.mutex.Lock()
	defer i.mutex.Unlock()

	msgInfo, ok := i.msgsMap[messageID]
	if !ok {
		return nil, false
	}

	return &msgInfo.message, true
}
