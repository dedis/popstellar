package types

import (
	"popstellar/internal/message/query/method"
	"sync"

	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
)

// Queries let the hub remember all queries that it sent to other servers
type Queries struct {
	sync.Mutex
	// state stores the ID of the server's queries and their state. False for a
	// query not yet answered, else true.
	state map[int]bool
	// getMessagesByIdQueries stores the server's getMessagesByIds queries by their ID.
	getMessagesByIdQueries map[int]method.GetMessagesById
	getRumorQueries        map[int]method.Rumor

	// nextID store the ID of the next query
	nextID int
	// zerolog
	log *zerolog.Logger
}

// NewQueries creates a new queries struct
func NewQueries(log *zerolog.Logger) *Queries {
	return &Queries{
		state:                  make(map[int]bool),
		getMessagesByIdQueries: make(map[int]method.GetMessagesById),
		getRumorQueries:        make(map[int]method.Rumor),
		log:                    log,
	}
}

// GetQueryState returns a given query's state
func (q *Queries) GetQueryState(id int) (bool, error) {
	q.Lock()
	defer q.Unlock()

	state, ok := q.state[id]
	if !ok {
		return false, xerrors.Errorf("query with id %d not found", id)
	}
	return state, nil
}

// GetNextID returns the next query ID
func (q *Queries) GetNextID() int {
	q.Lock()
	defer q.Unlock()

	id := q.nextID
	q.nextID++
	return id
}

// SetQueryReceived sets the state of the query with the given ID as received
func (q *Queries) SetQueryReceived(id int) error {
	q.Lock()
	defer q.Unlock()

	currentState, ok := q.state[id]

	if !ok {
		return xerrors.Errorf("query with id %d not found", id)
	}

	if currentState {
		q.log.Info().Msgf("query with id %d already answered", id)
		return nil
	}

	q.state[id] = true
	return nil
}

// AddQuery adds the given query to the table
func (q *Queries) AddQuery(id int, query method.GetMessagesById) {
	q.Lock()
	defer q.Unlock()

	q.getMessagesByIdQueries[id] = query
	q.state[id] = false
}

func (q *Queries) AddRumorQuery(id int, query method.Rumor) {
	q.Lock()
	defer q.Unlock()

	q.getRumorQueries[id] = query
	q.state[id] = false
}

func (q *Queries) IsRumorQuery(queryID int) bool {
	q.Lock()
	defer q.Unlock()

	_, ok := q.getRumorQueries[queryID]

	return ok
}

func (q *Queries) GetRumorFromPastQuery(queryID int) (method.Rumor, bool) {
	q.Lock()
	defer q.Unlock()

	rumor, ok := q.getRumorQueries[queryID]
	if !ok {
		return method.Rumor{}, false
	}

	return rumor, true
}