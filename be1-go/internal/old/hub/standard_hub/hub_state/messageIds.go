package hub_state

import (
	"golang.org/x/exp/slices"
)

// MessageIds stores a channel id with its corresponding message ids
type MessageIds struct {
	ThreadSafeMap[string, []string]
}

// NewMessageIdsMap creates a new MessageIds structure
func NewMessageIdsMap() MessageIds {
	return MessageIds{
		ThreadSafeMap: NewThreadSafeMap[string, []string](),
	}
}

// Add adds a message id to the slice of message ids of the channel
func (i *MessageIds) Add(channel string, id string) {
	i.Lock()
	defer i.Unlock()
	messageIds, channelStored := i.table[channel]
	if !channelStored {
		i.table[channel] = append(i.table[channel], id)
		return
	}
	alreadyStoredId := slices.Contains(messageIds, id)
	if !alreadyStoredId {
		i.table[channel] = append(i.table[channel], id)
	}
}

// AddAll adds a slice of message ids to the slice of message ids of the channel
func (i *MessageIds) AddAll(channel string, ids []string) {
	i.Lock()
	defer i.Unlock()
	i.table[channel] = append(i.table[channel], ids...)
}
