package database

import (
	"golang.org/x/xerrors"
	"popstellar/internal/popserver/database/repository"
	"popstellar/message/answer"
	"sync"
	"testing"
)

var once sync.Once
var instance repository.Repository

func InitDatabase(db repository.Repository) bool {
	hasInit := false

	once.Do(func() {
		instance = db
		hasInit = true
	})

	return hasInit
}

func SetDatabase(t *testing.T) (*repository.MockRepository, error) {
	if t == nil {
		return nil, xerrors.Errorf("only for tests")
	}

	mockRepository := repository.NewMockRepository(t)

	instance = mockRepository

	return mockRepository, nil
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
