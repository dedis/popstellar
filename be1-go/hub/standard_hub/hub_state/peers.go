package hub_state

import (
	"golang.org/x/exp/slices"
	"popstellar/message/query/method"
	"sync"
)

// Peers provides a thread-safe structure that stores the peers' information
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
	if slices.Contains(p.peersGreeted, socketId) {
		return
	}
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
