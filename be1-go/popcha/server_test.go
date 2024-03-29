package popcha

import (
	"bufio"
	"bytes"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"net/url"
	"os"
	"popstellar"
	"popstellar/crypto"
	"popstellar/hub"
	"popstellar/hub/standard_hub"
	"reflect"
	"strings"
	"testing"
	"testing/quick"

	"github.com/gorilla/websocket"
	"github.com/rs/xid"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/rzajac/zltest"
	"github.com/stretchr/testify/require"
	"github.com/zitadel/oidc/v2/pkg/oidc"
	"github.com/zitadel/oidc/v2/pkg/op"
)

const (
	MaxStringSize = 128
	MaxChecks     = 100000
	ValidAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
	BaseURL       = "http://localhost:%d/authorize?"

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

	h, err := standard_hub.NewHub(crypto.Suite.Point(), "", "", l, nil)
	require.NoError(t, err, "could not create hub")

	s, err := NewAuthServer(h, "localhost", 2003, l)
	require.NoError(t, err)

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
	s, err := NewAuthServer(fakeHub{}, "localhost", 3003, l)
	require.NoError(t, err)
	s.Start()
	<-s.Started

	// send a valid mock authorization request
	res, err := sendValidAuthRequest()
	require.NoError(t, err)

	require.Equal(t, 200, res.StatusCode)
	err = s.Shutdown()
	<-s.Stopped
	require.NoError(t, err)

}

// helper method generating a valid authorization request with some pre-determined parameters.
func sendValidAuthRequest() (*http.Response, error) {
	qrURL := createAuthRequestURL("random_nonce", "v4l1d_client_id",
		strings.Join([]string{openID, profile}, " "), "v4l1d_lao_id", "http://localhost:3008/",
		respTypeIDToken, "st4te", "query", 3003)
	res, err := http.Get(qrURL)
	log.Info().Msg(qrURL)
	if err != nil {
		return res, err
	}
	return res, nil
}

// helper method creating a valid authorization request URL
func createAuthRequestURL(n string, c string, s string, l string,
	redir string, resType string, st string, resMode string, port int) string {
	params := url.Values{}
	params.Add(nonce, n)
	params.Add(clientID, c)
	params.Add(scope, s)
	params.Add(loginHint, l)
	params.Add(redirectURI, redir)
	params.Add(responseType, resType)
	params.Add(state, st)
	params.Add(responseMode, resMode)

	qrURL := fmt.Sprintf(BaseURL, port) + params.Encode()
	return qrURL
}

// TestAuthRequestFails tries different scenarios in which the authorization request should not be validated
func TestAuthRequestFails(t *testing.T) {
	logTester := zltest.New(t)

	l := zerolog.New(logTester).With().Timestamp().Logger()
	s, err := NewAuthServer(fakeHub{}, "localhost", 3007, l)
	require.NoError(t, err)
	s.Start()
	<-s.Started

	// init parameters map
	params := url.Values{}

	// first case, url without parameters
	emptyURL := fmt.Sprintf(BaseURL, 3007)

	res, err := http.Get(emptyURL)
	log.Info().Msg(emptyURL)
	require.NoError(t, err)

	lastEntry := logTester.LastEntry()
	lastEntry.ExpLevel(zerolog.ErrorLevel)
	lastEntry.ExpMsg("Error while verifying the parameters of the request")
	bodyBytes := helperValidateGetAndParseBody(t, res)

	// we require that in the error response, the number of missing arguments is equal to 6
	helperMissingArgs(t, bodyBytes, 6)

	// add a nonce
	params.Add(nonce, "some_n0nc3")

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

	// testing request with valid number of parameters, but invalid scope

	invalidScopeURL := createAuthRequestURL("n", "c", "invalid", "l", "localhost:3001", respTypeIDToken, " ", " ", 3007)
	_, err = http.Get(invalidScopeURL)

	// no error from the get request
	require.NoError(t, err)

	lastEntry = logTester.LastEntry()
	lastEntry.ExpLevel(zerolog.ErrorLevel)
	lastEntry.ExpMsg("Error while validating the auth request")

	// testing request with wrong response type
	invalidResTypeURL := createAuthRequestURL("n", "c", openID, "l", "localhost:3001", "invalid", "", "", 3007)

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
	s, err := NewAuthServer(fakeHub{}, "localhost", 3004, l)
	require.NoError(t, err)
	s.Start()
	<-s.Started

	//parameters definition for the unit test
	clientID := "client"
	laoID := "lao"
	nonce := "nonce"
	state := "state"
	redirectURI := "https://example.com/"
	scope := strings.Join([]string{openID, profile}, " ")
	resType := respTypeIDToken

	// create the URL of the PopCHA webpage
	u := createAuthRequestURL(nonce, clientID, scope, laoID, redirectURI, resType, state, "", 3004)

	_, err = http.Get(u)
	require.NoError(t, err)
	l.Info().Msg(u)

	// constructing the unique URL endpoint of the PopCHA Websocket server.
	popChaPath := strings.Join([]string{responseEndpoint, laoID, "authentication", clientID, nonce}, "/")

	//  ws://popcha.example/response/lao/authentication/client/nonce
	popChaWsURL := url.URL{Scheme: "ws", Host: "localhost:3004", Path: popChaPath}

	// instantiating websocket connection
	client, err := newWSClient(popChaWsURL)
	require.NoError(t, err)

	//creating fake redirect URI parameters, for example just with the clientID. We are not
	// testing the validity of the parameters here, but rather that the websocket protocol doesn't
	// throw errors.
	fakeParams := url.Values{}
	fakeParams.Add("client_id", clientID)
	err = client.conn.WriteMessage(websocket.TextMessage, []byte(fakeParams.Encode()))
	require.NoError(t, err)

	err = s.Shutdown()
	require.NoError(t, err)
	<-s.Stopped
}

