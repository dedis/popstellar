package inbox

import (
	state "popstellar/hub/standard_hub/hub_state"
	"popstellar/message/query/method/message"
	"sync"
)

const rootChannel = "/root"

// HubInbox represents an inbox for the hub for storing messages and ids atomically
type HubInbox struct {
	sync.RWMutex
	Inbox
	// messageIdsByChannel stores all the message ids and the corresponding channel ids
	// to help servers determine in which channel the message ids go
	messageIdsByChannel state.MessageIds
	rootMessages        []message.Message
}

// NewHubInbox creates a new HubInbox
func NewHubInbox(channelID string) *HubInbox {
	return &HubInbox{
		Inbox:               *NewInbox(channelID),
		messageIdsByChannel: state.NewMessageIdsMap(),
		rootMessages:        make([]message.Message, 0),
	}
}

// StoreMessage stores a message inside the inbox and adds the message id to the map
func (i *HubInbox) StoreMessage(channel string, msg message.Message) {
	i.Lock()
	defer i.Unlock()
	if channel == rootChannel {
		i.rootMessages = append(i.rootMessages, msg)
	}
	i.Inbox.StoreMessage(msg)
	i.messageIdsByChannel.Add(channel, msg.MessageID)
}

// IsEmpty returns true if the inbox is empty
func (i *HubInbox) IsEmpty() bool {
	i.RLock()
	defer i.RUnlock()
	return i.messageIdsByChannel.IsEmpty()
}

// GetIDsTable returns the table of message ids by channel
func (i *HubInbox) GetIDsTable() map[string][]string {
	i.RLock()
	defer i.RUnlock()
	return i.messageIdsByChannel.GetTable()
}

// GetSortedRootMessages returns the root messages
func (i *HubInbox) GetSortedRootMessages() []message.Message {
	i.RLock()
	defer i.RUnlock()
	res := make([]message.Message, len(i.rootMessages))
	copy(res, i.rootMessages)
	return res
}
