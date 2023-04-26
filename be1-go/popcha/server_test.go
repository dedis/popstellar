package popcha

import (
	"bytes"
	"github.com/gorilla/websocket"
	"github.com/rs/xid"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/rzajac/zltest"
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

	//message content for the websocket workflow test
	wsData = "Hello receiver!"
)

// genString is a helper method generating a string in the alphanumerical alphabet
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
	<-s.Started
	// let the server properly start
	time.Sleep(1 * time.Second)

	// send a valid mock authorization request
	res, err := sendValidAuthRequest()
	require.NoError(t, err)

	time.Sleep(time.Second)

	require.Equal(t, 200, res.StatusCode)
	err = s.Shutdown()
	<-s.Stopped
	require.NoError(t, err)

}

// helper method generating a valid authorization request with some pre-determined parameters.
func sendValidAuthRequest() (*http.Response, error) {
	qrURL := createAuthRequestURL("random_nonce", "v4l1d_client_id",
		strings.Join([]string{OpenID, Profile}, " "), "v4l1d_lao_id", "http://localhost:3008/",
		ResTypeMulti, "st4te")
	res, err := http.Get(qrURL)
	log.Info().Msg(qrURL)
	if err != nil {
		return res, err
	}
	return res, nil
}

// helper method creating a valid authorization request URL
func createAuthRequestURL(nonce string, clientID string, scope string, loginHint string,
	redirectURI string, responseType string, state string) string {
	params := url.Values{}
	params.Add(Nonce, nonce)
	params.Add(ClientID, clientID)
	params.Add(Scope, scope)
	params.Add(LoginHint, loginHint)
	params.Add(RedirectURI, redirectURI)
	params.Add(ResponseType, responseType)
	params.Add(State, state)

	qrURL := BaseURL + params.Encode()
	return qrURL
}

// TestAuthRequestFails tries different scenarios in which the authorization request should not be validated
func TestAuthRequestFails(t *testing.T) {
	logTester := zltest.New(t)

	l := zerolog.New(logTester).With().Timestamp().Logger()
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()
	<-s.Started
	// let the server properly start
	time.Sleep(200 * time.Millisecond)

	// init parameters map
	params := url.Values{}

	// first case, url without parameters
	emptyURL := BaseURL

	res, err := http.Get(emptyURL)
	log.Info().Msg(emptyURL)
	require.NoError(t, err)

	lastEntry := logTester.LastEntry()
	lastEntry.ExpLevel(zerolog.ErrorLevel)
	lastEntry.ExpMsg("Error while verifying the parameters of the request")
	bodyBytes := helperValidateGetAndParseBody(t, res)

	// we require that in the error response, the number of missing arguments is equal to 6
	helperMissingArgs(t, bodyBytes, 6)

	time.Sleep(200 * time.Millisecond)

	// add a nonce
	params.Add(Nonce, "some_n0nc3")

	partialURL := emptyURL + params.Encode()

	res, err = http.Get(partialURL)
	log.Info().Msg(partialURL)
	require.NoError(t, err)

	lastEntry = logTester.LastEntry()
	lastEntry.ExpLevel(zerolog.ErrorLevel)
	lastEntry.ExpMsg("Error while verifying the parameters of the request")
	bodyBytes = helperValidateGetAndParseBody(t, res)
	// we require that there are 5 missing arguments
	helperMissingArgs(t, bodyBytes, 5)

	time.Sleep(200 * time.Millisecond)

	// testing request with valid number of parameters, but invalid scope

	invalidScopeURL := createAuthRequestURL("n", "c", "invalid", "l", "localhost:3001", ResTypeMulti, " ")
	_, err = http.Get(invalidScopeURL)

	// no error from the get request
	require.NoError(t, err)

	lastEntry = logTester.LastEntry()
	lastEntry.ExpLevel(zerolog.ErrorLevel)
	lastEntry.ExpMsg("Error while validating the auth request")

	time.Sleep(200 * time.Millisecond)

	// testing request with wrong response type
	invalidResTypeURL := createAuthRequestURL("n", "c", OpenID, "l", "localhost:3001", "invalid", " ")

	_, err = http.Get(invalidResTypeURL)

	// no error from the get request
	require.NoError(t, err)

	lastEntry = logTester.LastEntry()
	lastEntry.ExpLevel(zerolog.ErrorLevel)
	lastEntry.ExpErr(oidc.ErrInvalidRequest().WithDescription(errValidAuthFormat, errInvalidResponseType))
	err = s.Shutdown()
	require.NoError(t, err)
	<-s.Stopped

}

