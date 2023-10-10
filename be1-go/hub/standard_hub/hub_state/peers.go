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

// NewPeers creates a new Peers structure
func NewPeers() Peers {
	return Peers{
		peersInfo:    make(map[string]method.ServerInfo),
		peersGreeted: make([]string, 0),
	}
}

// AddPeerInfo adds a peer's info to the table
func (p *Peers) AddPeerInfo(socketId string, info method.ServerInfo) {
	p.Lock()
	defer p.Unlock()
	p.peersInfo[socketId] = info
}

// AddPeerGreeted adds a peer's socket ID to the slice of peers greeted
func (p *Peers) AddPeerGreeted(socketId string) {
	p.Lock()
	defer p.Unlock()
	if slices.Contains(p.peersGreeted, socketId) {
		return
	}
	p.peersGreeted = append(p.peersGreeted, socketId)
}

// GetAllPeersInfo returns a copy of the peers' info slice
func (p *Peers) GetAllPeersInfo() []method.ServerInfo {
	p.RLock()
	defer p.RUnlock()
	peersInfo := make([]method.ServerInfo, 0)
	for _, info := range p.peersInfo {
		peersInfo = append(peersInfo, info)
	}
	return peersInfo
}

// IsPeerGreeted returns true if the peer was greeted, otherwise it returns false
func (p *Peers) IsPeerGreeted(socketId string) bool {
	p.RLock()
	defer p.RUnlock()
	return slices.Contains(p.peersGreeted, socketId)
}