// TestAuthorizationServerWorkflow tests the control flow of the websocket communication.
// It tries to connect on /response endpoint without any additional path, and then
// tests the protocol with well-behaved clients using a valid path.
func TestAuthorizationServerWorkflow(t *testing.T) {
	logFile, err := os.CreateTemp("", "popcha_test_logs")
	require.NoError(t, err)
	defer func() {
		logFile.Close()
		os.Remove(logFile.Name())
	}()

	l := zerolog.New(logFile).With().Timestamp().Logger()
	s, err := NewAuthServer(fakeHub{}, "localhost", 3005, l)
	require.NoError(t, err)
	s.Start()
	<-s.Started

	// test error on /response with empty path suffix
	emptyPathURL := url.URL{Scheme: "ws", Host: "localhost:3005", Path: responseEndpoint}
	emptyPathClient, err := newWSClient(emptyPathURL)
	require.NoError(t, err)

	// send any message to the websocket server
	err = emptyPathClient.conn.WriteMessage(websocket.TextMessage, []byte("test"))
	require.NoError(t, err)

	// Read log file contents and check for the expected log message
	err = logFile.Sync()
	require.NoError(t, err)
	lastLine, err := getLastLine(logFile.Name())
	require.NoError(t, err)
	require.Contains(t, lastLine, "Error while receiving a request on /response: empty path")

	// create two clients, a sender and a receiver, on a valid path
	validPath := strings.Join([]string{responseEndpoint, "laoid", "authentication", "clientid", "nonce"}, "/")

	validURL := url.URL{Scheme: "ws", Host: "localhost:3005", Path: validPath}

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

	// Read log file contents and check for the expected log message
	err = logFile.Sync()
	require.NoError(t, err)
	lastLine, err = getLastLine(logFile.Name())
	require.NoError(t, err)
	require.Contains(t, lastLine, "Received the correct message from the sender client.")

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
	s, err := NewAuthServer(fakeHub{}, "localhost", 3006, l)
	require.NoError(t, err)

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

	err = s.generateQRCode(&fakeResponseWriter{}, req, "l", "c", "n", "example.com", fragment)
	require.Error(t, err)

	// testing that the QRCode can be generated with special characters, due to URL encoding
	specialCharURL := &url.URL{
		Scheme: "http",
		Host:   "example.com",
		Path:   "client_id=" + strings.Repeat("ち💆🏻‍ҨऔȢЖ", 3),
	}

	req.URL = specialCharURL
	err = s.generateQRCode(&fakeResponseWriter{}, req, "l", "c", "n", "example.com", query)
	require.NoError(t, err)
}

// TestClientParams tests the validity of the client parameters
func TestClientParams(t *testing.T) {
	l := zerolog.New(io.Discard)
	s, err := NewAuthServer(fakeHub{}, "localhost", 3009, l)
	require.NoError(t, err)
	s.Start()
	<-s.Started

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
	<-s.Stopped
}

// generates a valid client for PopCHA
func randomClientParams(r *rand.Rand) clientParams {
	c := clientParams{
		clientID:     genString(r, r.Intn(MaxStringSize)),
		redirectURIs: []string{"localhost:3500"},
		resType:      respTypeIDToken,
	}
	return c
}

func noIDClientParam() clientParams {
	c := clientParams{
		clientID:     "",
		redirectURIs: []string{"localhost:3500"},
		resType:      respTypeIDToken,
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
		c.ResponseTypes()[0] == respTypeIDToken &&
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

// getLastLine returns the last line of a file
func getLastLine(path string) (string, error) {
	file, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer file.Close()

	var lastLine string
	scanner := bufio.NewScanner(file)
	i := 1
	for scanner.Scan() {
		lastLine = scanner.Text()
		i++
	}
	if err = scanner.Err(); err != nil {
		return "", err
	}
	return lastLine, scanner.Err()
}