// check the response has code 400, and parse the response body
func helperValidateGetAndParseBody(t *testing.T, res *http.Response) []byte {
	require.Equal(t, res.StatusCode, badRequestCode)
	bodyBytes, err := io.ReadAll(res.Body)
	require.NoError(t, err)
	err = res.Body.Close()
	require.NoError(t, err)
	return bodyBytes
}

// verifies that the number of missing arguments in the body is equal to n
func helperMissingArgs(t *testing.T, body []byte, n int) {
	missingArgs := strings.Split(strings.SplitN(string(body), "arguments ", 2)[1], " ")
	require.Equal(t, len(missingArgs), n)
}

// TestAuthorizationServerWebsocket starts the AS, and tests the websocket connection between the webpage and the
// server. It also tests that no error is thrown when a client sends a message to the webpage websocket through the
// server.
func TestAuthorizationServerWebsocket(t *testing.T) {

	// starting the authorization server
	l := popstellar.Logger
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()
	<-s.Started
	// let the server properly start
	time.Sleep(1 * time.Second)

	//parameters definition for the unit test
	clientID := "client"
	laoID := "lao"
	nonce := "nonce"
	state := "state"
	redirectURI := "https://example.com/"
	scope := strings.Join([]string{OpenID, Profile}, " ")
	resType := ResTypeMulti

	// create the URL of the PopCHA webpage
	u := createAuthRequestURL(nonce, clientID, scope, laoID, redirectURI, resType, state)

	_, err = http.Get(u)
	require.NoError(t, err)
	l.Info().Msg(u)

	time.Sleep(2 * time.Second)

	// constructing the unique URL endpoint of the PopCHA Websocket server.
	popChaPath := strings.Join([]string{responseEndpoint, laoID, "authentication", clientID, nonce}, "/")

	//  ws://popcha.example/response/lao/authentication/client/nonce
	popChaWsURL := url.URL{Scheme: "ws", Host: "localhost:3003", Path: popChaPath}

	// instantiating websocket connection
	client, err := newWSClient(popChaWsURL)
	require.NoError(t, err)
	time.Sleep(time.Second)

	//creating fake redirect URI parameters, for example just with the clientID. We are not
	// testing the validity of the parameters here, but rather that the websocket protocol doesn't
	// throw errors.
	fakeParams := url.Values{}
	fakeParams.Add("client_id", clientID)
	err = client.conn.WriteMessage(websocket.TextMessage, []byte(fakeParams.Encode()))
	require.NoError(t, err)

	time.Sleep(time.Second)

	err = s.Shutdown()
	require.NoError(t, err)
	<-s.Stopped
}

