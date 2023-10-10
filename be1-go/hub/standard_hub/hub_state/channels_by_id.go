package hub_state

import (
	"maps"
	"popstellar/channel"
	"sync"
)

// ChannelsById provides a thread-safe structure that stores channel ids with their corresponding channels
type ChannelsById struct {
	sync.RWMutex
	table map[string]channel.Channel
}

func NewChannelsById() ChannelsById {
	return ChannelsById{
		table: make(map[string]channel.Channel),
	}
}

func (c *ChannelsById) Add(channelId string, channel channel.Channel) {
	c.Lock()
	defer c.Unlock()
	c.table[channelId] = channel
}

func (c *ChannelsById) GetChannel(channelId string) (channel.Channel, bool) {
	c.RLock()
	defer c.RUnlock()
	channel, ok := c.table[channelId]
	return channel, ok
}

func (c *ChannelsById) GetAll() map[string]channel.Channel {
	c.RLock()
	defer c.RUnlock()
	channelsCopy := make(map[string]channel.Channel)
	maps.Copy(channelsCopy, c.table)
	return channelsCopy
}
