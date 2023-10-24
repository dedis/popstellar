package hub_state

import "popstellar/channel"

// Channels stores channel ids with their corresponding channels
type Channels struct {
	ThreadSafeMap[string, channel.Channel]
}

// ForEach iterates over all channels and applies the given function
func (c *Channels) ForEach(f func(channel.Channel)) {
	c.Lock()
	defer c.Unlock()
	for _, channel := range c.table {
		f(channel)
	}
}