// TestAuthorizationServerWorkflow tests the control flow of the websocket communication.
// It tries to connect on /response endpoint without any additional path, and then
// tests the protocol with well-behaved clients using a valid path.
func TestAuthorizationServerWorkflow(t *testing.T) {
	logTester := zltest.New(t)

	l := zerolog.New(logTester).With().Timestamp().Logger()
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()
	<-s.Started
	// let the server properly start
	time.Sleep(200 * time.Millisecond)

	// test error on /response with empty path suffix
	emptyPathURL := url.URL{Scheme: "ws", Host: "localhost:3003", Path: responseEndpoint}
	emptyPathClient, err := newWSClient(emptyPathURL)
	require.NoError(t, err)

	// send any message to the websocket server
	err = emptyPathClient.conn.WriteMessage(websocket.TextMessage, []byte("test"))
	logTester.LastEntry().ExpMsg("Error while receiving a request on /response: empty path")
	require.NoError(t, err)

	time.Sleep(200 * time.Millisecond)

	// create two clients, a sender and a receiver, on a valid path
	validPath := strings.Join([]string{responseEndpoint, "laoid", "authentication", "clientid", "nonce"}, "/")

	validURL := url.URL{Scheme: "ws", Host: "localhost:3003", Path: validPath}

	// creating the clients

	// the receiver instantiates the connection first. The server will keep this connection in memory
	clientReceiver, err := newWSClient(validURL)
	require.NoError(t, err)

	// the sender's connection is not saved, but the server will wait for a message from it.
	clientSender, err := newWSClient(validURL)
	require.NoError(t, err)

	// checking the two clients are different
	require.NotEqual(t, clientSender.id, clientReceiver.id)

	received := make(chan int)
	go func() {
		mt, message, err := clientReceiver.conn.ReadMessage()
		require.NoError(t, err)
		require.Equal(t, websocket.TextMessage, mt)
		require.Equal(t, wsData, string(message))
		l.Info().Msg("Received the correct message from the sender client.")
		received <- 1
	}()

	err = clientSender.conn.WriteMessage(websocket.TextMessage, []byte(wsData))
	require.NoError(t, err)

	<-received

	logTester.LastEntry().ExpLevel(zerolog.InfoLevel)

	time.Sleep(time.Second)

	require.NoError(t, clientReceiver.conn.Close())
	require.NoError(t, clientSender.conn.Close())
	err = s.Shutdown()
	require.NoError(t, err)
	<-s.Stopped

}

// TestGenerateQrCodeOnEdgeCases tests long inputs, or special characters on the generateQrCode method
func TestGenerateQrCodeOnEdgeCases(t *testing.T) {
	// create authorization server
	l := zerolog.New(io.Discard)
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")

	// testing that the QRCode can't be generated if the data is too long
	longURL := &url.URL{
		Scheme: "http",
		Host:   "example.com",
		Path:   "client_id=" + strings.Repeat("c", 3000),
	}

	var req = &http.Request{
		Method: http.MethodGet,
		URL:    longURL,
	}

	err = s.generateQRCode(&fakeResponseWriter{}, req, "l", "c", "n", "")
	require.Error(t, err)

	// testing that the QRCode can be generated with special characters, due to URL encoding
	specialCharURL := &url.URL{
		Scheme: "http",
		Host:   "example.com",
		Path:   "client_id=" + strings.Repeat("ち💆🏻‍ҨऔȢЖ", 3),
	}

	req.URL = specialCharURL
	err = s.generateQRCode(&fakeResponseWriter{}, req, "l", "c", "n", "")
	require.NoError(t, err)
}

// TestClientParams tests the validity of the client parameters
func TestClientParams(t *testing.T) {
	l := zerolog.New(io.Discard)
	s, err := NewAuthServer(fakeHub{}, "authorize", "localhost",
		3003, "random_string", l)
	require.NoError(t, err, "could not create AuthServer")
	s.Start()
	<-s.Started
	// let the server properly start
	time.Sleep(1 * time.Second)

	// create a property generating random, valid client parameters (valid because only
	// the ClientID is random, and only uses alphanumerical alphabet).
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
	<-s.Stopped
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
	// ClientID must be present
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
		c.ResponseTypes()[0] == ResTypeMulti &&
		// clock skew is set at 0
		c.ClockSkew() == 0 &&
		// ID Token lifetime duration is valid
		c.IDTokenLifetime() > 0

}

// -----------------------------------------------------------------------------
// Utility functions

type fakeHub struct {
	hub.Hub
}

type fakeWSClient struct {
	id   string
	conn *websocket.Conn
}

func newWSClient(urlPath url.URL) (*fakeWSClient, error) {
	conn, _, err := websocket.DefaultDialer.Dial(urlPath.String(), nil)
	if err != nil {
		return nil, err
	}
	return &fakeWSClient{
		id:   xid.New().String(),
		conn: conn,
	}, nil

}

// implements http.ResponseWriter
type fakeResponseWriter struct{}

/*
Set of methods of http.ResponseWriter interface. In that case, it does nothing.
*/

func (f *fakeResponseWriter) Header() http.Header {
	return http.Header{}
}

func (f *fakeResponseWriter) Write(_ []byte) (int, error) {
	return 0, nil
}

func (f *fakeResponseWriter) WriteHeader(_ int) {}
