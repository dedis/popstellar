package database

import (
	"sync"

	"popstellar/internal/errors"
	"popstellar/internal/mock"
	"popstellar/internal/repository"
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
func SetDatabase(mockRepo *mock.Repository) {
	instance = mockRepo
}

func getInstance() (repository.Repository, error) {
	if instance == nil {
		return nil, errors.NewInternalServerError("database was not instantiated")
	}

	return instance, nil
}

func GetRumorSenderRepositoryInstance() (repository.RumorSenderRepository, error) {
	return getInstance()
}

func GetQueryRepositoryInstance() (repository.QueryRepository, error) {
	return getInstance()
}

func GetChannelRepositoryInstance() (repository.ChannelRepository, error) {
	return getInstance()
}

func GetRootRepositoryInstance() (repository.RootRepository, error) {
	return getInstance()
}

func GetLAORepositoryInstance() (repository.LAORepository, error) {
	return getInstance()
}

func GetChirpRepositoryInstance() (repository.ChirpRepository, error) {
	return getInstance()
}

func GetCoinRepositoryInstance() (repository.CoinRepository, error) {
	return getInstance()
}

func GetElectionRepositoryInstance() (repository.ElectionRepository, error) {
	return getInstance()
}

func GetReactionRepositoryInstance() (repository.ReactionRepository, error) {
	return getInstance()
}

func GetFederationRepositoryInstance() (repository.FederationRepository, error) {
	return getInstance()
}
