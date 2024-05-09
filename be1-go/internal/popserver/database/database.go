package database

import (
	"golang.org/x/xerrors"
	"popstellar/message/answer"
	"sync"
	"testing"
)

var once sync.Once
var instance Repository

func InitDatabase(db Repository) bool {
	hasInit := false

	once.Do(func() {
		instance = db
		hasInit = true
	})

	return hasInit
}

func SetDatabase(t *testing.T) (*MockRepository, error) {
	if t == nil {
		return nil, xerrors.Errorf("only for tests")
	}

	mockRepository := NewMockRepository(t)

	instance = mockRepository

	return mockRepository, nil
}

func getInstance() (Repository, *answer.Error) {
	if instance == nil {
		errAnswer := answer.NewInternalServerError("database was not instantiated")
		return nil, errAnswer
	}

	return instance, nil
}

func GetQueryRepositoryInstance() (QueryRepository, *answer.Error) {
	return getInstance()
}

func GetChannelRepositoryInstance() (ChannelRepository, *answer.Error) {
	return getInstance()
}

func GetRootRepositoryInstance() (RootRepository, *answer.Error) {
	return getInstance()
}

func GetLAORepositoryInstance() (LAORepository, *answer.Error) {
	return getInstance()
}

func GetChirpRepositoryInstance() (ChirpRepository, *answer.Error) {
	return getInstance()
}

func GetCoinRepositoryInstance() (CoinRepository, *answer.Error) {
	return getInstance()
}

func GetElectionRepositoryInstance() (ElectionRepository, *answer.Error) {
	return getInstance()
}

func GetReactionRepositoryInstance() (ReactionRepository, *answer.Error) {
	return getInstance()
}
