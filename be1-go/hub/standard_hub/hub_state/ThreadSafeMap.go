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

func (t *ThreadSafeMap[K, V]) Get(key K) (V, bool) {
	t.RLock()
	defer t.RUnlock()
	val, ok := t.table[key]
	return val, ok
}

func (t *ThreadSafeMap[K, V]) Set(key K, val V) {
	t.Lock()
	defer t.Unlock()
	t.table[key] = val
}

func (t *ThreadSafeMap[K, V]) GetTable() map[K]V {
	t.Lock()
	defer t.Unlock()
	tableCopy := make(map[K]V)
	for key, val := range t.table {
		tableCopy[key] = val
	}
	return tableCopy
}

func (t *ThreadSafeMap[K, V]) IsEmpty() bool {
	t.Lock()
	defer t.Unlock()
	return len(t.table) == 0
}
