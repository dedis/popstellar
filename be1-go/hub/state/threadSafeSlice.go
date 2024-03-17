package state

import (
	"golang.org/x/exp/slices"
	"sync"
)

type ThreadSafeSlice[E comparable] struct {
	sync.RWMutex
	els []E
}

func NewThreadSafeSlice[E comparable]() ThreadSafeSlice[E] {
	return ThreadSafeSlice[E]{
		els: make([]E, 0),
	}
}

func (i *ThreadSafeSlice[E]) Append(elems ...E) {
	i.Lock()
	defer i.Unlock()
	i.els = append(i.els, elems...)
}

func (i *ThreadSafeSlice[E]) Contains(elem E) bool {
	i.RLock()
	defer i.RUnlock()
	return slices.Contains(i.els, elem)
}
