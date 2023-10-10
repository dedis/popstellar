package hub_state

import (
	"popstellar/message/query/method"
	"sync"
)

// Queries let the hub remember all queries that it sent to other servers
type Queries struct {
	sync.Mutex
	// state stores the ID of the server's queries and their state. False for a
	// query not yet answered, else true.
	state map[int]*bool
	// getMessagesByIdQueries stores the server's getMessagesByIds queries by their ID.
	getMessagesByIdQueries map[int]method.GetMessagesById
	// nextID store the ID of the next query
	nextID int
}

// NewQueries creates a new queries struct
func NewQueries() Queries {
	return Queries{
		state:                  make(map[int]*bool),
		getMessagesByIdQueries: make(map[int]method.GetMessagesById),
	}
}

// GetQueryState returns a given query's state
func (q *Queries) GetQueryState(id int) *bool {
	q.Lock()
	defer q.Unlock()

	return q.state[id]
}

// GetNextID returns the next query ID
func (q *Queries) GetNextID() int {
	q.Lock()
	defer q.Unlock()

	id := q.nextID
	q.nextID++
	return id
}

// SetQueryState sets the state of the query with the given ID
func (q *Queries) SetQueryState(id int, state bool) {
	q.Lock()
	defer q.Unlock()

	q.state[id] = &state
}

// AddQuery adds the given query to the table
func (q *Queries) AddQuery(id int, query method.GetMessagesById) {
	q.Lock()
	defer q.Unlock()

	q.getMessagesByIdQueries[id] = query
}
