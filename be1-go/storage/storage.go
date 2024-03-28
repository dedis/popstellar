package storage

import "popstellar/repository"

// Storage is an interface that combines the HubRepository and ElectionRepository interfaces.
type Storage interface {
	repository.HubRepository
	repository.ElectionRepository
	repository.RootRepository
}
