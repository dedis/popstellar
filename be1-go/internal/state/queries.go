package state

import (
	"github.com/rs/zerolog"
	"popstellar/internal/errors"
	"popstellar/internal/handler/method/rumor/mrumor"
	"sync"
)

const (
	GetMessagesByIDQuery = iota
	RumorQuery
	RumorStateQuery
)

// Queries let the hub remember all queries that it sent to other servers
type Queries struct {
	sync.Mutex

	state map[int]int

	rumorQueries map[int]mrumor.Rumor

	// nextID store the ID of the next query
	nextID int

	// zerolog
	log zerolog.Logger
}

// NewQueries creates a new queries struct
func NewQueries(log zerolog.Logger) *Queries {
	return &Queries{
		state:        make(map[int]int),
		rumorQueries: make(map[int]mrumor.Rumor),
		log:          log.With().Str("module", "queries").Logger(),
	}
}

// GetNextID returns the next query ID
func (q *Queries) GetNextID() int {
	q.Lock()
	defer q.Unlock()

	id := q.nextID
	q.nextID++
	return id
}

func (q *Queries) Remove(id int) {
	q.Lock()
	defer q.Unlock()

	delete(q.state, id)
	delete(q.rumorQueries, id)
}

func (q *Queries) addQuery(id, queryType int) error {
	q.Lock()
	defer q.Unlock()

	_, ok := q.state[id]
	if ok {
		return errors.NewDuplicateResourceError("cannot have two queries with the same id %d", id)
	}

	q.state[id] = queryType

	return nil
}

func (q *Queries) AddGetMessagesByID(id int) error {
	q.Lock()
	defer q.Unlock()

	return q.addQuery(id, GetMessagesByIDQuery)
}

func (q *Queries) AddRumor(id int, query mrumor.Rumor) error {
	q.Lock()
	defer q.Unlock()

	err := q.addQuery(id, RumorQuery)
	if err != nil {
		return err
	}

	q.rumorQueries[id] = query

	return nil
}

func (q *Queries) AddRumorState(id int) error {
	q.Lock()
	defer q.Unlock()

	return q.addQuery(id, RumorStateQuery)
}

func (q *Queries) GetRumor(queryID int) (mrumor.Rumor, bool) {
	q.Lock()
	defer q.Unlock()

	rumor, ok := q.rumorQueries[queryID]
	if !ok {
		return mrumor.Rumor{}, false
	}

	return rumor, true
}

func (q *Queries) IsGetMessagesByID(id int) bool {
	q.Lock()
	defer q.Unlock()

	queryType, ok := q.state[id]
	if !ok {
		return false
	}

	return queryType == GetMessagesByIDQuery
}

func (q *Queries) IsRumor(id int) bool {
	q.Lock()
	defer q.Unlock()

	queryType, ok := q.state[id]
	if !ok {
		return false
	}

	return queryType == RumorQuery
}

func (q *Queries) IsRumorState(id int) bool {
	q.Lock()
	defer q.Unlock()

	queryType, ok := q.state[id]
	if !ok {
		return false
	}

	return queryType == RumorStateQuery
}
