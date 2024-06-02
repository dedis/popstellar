package database

import (
	"popstellar/internal/message/answer"
	"popstellar/internal/mock"
	repository2 "popstellar/internal/repository"
	"sync"
)

var once sync.Once
var instance repository2.Repository

func InitDatabase(db repository2.Repository) {
	once.Do(func() {
		instance = db
	})
}

// ONLY FOR TEST PURPOSE
// SetDatabase is only here to be used to reset the database before each test
func SetDatabase(mockRepo *mock.Repository) {
	instance = mockRepo
}

func getInstance() (repository2.Repository, *answer.Error) {
	if instance == nil {
		errAnswer := answer.NewInternalServerError("database was not instantiated")
		return nil, errAnswer
	}

	return instance, nil
}

func GetRumorSenderRepositoryInstance() (repository2.RumorSenderRepository, *answer.Error) {
	return getInstance()
}

func GetQueryRepositoryInstance() (repository2.QueryRepository, *answer.Error) {
	return getInstance()
}

func GetChannelRepositoryInstance() (repository2.ChannelRepository, *answer.Error) {
	return getInstance()
}

func GetRootRepositoryInstance() (repository2.RootRepository, *answer.Error) {
	return getInstance()
}

func GetLAORepositoryInstance() (repository2.LAORepository, *answer.Error) {
	return getInstance()
}

func GetChirpRepositoryInstance() (repository2.ChirpRepository, *answer.Error) {
	return getInstance()
}

func GetCoinRepositoryInstance() (repository2.CoinRepository, *answer.Error) {
	return getInstance()
}

func GetElectionRepositoryInstance() (repository2.ElectionRepository, *answer.Error) {
	return getInstance()
}

func GetReactionRepositoryInstance() (repository2.ReactionRepository, *answer.Error) {
	return getInstance()
}

func GetFederationRepositoryInstance() (repository2.FederationRepository, *answer.Error) {
	return getInstance()
}
