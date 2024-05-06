package database

import (
	"golang.org/x/xerrors"
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

func GetQueryRepositoryInstance() (QueryRepository, bool) {
	if instance == nil {
		return nil, false
	}

	return instance, true
}

func GetChannelRepositoryInstance() (ChannelRepository, bool) {
	if instance == nil {
		return nil, false
	}

	return instance, true
}

func GetRootRepositoryInstance() (RootRepository, bool) {
	if instance == nil {
		return nil, false
	}

	return instance, true
}

func GetLAORepositoryInstance() (LAORepository, bool) {
	if instance == nil {
		return nil, false
	}

	return instance, true
}

func GetChirpRepositoryInstance() (ChirpRepository, bool) {
	if instance == nil {
		return nil, false
	}

	return instance, true
}

func GetCoinRepositoryInstance() (CoinRepository, bool) {
	if instance == nil {
		return nil, false
	}

	return instance, true
}

func GetElectionRepositoryInstance() (ElectionRepository, bool) {
	if instance == nil {
		return nil, false
	}

	return instance, true
}

func GetReactionRepositoryInstance() (ReactionRepository, bool) {
	if instance == nil {
		return nil, false
	}

	return instance, true
}
