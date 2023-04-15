package popcha

import (
	"bytes"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/stretchr/testify/require"
	"github.com/zitadel/oidc/v2/pkg/oidc"
	"github.com/zitadel/oidc/v2/pkg/op"
	"io"
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
	MaxStringSize = 128
	MaxChecks     = 100000
	ValidAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
	BaseURL       = "http://localhost:3003/authorize?"
)

// genString is a helper method generating a string in the alpha-numerical alphabet
func genString(r *rand.Rand, s int) string {
	if s == 0 {
		s += 1
	}
	var b bytes.Buffer
	for i := 0; i < s; i++ {
		rdmIdx := r.Intn(len(ValidAlphabet))
		b.WriteString(string(ValidAlphabet[rdmIdx]))
	}
	return b.String()
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

// TestAuthorizationServerHandleValidateRequest tests if , when receiving a valid authorization request,
// the server serves a webpage with the associated QRCode.
func TestAuthorizationServerHandleValidateRequest(t *testing.T) {
	l := popstellar.Logger
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()

	// let the server properly start
	time.Sleep(1 * time.Second)

	// send a valid mock authorization request
	res, err := sendValidAuthRequest()
	require.NoError(t, err)
	require.Equal(t, 200, res.StatusCode)
	err = s.Shutdown()
	require.NoError(t, err)

}

// helper method generating a valid authorization request with some pre-determined parameters.
func sendValidAuthRequest() (*http.Response, error) {
	qrURL := createAuthRequestURL()
	res, err := http.Get(qrURL)
	log.Info().Msg(qrURL)
	if err != nil {
		return res, err
	}
	return res, nil
}

// helper method creating a valid authorization request URL
func createAuthRequestURL() string {
	params := url.Values{}
	params.Add(Nonce, "random_nonce")
	params.Add(ClientID, "v4l1d_client_id")
	params.Add(Scope, strings.Join([]string{OpenID, Profile}, " "))
	params.Add(LoginHint, "v4l1d_lao_id")
	params.Add(RedirectURI, "localhost:3008")
	params.Add(ResponseType, ResTypeMulti)
	params.Add(State, "st4te")

	qrURL := BaseURL + params.Encode()
	return qrURL
}

// TestAuthRequestFails tries different scenarios in which the authorization request should not be validated
func TestAuthRequestFails(t *testing.T) {
	l := popstellar.Logger
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()
	// let the server properly start
	time.Sleep(1 * time.Second)

	// init parameters map
	params := url.Values{}

	// first case, url without parameters
	emptyURL := BaseURL

	res, err := http.Get(emptyURL)
	log.Info().Msg(emptyURL)
	require.NoError(t, err)

	bodyBytes := helperValidateGetAndParseBody(t, res)

	// we require that in the error response, the number of missing arguments is equal to 6
	helperMissingArgs(t, bodyBytes, 6)

	time.Sleep(time.Second)

	// add a nonce
	params.Add(Nonce, "some_n0nc3")

	partialURL := emptyURL + params.Encode()

	res, err = http.Get(partialURL)
	log.Info().Msg(partialURL)
	require.NoError(t, err)

	bodyBytes = helperValidateGetAndParseBody(t, res)

	// we require that there are 5 missing arguments
	helperMissingArgs(t, bodyBytes, 5)

	err = s.Shutdown()
	require.NoError(t, err)

}

// check the response has code 400, and parse the response body
func helperValidateGetAndParseBody(t *testing.T, res *http.Response) []byte {
	require.Equal(t, res.StatusCode, badRequestCode)
	bodyBytes, err := io.ReadAll(res.Body)
	require.NoError(t, err)
	return bodyBytes
}

// verifies that the number of missing arguments in the body is equal to n
func helperMissingArgs(t *testing.T, body []byte, n int) {
	missingArgs := strings.Split(strings.SplitN(string(body), "arguments ", 2)[1], " ")
	require.Equal(t, len(missingArgs), n)
}

// TestClientParams tests the validity of the client parameters
func TestClientParams(t *testing.T) {
	l := zerolog.New(io.Discard)
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()

	// let the server properly start
	time.Sleep(1 * time.Second)

	// create a property generating random, valid client parameters (valid because only
	// the clientID is random, and only uses alphanumerical alphabet).
	propertyConfig := quick.Config{
		MaxCount: MaxChecks,
		Values: func(values []reflect.Value, r *rand.Rand) {
			values[0] = reflect.ValueOf(randomClientParams(r))
		}}

	// validate the client parameters for many values
	err = quick.Check(validClientParams, &propertyConfig)
	require.NoError(t, err)

	// test client parameters without ID
	require.False(t, validClientParams(noIDClientParam()))

	// test client parameters with invalid response type
	require.False(t, validClientParams(invalidResponseTypeClientParam()))

	err = s.Shutdown()
	require.NoError(t, err)
}

// generates a valid client for PopCHA
func randomClientParams(r *rand.Rand) clientParams {
	c := clientParams{
		clientID:     genString(r, r.Intn(MaxStringSize)),
		redirectURIs: []string{"localhost:3500"},
		resType:      ResTypeMulti,
	}
	return c
}

func noIDClientParam() clientParams {
	c := clientParams{
		clientID:     "",
		redirectURIs: []string{"localhost:3500"},
		resType:      ResTypeMulti,
	}
	return c
}

func invalidResponseTypeClientParam() clientParams {
	c := clientParams{
		clientID:     "s4m3cl1entID",
		redirectURIs: []string{"localhost:3500"},
		resType:      "inval1dR3sType",
	}
	return c
}

// property checker method for clientParams
func validClientParams(c clientParams) bool {
	// boolean formula validating client parameters.
	// clientID must be present
	return c.GetID() != "" &&
		// at least one URI is present
		len(c.RedirectURIs()) != 0 &&
		// native application, as we use HTTP / localhost
		c.ApplicationType() == op.ApplicationTypeNative &&
		// we use a custom authentication method
		c.AuthMethod() == oidc.AuthMethodNone &&
		// one response type is present
		len(c.ResponseTypes()) == 1 &&
		// we work with implicit flow
		len(c.GrantTypes()) == 1 &&
		c.GrantTypes()[0] == oidc.GrantTypeImplicit &&
		// access token is of type 'bearer'
		c.AccessTokenType() == op.AccessTokenTypeBearer &&
		// no developer mode by default
		!c.DevMode() &&
		// responseType is correct
		c.ResponseTypes()[0] == ResTypeMulti

}

// -----------------------------------------------------------------------------
// Utility functions

type fakeHub struct {
	hub.Hub
}
