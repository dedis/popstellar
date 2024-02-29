package hub_state

import (
	"golang.org/x/xerrors"
	"popstellar/message/query/method"
	"sync"

	"golang.org/x/exp/maps"
	"golang.org/x/exp/slices"
)

// Peers stores the peers' information
type Peers struct {
	sync.RWMutex
	// peersInfo stores the info of the peers: public key, client and server endpoints associated with the socket ID
	peersInfo map[string]method.ServerInfo
	// peersGreeted stores the peers that were greeted by the socket ID
	peersGreeted map[string]struct{}
}

// NewPeers creates a new Peers structure
func NewPeers() Peers {
	return Peers{
		peersInfo:    make(map[string]method.ServerInfo),
		peersGreeted: make(map[string]struct{}),
	}
}

// AddPeerInfo adds a peer's info to the table
func (p *Peers) AddPeerInfo(socketId string, info method.ServerInfo) error {
	p.Lock()
	defer p.Unlock()

	currentInfo, ok := p.peersInfo[socketId]
	if ok {
		return xerrors.Errorf(
			"cannot add %s because peersInfo[%s] already contains %s",
			info, socketId, currentInfo)
	}

	p.peersInfo[socketId] = info
	return nil
}

// AddPeerGreeted adds a peer's socket ID to the slice of peers greeted
func (p *Peers) AddPeerGreeted(socketId string) {
	p.Lock()
	defer p.Unlock()
	p.peersGreeted[socketId] = struct{}{}
}

// GetAllPeersInfo returns a copy of the peers' info slice
func (p *Peers) GetAllPeersInfo() []method.ServerInfo {
	p.RLock()
	defer p.RUnlock()
	peersInfo := make([]method.ServerInfo, 0, len(p.peersInfo))
	for _, info := range p.peersInfo {
		if !slices.Contains(peersInfo, info) {
			peersInfo = append(peersInfo, info)
		}
	}
	return peersInfo
}

// IsPeerGreeted returns true if the peer was greeted, otherwise it returns false
func (p *Peers) IsPeerGreeted(socketId string) bool {
	p.RLock()
	defer p.RUnlock()
	return slices.Contains(maps.Keys(p.peersGreeted), socketId)
}
