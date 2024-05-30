package state

import (
	"popstellar/message/answer"
	"popstellar/message/query/method"
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

func getQueries() (Querier, *answer.Error) {
	if instance == nil || instance.queries == nil {
		return nil, answer.NewInternalServerError("querier was not instantiated")
	}

	return instance.queries, nil
}

func GetNextID() (int, *answer.Error) {
	queries, errAnswer := getQueries()
	if errAnswer != nil {
		return -1, errAnswer
	}

	return queries.GetNextID(), nil
}

func SetQueryReceived(ID int) *answer.Error {
	queries, errAnswer := getQueries()
	if errAnswer != nil {
		return errAnswer
	}

	err := queries.SetQueryReceived(ID)
	if err != nil {
		errAnswer := answer.NewInvalidActionError("%v", err)
		return errAnswer
	}

	return nil
}

func AddQuery(ID int, query method.GetMessagesById) *answer.Error {
	queries, errAnswer := getQueries()
	if errAnswer != nil {
		return errAnswer
	}

	queries.AddQuery(ID, query)

	return nil
}

func AddRumorQuery(ID int, query method.Rumor) *answer.Error {
	queries, errAnswer := getQueries()
	if errAnswer != nil {
		return errAnswer
	}

	queries.AddRumorQuery(ID, query)

	return nil
}

func IsRumorQuery(ID int) (bool, *answer.Error) {
	queries, errAnswer := getQueries()
	if errAnswer != nil {
		return false, errAnswer
	}

	return queries.IsRumorQuery(ID), nil
}

func GetRumorFromPastQuery(ID int) (method.Rumor, bool, *answer.Error) {
	queries, errAnswer := getQueries()
	if errAnswer != nil {
		return method.Rumor{}, false, errAnswer
	}

	rumor, ok := queries.GetRumorFromPastQuery(ID)

	return rumor, ok, nil
}
