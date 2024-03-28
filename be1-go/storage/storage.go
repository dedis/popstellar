package storage

import (
	"popstellar/hub"
)

// Storage is an interface that combines the Repository and ElectionRepository interfaces.
type Storage interface {
	hub.Repository
	hub.ElectionRepository
	hub.RootRepository
}
