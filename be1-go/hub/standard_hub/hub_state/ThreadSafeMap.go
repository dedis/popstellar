package hub_state

import "sync"

type ThreadSafeMap[K comparable, V any] struct {
	sync.RWMutex
	table map[K]V
}

func NewThreadSafeMap[K comparable, V any]() ThreadSafeMap[K, V] {
	return ThreadSafeMap[K, V]{
		table: make(map[K]V),
	}
}

func (i *ThreadSafeMap[K, V]) Get(key K) (V, bool) {
	i.RLock()
	defer i.RUnlock()
	val, ok := i.table[key]
	return val, ok
}

func (i *ThreadSafeMap[K, V]) Set(key K, val V) {
	i.Lock()
	defer i.Unlock()
	i.table[key] = val
}

// GetTable returns a shallow copy of the map
func (i *ThreadSafeMap[K, V]) GetTable() map[K]V {
	i.Lock()
	defer i.Unlock()
	tableCopy := make(map[K]V, len(i.table))
	for key, val := range i.table {
		tableCopy[key] = val
	}
	return tableCopy
}

func (i *ThreadSafeMap[K, V]) IsEmpty() bool {
	i.Lock()
	defer i.Unlock()
	return len(i.table) == 0
}
