package storage

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"github.com/rs/xid"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/zitadel/oidc/v2/pkg/oidc"
	"github.com/zitadel/oidc/v2/pkg/op"
	"golang.org/x/xerrors"
	"gopkg.in/square/go-jose.v2"
	"math/big"
	"popstellar/popcha/server"
	"sync"
	"time"
)

const (
	implicitFlow = "PoPCHA exclusively uses an Implicit Flow"
)

type signingKey struct {
	id        string
	algorithm jose.SignatureAlgorithm
	key       *rsa.PrivateKey
}

func (s signingKey) SignatureAlgorithm() jose.SignatureAlgorithm {
	return s.algorithm
}

func (s signingKey) Key() interface{} {
	return s.key
}

func (s signingKey) ID() string {
	return s.ID()
}

type Storage struct {
	lock              *sync.Mutex
	POPsToIdentifiers map[string]big.Int // hash map 'POP Token -> long term identifier'
	authReqs          map[string]*server.AuthRequest
	signingKey        signingKey
	log               zerolog.Logger
}

func (s *Storage) CreateAuthRequest(ctx context.Context, request *oidc.AuthRequest, s2 string) (op.AuthRequest, error) {
	s.lock.Lock()
	defer s.lock.Unlock()

	panic("implement me")
}

func (s *Storage) AuthRequestByID(_ context.Context, id string) (op.AuthRequest, error) {
	s.lock.Lock()
	defer s.lock.Unlock()
	r, ok := s.authReqs[id]
	if !ok {
		return nil, xerrors.Errorf("The given ID is not present in the auth requests database")
	}
	return r, nil
}

func (s *Storage) AuthRequestByCode(_ context.Context, _ string) (op.AuthRequest, error) {
	return nil, xerrors.Errorf("Can't get the auth request by code: %s", implicitFlow)
}

func (s *Storage) SaveAuthCode(_ context.Context, _string, _ string) error {
	return xerrors.Errorf("Can't save the auth code : %s", implicitFlow)
}

func (s *Storage) DeleteAuthRequest(ctx context.Context, id string) error {
	s.lock.Lock()
	defer s.lock.Unlock()
	delete(s.authReqs, id)
	return nil
}

func (s *Storage) CreateAccessToken(ctx context.Context, request op.TokenRequest) (accessTokenID string,
	expiration time.Time, err error) {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) CreateAccessAndRefreshTokens(_ context.Context, _ op.TokenRequest, _ string) (accessTokenID string,
	newRefreshTokenID string, expiration time.Time, err error) {
	return "", "", time.Time{},
		xerrors.Errorf("Error while trying to create Refresh Tokens: %s", implicitFlow)
}

func (s *Storage) TokenRequestByRefreshToken(_ context.Context, _ string) (op.RefreshTokenRequest, error) {
	return nil,
		xerrors.Errorf("Error while trying to create a token request from a refrwsh token: %s", implicitFlow)
}

func (s *Storage) TerminateSession(ctx context.Context, userID string, clientID string) error {
	//TODO not sure whether it is useful
	panic("implement me")
}

func (s *Storage) RevokeToken(ctx context.Context, tokenOrTokenID string, userID string, clientID string) *oidc.Error {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) GetRefreshTokenInfo(_ context.Context, _ string, _ string) (userID string,
	tokenID string, err error) {
	return "", "",
		xerrors.Errorf("Error while trying to get refresh token info: %s", implicitFlow)
}

func (s *Storage) SigningKey(ctx context.Context) (op.SigningKey, error) {
	return &s.signingKey, nil
}

func (s *Storage) SignatureAlgorithms(ctx context.Context) ([]jose.SignatureAlgorithm, error) {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) KeySet(ctx context.Context) ([]op.Key, error) {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) GetClientByClientID(ctx context.Context, clientID string) (op.Client, error) {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) AuthorizeClientIDSecret(ctx context.Context, clientID, clientSecret string) error {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) SetUserinfoFromScopes(ctx context.Context, userinfo *oidc.UserInfo, userID, clientID string, scopes []string) error {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) SetUserinfoFromToken(ctx context.Context, userinfo *oidc.UserInfo, tokenID, subject, origin string) error {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) SetIntrospectionFromToken(ctx context.Context, userinfo *oidc.IntrospectionResponse, tokenID,
	subject, clientID string) error {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) GetPrivateClaimsFromScopes(ctx context.Context, userID, clientID string,
	scopes []string) (map[string]interface{}, error) {
	//TODO implement me
	panic("implement me")
}

func (s *Storage) GetKeyByIDAndClientID(_ context.Context, _, _ string) (*jose.JSONWebKey, error) {
	return nil, xerrors.Errorf("Error while tryin to get key by ID: %s", implicitFlow)
}

func (s *Storage) ValidateJWTProfileScopes(_ context.Context, _ string, _ []string) ([]string, error) {
	return []string{}, xerrors.Errorf("Error while trying to Validate JWT Profile: %s", implicitFlow)
}

func (s *Storage) Health(_ context.Context) error {
	return nil
}

func (s *Storage) AddTokenToID(token string, id big.Int) error {
	s.lock.Lock()
	defer s.lock.Unlock()
	_, ok := s.POPsToIdentifiers[token]
	if ok {
		return xerrors.Errorf("Error while trying to add a token to ID in storage: token already present")
	}
	s.POPsToIdentifiers[token] = id
	return nil
}

func NewStorage() (*Storage, error) {
	key, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		return nil, err
	}
	return &Storage{
		lock:              &sync.Mutex{},
		POPsToIdentifiers: make(map[string]big.Int),
		authReqs:          make(map[string]*server.AuthRequest),
		signingKey: signingKey{
			id:        xid.New().String(),
			algorithm: jose.RS256,
			key:       key,
		},
		log: log.With().Str("role", "auth server's storage").Logger(),
	}, nil
}
