package hub_state

import (
	"popstellar/internal/old/oldchannel"
)

// Channels stores channel ids with their corresponding channels
type Channels struct {
	ThreadSafeMap[string, oldchannel.Channel]
}

// NewChannelsMap creates a new Channels structure
func NewChannelsMap() Channels {
	return Channels{
		ThreadSafeMap: NewThreadSafeMap[string, oldchannel.Channel](),
	}
}

// ForEach iterates over all channels and applies the given function
func (c *Channels) ForEach(f func(oldchannel.Channel)) {
	c.Lock()
	defer c.Unlock()
	for _, channel := range c.table {
		f(channel)
	}
}
