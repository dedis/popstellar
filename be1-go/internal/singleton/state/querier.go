package state

import (
	"popstellar/internal/errors"
	"popstellar/internal/message/query/method"
)

type Querier interface {
	GetQueryState(ID int) (bool, error)
	GetNextID() int
	SetQueryReceived(ID int) error
	AddQuery(ID int, query method.GetMessagesById)
	AddRumorQuery(id int, query method.Rumor)
	IsRumorQuery(queryID int) bool
	GetRumorFromPastQuery(queryID int) (method.Rumor, bool)
}

func getQueries() (Querier, error) {
	if instance == nil || instance.queries == nil {
		return nil, errors.NewInternalServerError("querier was not instantiated")
	}

	return instance.queries, nil
}

func GetNextID() (int, error) {
	queries, err := getQueries()
	if err != nil {
		return -1, err
	}

	return queries.GetNextID(), nil
}

func SetQueryReceived(ID int) error {
	queries, err := getQueries()
	if err != nil {
		return err
	}

	err = queries.SetQueryReceived(ID)
	if err != nil {
		return err
	}

	return nil
}

func AddQuery(ID int, query method.GetMessagesById) error {
	queries, err := getQueries()
	if err != nil {
		return err
	}

	queries.AddQuery(ID, query)

	return nil
}

func AddRumorQuery(ID int, query method.Rumor) error {
	queries, err := getQueries()
	if err != nil {
		return err
	}

	queries.AddRumorQuery(ID, query)

	return nil
}

func IsRumorQuery(ID int) (bool, error) {
	queries, err := getQueries()
	if err != nil {
		return false, err
	}

	return queries.IsRumorQuery(ID), nil
}

func GetRumorFromPastQuery(ID int) (method.Rumor, bool, error) {
	queries, err := getQueries()
	if err != nil {
		return method.Rumor{}, false, err
	}

	rumor, ok := queries.GetRumorFromPastQuery(ID)

	return rumor, ok, nil
}
