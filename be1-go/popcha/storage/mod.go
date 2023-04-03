package storage

import (
	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
	"sync"
)

/*
Storage defines the in-memory store of the authorization server. It is used for
nonce replay prevention, Pop-Token-To-Identifier uniqueness as well as for keeping
track of ongoing requests.
*/
type Storage struct {
	lock                 *sync.Mutex
	popTokenToIdentifier map[string]string //hash map enforcing uniqueness on (PopToken,identifier)
	nonces               map[string]struct{}
	log                  zerolog.Logger
}

// NewStorage initializes a Storage object given a logger
func NewStorage(log zerolog.Logger) (*Storage, error) {
	return &Storage{
		lock:                 &sync.Mutex{},
		popTokenToIdentifier: make(map[string]string, 0),
		nonces:               make(map[string]struct{}, 0),
		log:                  log.With().Str("role", "message server's storage").Logger(),
	}, nil
}

// AddNonce attempts to add a nonce string to the storage and mark it as used
func (s *Storage) AddNonce(nonce string) error {
	s.lock.Lock()
	defer s.lock.Unlock()
	_, ok := s.nonces[nonce]
	if ok {
		return xerrors.New("nonce already used")
	}
	s.nonces[nonce] = struct{}{}
	return nil
}

// GetNonce returns whether the given nonce has been marked or not
func (s *Storage) GetNonce(nonce string) bool {
	s.lock.Lock()
	defer s.lock.Unlock()
	_, ok := s.nonces[nonce]
	return ok
}

// SetIdentifier attempts to write an entry in the Token-Identifier table
func (s *Storage) SetIdentifier(popToken string, identifier string) error {
	s.lock.Lock()
	defer s.lock.Unlock()
	_, ok := s.popTokenToIdentifier[popToken]
	if ok {
		return xerrors.Errorf("an identifier for the given pop token already exists: %s", popToken)
	}
	s.popTokenToIdentifier[popToken] = identifier
	return nil
}

// GetIdentifier returns the identifier associated to the pop token, or nonce
// if no such entry has been found
func (s *Storage) GetIdentifier(popToken string) string {
	s.lock.Lock()
	defer s.lock.Unlock()
	id, ok := s.popTokenToIdentifier[popToken]
	if ok {
		return id
	}
	return ""
}
