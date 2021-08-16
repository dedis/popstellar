// Package concurrent defines data structures for keeping
// track of open go-routines and returning from them cleanly
// when the application is shut down.
package concurrent

import (
	"sync"
)

// WaitGroup interface abstracts custom implementations
// of sync.WaitGroup
type WaitGroup interface {
	// Add adds delta count to the WaitGroup.
	Add(delta int)

	// Done decrements the count of the WaitGroup by one.
	Done()

	// Wait blocks on the WaitGroup to reach a count of 0.
	Wait()
}

// Rendezvous implements a WaitGroup using a sync.Cond.
type Rendezvous struct {
	count int
	cond  *sync.Cond
}

func NewRendezvous() *Rendezvous {
	return &Rendezvous{
		cond: sync.NewCond(&sync.Mutex{}),
	}
}

func (r *Rendezvous) Add(delta int) {
	r.cond.L.Lock()
	defer r.cond.L.Unlock()

	r.count++
}

func (r *Rendezvous) Wait() {
	r.cond.L.Lock()

	for r.count > 0 {
		r.cond.Wait()
	}

	r.cond.L.Unlock()
}

// Done decrements the WaitGroup count by one.
func (r *Rendezvous) Done() {
	r.cond.L.Lock()
	r.count--
	r.cond.L.Unlock()

	r.cond.Broadcast()
}
