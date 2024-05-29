package database

import (
	"popstellar/internal/popserver/database/repository"
	"popstellar/message/answer"
	"sync"
)

var once sync.Once
var instance repository.Repository

func InitDatabase(db repository.Repository) {
	once.Do(func() {
		instance = db
	})
}

// ONLY FOR TEST PURPOSE
// SetDatabase is only here to be used to reset the database before each test
func SetDatabase(mockRepo *repository.MockRepository) {
	instance = mockRepo
}

func getInstance() (repository.Repository, *answer.Error) {
	if instance == nil {
		errAnswer := answer.NewInternalServerError("database was not instantiated")
		return nil, errAnswer
	}

	return instance, nil
}

func GetQueryRepositoryInstance() (repository.QueryRepository, *answer.Error) {
	return getInstance()
}

func GetChannelRepositoryInstance() (repository.ChannelRepository, *answer.Error) {
	return getInstance()
}

func GetRootRepositoryInstance() (repository.RootRepository, *answer.Error) {
	return getInstance()
}

func GetLAORepositoryInstance() (repository.LAORepository, *answer.Error) {
	return getInstance()
}

func GetChirpRepositoryInstance() (repository.ChirpRepository, *answer.Error) {
	return getInstance()
}

func GetCoinRepositoryInstance() (repository.CoinRepository, *answer.Error) {
	return getInstance()
}

func GetElectionRepositoryInstance() (repository.ElectionRepository, *answer.Error) {
	return getInstance()
}

func GetReactionRepositoryInstance() (repository.ReactionRepository, *answer.Error) {
	return getInstance()
}

func GetFederationRepositoryInstance() (repository.FederationRepository, *answer.Error) {
	return getInstance()
}
