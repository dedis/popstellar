package hub_state

import (
	"golang.org/x/exp/slices"
	"maps"
	"sync"
)

// IdsByChannel provides a thread-safe structure that stores a channel id with its corresponding message ids
type IdsByChannel struct {
	sync.RWMutex
	table map[string][]string
}

// NewIdsByChannel creates a new IdsByChannel structure
func NewIdsByChannel() IdsByChannel {
	return IdsByChannel{
		table: make(map[string][]string),
	}
}

// Add adds a message id to the table
func (i *IdsByChannel) Add(channel string, id string) {
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

// AddAll adds a slice of message ids to the table
func (i *IdsByChannel) AddAll(channel string, ids []string) {
	i.Lock()
	defer i.Unlock()
	i.table[channel] = append(i.table[channel], ids...)
}

// GetAll returns a copy of the table
func (i *IdsByChannel) GetAll() map[string][]string {
	i.RLock()
	defer i.RUnlock()
	tableCopy := make(map[string][]string)
	maps.Copy(tableCopy, i.table)
	return tableCopy
}

// IsEmpty returns true if the table is empty, otherwise it returns false
func (i *IdsByChannel) IsEmpty() bool {
	i.RLock()
	defer i.RUnlock()

	return len(i.table) == 0
}
