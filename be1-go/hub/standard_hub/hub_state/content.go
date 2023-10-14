package hub_state

import (
	"golang.org/x/exp/slices"
	"popstellar/channel"
)

// Channels provides a thread-safe structure that stores channel ids with their corresponding channels
type Channels ThreadSafeMap[string, channel.Channel]

// MessageIds provides a thread-safe structure that stores a channel id with its corresponding message ids
type MessageIds ThreadSafeMap[string, []string]

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

func (i *MessageIds) AddAll(channel string, ids []string) {
	i.Lock()
	defer i.Unlock()
	i.table[channel] = append(i.table[channel], ids...)
}
