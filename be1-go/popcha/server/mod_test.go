package server

import (
	"bytes"
	"github.com/rs/xid"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/stretchr/testify/require"
	"github.com/zitadel/oidc/v2/pkg/oidc"
	"github.com/zitadel/oidc/v2/pkg/op"
	"io"
	"math"
	"math/rand"
	"net/http"
	"net/url"
	"popstellar"
	"popstellar/crypto"
	"popstellar/hub"
	"popstellar/hub/standard_hub"
	"reflect"
	"strings"
	"testing"
	"testing/quick"
	"time"
)

const (
	Alphabet      = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01233456789"
	MaxStringSize = 128
	MaxChecks     = 100000
)

func validScopes() []string {
	return []string{Profile}
}

func validResponseTypes() []string {
	return []string{ResTypeMulti, ResTypeId}
}

// TestAuthServerStartAndShutdown tests if the authorization server correctly starts and stops.
func TestAuthServerStartAndShutdown(t *testing.T) {
	l := popstellar.Logger

	h, err := standard_hub.NewHub(crypto.Suite.Point(), "", l, nil)
	require.NoError(t, err, "could not create hub")

	s, err := NewAuthServer(h, "/authorize", "localhost", 2003,
		"random_string", l)

	require.NoError(t, err, "could not create AuthServer")
	s.Start()
	<-s.Started

	err = s.Shutdown()
	require.NoError(t, err)
	<-s.Stopped
}

func TestAuthorizationServerHandleValidateRequest(t *testing.T) {
	l := popstellar.Logger
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()

	// let the server properly start
	time.Sleep(1 * time.Second)

	// send a valid mock authorization request
	status, err := sendValidAuthRequest()
	require.NoError(t, err)
	require.Equal(t, "200 OK", status)
	err = s.Shutdown()
	require.NoError(t, err)

}

func sendValidAuthRequest() (string, error) {
	base := "http://localhost:3003/authorize?"
	params := url.Values{}
	params.Add(Nonce, xid.New().String())
	params.Add(ClientID, xid.New().String())
	params.Add(Scope, "openid profile")
	params.Add(LaoID, xid.New().String())
	params.Add(RedirectURI, "localhost:3008")
	params.Add(ResponseType, "id_token")
	params.Add(State, xid.New().String())

	u := base + params.Encode()
	log.Info().Msg(u)
	res, err := http.Get(u)
	if err != nil {
		return "", err
	}

	return res.Status, nil
}

func TestAuthRequestProperties(t *testing.T) {
	l := popstellar.Logger
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()

	propertyConfig := quick.Config{
		MaxCount: MaxChecks,
		Values: func(values []reflect.Value, r *rand.Rand) {
			values[0] = reflect.ValueOf(createOIDCRequestFromParams(randRequestParams(r)))
		}}

	// let the server properly start
	time.Sleep(1 * time.Second)
	err = quick.Check(isReqValid(s), &propertyConfig)
	require.NoError(t, err)

	err = s.Shutdown()
	require.NoError(t, err)
}

func isReqValid(as *AuthorizationServer) func(authRequest *oidc.AuthRequest) bool {
	return func(authRequest *oidc.AuthRequest) bool {
		err := as.ValidateAuthRequest(authRequest)
		return err == nil
	}
}
func randRequestParams(r *rand.Rand) map[string]string {
	params := make(map[string]string)
	params[Nonce] = genString(r, r.Intn(MaxStringSize))
	params[ClientID] = genString(r, r.Intn(MaxStringSize))
	var scopesBuilder strings.Builder
	scopesBuilder.WriteString(OpenID)
	scopesBuilder.WriteString(" ")
	scopesBuilder.WriteString(strings.Join(selectScopes(r), " "))
	params[Scope] = scopesBuilder.String()
	params[LaoID] = genString(r, r.Intn(MaxStringSize))
	params[RedirectURI] = "localhost:3008"
	params[ResponseType] = selectResponseType(r)
	params[State] = genString(r, r.Intn(MaxStringSize))
	return params
}

func TestClientParamsProperties(t *testing.T) {
	l := zerolog.New(io.Discard)
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()

	propertyConfig := quick.Config{
		MaxCount: MaxChecks,
		Values: func(values []reflect.Value, r *rand.Rand) {
			values[0] = reflect.ValueOf(randomClientParams(r))
		}}

	// let the server properly start
	time.Sleep(1 * time.Second)
	err = quick.Check(validClientParams, &propertyConfig)
	require.NoError(t, err)

	err = s.Shutdown()
	require.NoError(t, err)
}

func randomClientParams(r *rand.Rand) clientParams {
	c := clientParams{
		clientID:      genString(r, r.Intn(MaxStringSize)),
		redirectURIs:  []string{"localhost:3500"},
		responseTypes: []oidc.ResponseType{oidc.ResponseType(selectResponseType(r))},
	}
	return c
}

// property checker method for clientParams
func validClientParams(c clientParams) bool {
	// generate a randomizer
	rd := rand.New(rand.NewSource(rand.Int63()))

	// boolean formula validating client parameters.
	// clientID must be present
	return c.GetID() != " " &&
		// at least one URI is present
		len(c.RedirectURIs()) != 0 &&
		// no postLogout URI
		c.PostLogoutRedirectURIs() == nil &&
		// native application, as we use HTTP / localhost
		c.ApplicationType() == op.ApplicationTypeNative &&
		// we use a custom authentication method
		c.AuthMethod() == oidc.AuthMethodNone &&
		// at least one response type is present
		len(c.ResponseTypes()) != 0 &&
		// we work with implicit flow
		len(c.GrantTypes()) == 1 &&
		c.GrantTypes()[0] == oidc.GrantTypeImplicit &&
		// no login URL, we work with QRCode
		c.LoginURL(genString(rd, rd.Intn(MaxStringSize))) == "" &&
		// access token is of type 'bearer'
		c.AccessTokenType() == op.AccessTokenTypeBearer &&
		// no developer mode by default
		!c.DevMode() &&
		// no other scope allowed
		!c.IsScopeAllowed(genString(rd, rd.Intn(MaxStringSize)))

}

func selectResponseType(r *rand.Rand) string {
	rTypes := validResponseTypes()
	i := r.Intn(len(rTypes))
	return rTypes[i]
}
func selectScopes(r *rand.Rand) []string {
	scopes := validScopes()
	start := r.Intn(len(scopes))
	end := int(math.Min(float64(len(scopes)), float64(start+r.Intn(len(scopes)))))
	return scopes[start:end]

}
func genString(r *rand.Rand, s int) string {
	if s == 0 {
		s += 1
	}
	var b bytes.Buffer
	for i := 0; i < s; i++ {
		rdmIdx := r.Intn(len(Alphabet))
		b.WriteString(string(Alphabet[rdmIdx]))
	}
	return b.String()
}

// -----------------------------------------------------------------------------
// Utility functions

type fakeHub struct {
	hub.Hub
}
