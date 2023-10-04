package helpers

import (
	"maps"
	"popstellar/channel"
	"popstellar/message/query/method"
	"slices"
	"sync"
)

type Peers struct {
	sync.RWMutex
	// peersInfo stores the info of the peers: public key, client and server endpoints associated with the socket ID
	peersInfo map[string]method.ServerInfo
	// peersGreeted stores the peers that were greeted by the socket ID
	peersGreeted []string
}

func NewPeers() Peers {
	return Peers{
		peersInfo:    make(map[string]method.ServerInfo),
		peersGreeted: make([]string, 0),
	}
}

func (p *Peers) AddPeerInfo(socketId string, info method.ServerInfo) {
	p.Lock()
	defer p.Unlock()
	p.peersInfo[socketId] = info
}

func (p *Peers) AddPeerGreeted(socketId string) {
	p.Lock()
	defer p.Unlock()
	p.peersGreeted = append(p.peersGreeted, socketId)
}

func (p *Peers) GetAllPeersInfo() []method.ServerInfo {
	p.RLock()
	defer p.RUnlock()
	peersInfo := make([]method.ServerInfo, 0)
	for _, info := range p.peersInfo {
		peersInfo = append(peersInfo, info)
	}
	return peersInfo
}

func (p *Peers) IsPeerGreeted(socketId string) bool {
	p.RLock()
	defer p.RUnlock()
	return slices.Contains(p.peersGreeted, socketId)
}

// MessageIds provides a thread-safe structure that stores a channel id with its corresponding message ids
type IdsByChannel struct {
	sync.RWMutex
	table map[string][]string
}

func NewIdsByChannel() IdsByChannel {
	return IdsByChannel{
		table: make(map[string][]string),
	}
}

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

func (i *IdsByChannel) AddAll(channel string, ids []string) {
	i.Lock()
	defer i.Unlock()
	i.table[channel] = append(i.table[channel], ids...)
}

func (i *IdsByChannel) GetAll() map[string][]string {
	i.RLock()
	defer i.RUnlock()
	tableCopy := make(map[string][]string)
	maps.Copy(tableCopy, i.table)
	return tableCopy
}

func (i *IdsByChannel) IsEmpty() bool {
	i.RLock()
	defer i.RUnlock()

	return len(i.table) == 0
}

type ChannelByID struct {
	sync.RWMutex
	table map[string]channel.Channel
}

func NewChannelById() ChannelByID {
	return ChannelByID{
		table: make(map[string]channel.Channel),
	}
}
