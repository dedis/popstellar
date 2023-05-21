package popcha

import (
	"bytes"
	"context"
	"fmt"
	"github.com/aaronarduino/goqrsvg"
	"github.com/ajstarks/svgo"
	"github.com/boombuler/barcode/qr"
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"
	"github.com/zitadel/oidc/v2/pkg/oidc"
	"github.com/zitadel/oidc/v2/pkg/op"
	"golang.org/x/xerrors"
	"html/template"
	"net/http"
	"os"
	"popstellar/hub"
	"strconv"
	"strings"
	"sync"
	"time"
)

const (
	// formatting for auth request validation error
	errValidAuthFormat = "Error while validating the auth request: %s"

	//error message for invalid response type
	errInvalidResponseType = "Response type should be " + resTypeMulti

	//error message for unimplemented methods of op.Client
	errUnimplementedMethod = "this method is not implemented for our protocol"

	// defaultValue for the ID token lifetime (in hours)
	tokenLifeTimeHour = 24

	// qrSize size of QRCode SVG
	qrSize = 10

	badRequestCode = 400

	//endpoint for websocket communication
	responseEndpoint = "/response"
)

// constant parameter names
const (
	clientID     = "client_id"
	nonce        = "nonce"
	scope        = "scope"
	redirectURI  = "redirect_uri"
	responseType = "response_type"
	state        = "state"
	resTypeMulti = "id_token token"
	openID       = "openid"
	profile      = "profile"
	loginHint    = "login_hint"
	responseMode = "response_mode"
	// modes of response
	query    = "query"
	fragment = "fragment"
)

// variable for upgrading the http connection into a websocket one
var upgrader = websocket.Upgrader{}

// clientParams implements op.Client
// as PoPCHA don't support registered clients, clientParams is an alternative which defines parameters
// for any client sending an auth request. As an example, any client requesting authorization has to use
// the implicit flow.
type clientParams struct {
	clientID     string
	redirectURIs []string
	resType      oidc.ResponseType
}

/*
Most of the methods of op.Client are implemented, however some of them
are not going to be used in our Implicit Flow protocol.
*/

// GetID returns the clientID
func (c clientParams) GetID() string {
	return c.clientID
}

// RedirectURIs returns all the registered and valid redirect URIs
func (c clientParams) RedirectURIs() []string {
	return c.redirectURIs
}

// ApplicationType designates whether the client supports https URIs or not
func (c clientParams) ApplicationType() op.ApplicationType {
	return op.ApplicationTypeNative
}

// AuthMethod returns the type of authentication mechanism used in our flow. We use a custom one, so we return
// none by default.
func (c clientParams) AuthMethod() oidc.AuthMethod {
	return oidc.AuthMethodNone
}

// ResponseTypes returns the valid types of response in the Implicit Flow.
func (c clientParams) ResponseTypes() []oidc.ResponseType {
	return []oidc.ResponseType{c.resType}
}

// GrantTypes returns the type of Flow used in PoPCHA (Implicit in our case)
func (c clientParams) GrantTypes() []oidc.GrantType {
	return []oidc.GrantType{oidc.GrantTypeImplicit}
}

// AccessTokenType returns the standard type of token used in PoPCHA
func (c clientParams) AccessTokenType() op.AccessTokenType {
	return op.AccessTokenTypeBearer
}

// IDTokenLifetime returns the lifetime of the token
func (c clientParams) IDTokenLifetime() time.Duration {
	return tokenLifeTimeHour * time.Hour
}

// DevMode is a utility boolean used to bypass the validation of the auth request.
// It is disabled by default.
func (c clientParams) DevMode() bool {
	return false
}

// ClockSkew is a parameter used to synchronize two auth servers. It is a required parameter, but will be set at 0 by
// default.
func (c clientParams) ClockSkew() time.Duration {
	return 0
}

// IsScopeAllowed returns whether the given scope is allowed. We set it to false by default for any additional scope
func (c clientParams) IsScopeAllowed(_ string) bool {
	return false
}

/*

 Unimplemented methods

*/

func (c clientParams) PostLogoutRedirectURIs() []string {
	panic(errUnimplementedMethod)
}

func (c clientParams) RestrictAdditionalIdTokenScopes() func(scopes []string) []string {
	panic(errUnimplementedMethod)
}

func (c clientParams) RestrictAdditionalAccessTokenScopes() func(scopes []string) []string {
	panic(errUnimplementedMethod)
}

func (c clientParams) LoginURL(_ string) string {
	panic(errUnimplementedMethod)
}

func (c clientParams) IDTokenUserinfoClaimsAssertion() bool {
	panic(errUnimplementedMethod)
}

// generates the client parameters for our Implicit Flow Protocol
func internalClientParams(clientID string, redURIS []string, respTypes oidc.ResponseType) *clientParams {
	return &clientParams{
		clientID:     clientID,
		redirectURIs: redURIS,
		resType:      respTypes,
	}
}

// AuthorizationServer defines the HTTP server displaying the QR Code webpage, and
// redirecting clients with their access tokens.
type AuthorizationServer struct {
	log               zerolog.Logger
	httpServer        *http.Server
	hub               hub.Hub
	challengeServAddr string
	internalConns     map[string]*websocket.Conn
	Stopped           chan struct{}
	Started           chan struct{}
	closing           *sync.Mutex
	connsMutex        *sync.Mutex
	htmlFilePath      string
}

// NewAuthServer creates an authorization server, given a hub, an address and port,
// the path of the html file it will display, and a logger.
func NewAuthServer(hub hub.Hub, httpAddr string, httpPort int, htmlFilePath string, log zerolog.Logger) *AuthorizationServer {

	as := AuthorizationServer{
		log: log.With().
			Str("role", "authorization server").Logger(),
		hub:               hub,
		challengeServAddr: fmt.Sprintf("%s:%d", httpAddr, httpPort),
		Stopped:           make(chan struct{}, 1),
		Started:           make(chan struct{}, 1),
		closing:           &sync.Mutex{},
		internalConns:     make(map[string]*websocket.Conn),
		connsMutex:        &sync.Mutex{},
		htmlFilePath:      htmlFilePath,
	}

	// creation of the http server
	as.httpServer = as.newChallengeServer()
	return &as
}

// Start launches a go routine to start the server
func (as *AuthorizationServer) Start() {
	go func() {
		as.log.Info().Msgf("starting the authorization server at: %s", as.challengeServAddr)
		as.Started <- struct{}{}
		err := as.httpServer.ListenAndServe()
		if err != nil && err != http.ErrServerClosed {
			as.log.Fatal().Err(err).Msg("Error while starting the challenge server: %v")
		}

		as.Stopped <- struct{}{}
		as.log.Info().Msg("Stopped the Authorization server...")
	}()
}

// Shutdown stops the serve gracefully
func (as *AuthorizationServer) Shutdown() error {
	as.closing.Lock()
	defer as.closing.Unlock()

	as.log.Info().Msgf("shutting down PoPCHA Server %s", as.challengeServAddr)

	err := as.httpServer.Shutdown(context.Background())
	if err != nil {
		return xerrors.Errorf("failed to shutdown authorization server: %v", err)
	}
	return nil
}

// newChallengeServer creates a new HTTP Server containing a multiplexing router,
// given the request endpoint.
func (as *AuthorizationServer) newChallengeServer() *http.Server {

	r := mux.NewRouter()
	// handler for request endpoint
	r.PathPrefix("/authorize").HandlerFunc(as.HandleRequest)
	//handler for pop backend communication endpoint
	r.PathPrefix(responseEndpoint).HandlerFunc(as.responseEndpoint)

	srv := &http.Server{
		Addr:    as.challengeServAddr,
		Handler: r,
	}
	return srv

}

// HandleRequest is an HTTP handler for the Challenge Server of PoPCHA. It only responds to GET request,
// parse them into authorization requests, validate them, and if validated, displays a QRCode containing the different
// information necessary for the client to be authenticated using its PoP App.
func (as *AuthorizationServer) HandleRequest(w http.ResponseWriter, req *http.Request) {
	w.Header().Set("Content-Type", "text/html")

	// take the request, parse its parameters ang creates an auth request object
	oidcReq, err := verifyParamsAndCreateRequest(req)
	if err != nil {
		as.handleBadRequest(w, err, "Error while verifying the parameters of the request")
		return
	}

	//validate the parameters of the authorization request
	err = as.ValidateAuthRequest(oidcReq)
	if err != nil {
		as.handleBadRequest(w, err, "Error while validating the auth request")

		return
	}

	// generate PoPCHA QRCode
	err = as.generateQRCode(w, req, oidcReq.LoginHint, oidcReq.ClientID, oidcReq.Nonce, oidcReq.RedirectURI)
	if err != nil {
		as.handleBadRequest(w, err, "Error while generating PoPCHA QRCode")
	}
}

// helper handling errors when receiving http requests.
func (as *AuthorizationServer) handleBadRequest(w http.ResponseWriter, err error, msg string) {
	as.log.Err(err).Msg(msg)
	w.WriteHeader(badRequestCode)
	_, err = w.Write([]byte(strconv.Itoa(badRequestCode) + " - " + err.Error()))
	if err != nil {
		as.log.Err(err).Msg("Error while writing error message in the HTTP response body")
	}
}

// generateQRCode builds a PoPCHA QRCode and executes an HTML template including it, given authorization parameters.
func (as *AuthorizationServer) generateQRCode(w http.ResponseWriter, req *http.Request, laoID string, clientID string,
	nonce string, redirectHost string) error {
	var buffer bytes.Buffer
	// new SVG buffer
	s := svg.New(&buffer)

	// QRCode contains the Auth request URL
	data := req.Host + req.URL.String()
	qrCode, err := qr.Encode(data, qr.M, qr.Auto)
	if err != nil {
		return err
	}

	// Write QR code to SVG
	qs := goqrsvg.NewQrSVG(qrCode, qrSize)
	qs.StartQrSVG(s)
	err = qs.WriteQrSVG(s)
	if err != nil {
		return err
	}
	// finalizing the buffer construction
	s.End()

	// internal HTML template struct, including the svg buffer, the Websocket and redirect addresses
	d := struct {
		SVGImage      template.HTML
		WebSocketAddr string
		RedirectHost  string
	}{
		SVGImage:      template.HTML(buffer.String()),
		WebSocketAddr: "ws://" + req.Host + strings.Join([]string{responseEndpoint, laoID, "authentication", clientID, nonce}, "/"),
		RedirectHost:  redirectHost,
	}

	templateFile := as.htmlFilePath
	// reading HTML template bytes
	templateContent, err := os.ReadFile(templateFile)
	if err != nil {
		return err
	}

	// reading the template bytes into a template object
	tmpl := template.Must(template.New("").Parse(string(templateContent)))
	// executing the template using the internal svg struct
	err = tmpl.Execute(w, d)
	if err != nil {
		return err
	}
	return nil
}

// helper method used for handling redirect uri by the Webpage websocket. Requests on the /response endpoint are
// solely handled through websocket. Because of the protocol, the first websocket to send a request will be the JS
// one.
func (as *AuthorizationServer) responseEndpoint(w http.ResponseWriter, r *http.Request) {

	//websocket upgrade
	c, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		as.log.Error().Msgf("Error while trying to upgrade connection to Websocket: %v", err)
		return
	}
	p := r.URL.Path[len(responseEndpoint):]
	// if the path is empty, send an error and return
	if p == "" {
		as.log.Error().Msg("Error while receiving a request on /response: empty path")
		return
	} else {
		// if it is the first connection on that path, we register it and return
		if as.isFirstConnection(p, c) {
			return
		}
	}

	as.handleClientResponse(p, c)

}

// helper method checking whether the given connection is the first one on the given path.
func (as *AuthorizationServer) isFirstConnection(path string, c *websocket.Conn) bool {
	as.connsMutex.Lock()
	_, ok := as.internalConns[path]
	// if no connection on that path has been made, add it to the map
	if !ok {
		as.internalConns[path] = c
		as.connsMutex.Unlock()
		// the first connection is the javascript websocket. It will simply receive a message
		// from the client.
		return true
	}
	as.connsMutex.Unlock()
	return false
}

// helper method handling client messages, and sending them to the JS websocket connection.
func (as *AuthorizationServer) handleClientResponse(path string, c *websocket.Conn) {
	// the server will read the messages from the client, and write it to the javascript websocket.
	mt, message, err := c.ReadMessage()
	if err != nil {
		as.log.Error().Msgf(" Error while reading websocket messages on /response: %v", err)
		return
	}
	as.connsMutex.Lock()
	co, ok := as.internalConns[path]
	// verifying that the javascript connection has not been deleted
	if string(message) != "" && ok {
		err = co.WriteMessage(mt, message)
		if err != nil {
			as.log.Error().Msgf("Error while writing websocket message on /response: %v", err)
			return
		}
		//once the message has been sent to the javascript websocket, delete the connection from the map
		delete(as.internalConns, path)
	}
	as.connsMutex.Unlock()
}

// ValidateAuthRequest takes an openID request, and validates its parameters according to the PoPCHA and Implicit
// flow protocols.
func (as *AuthorizationServer) ValidateAuthRequest(req *oidc.AuthRequest) error {

	// creating client parameters, with a set of rules regarding PoPCHA protocol
	client := internalClientParams(req.ClientID, []string{req.RedirectURI}, req.ResponseType)

	//validating the scopes
	_, err := op.ValidateAuthReqScopes(client, req.Scopes)
	if err != nil {
		return err
	}

	//validating the redirectURI
	err = op.ValidateAuthReqRedirectURI(client, req.RedirectURI, req.ResponseType)
	if err != nil {
		return err
	}

	err = as.validateResponseMode(req)
	if err != nil {
		return err
	}
	// validate the response type according to the implicit flow protocol
	return as.validateImplicitFlowResponseType(client, req)
}

// validateImplicitFlowResponseType validates the response type structure, but also its validity according to the
// implicit flow protocol.
func (as *AuthorizationServer) validateImplicitFlowResponseType(params *clientParams, req *oidc.AuthRequest) error {
	rType := req.ResponseType
	// only two response types are allowed
	if rType != resTypeMulti {
		return oidc.ErrInvalidRequest().WithDescription(errValidAuthFormat, errInvalidResponseType)
	}

	// further validation
	return op.ValidateAuthReqResponseType(params, req.ResponseType)
}

func (as *AuthorizationServer) validateResponseMode(req *oidc.AuthRequest) error {
	if req.ResponseMode == "" {
		return nil
	} else if !(req.ResponseMode == query || req.ResponseMode == fragment) {
		return xerrors.Errorf("The provided response mode is %s, should be query or fragment", req.ResponseMode)
	}
	return nil
}

// createOIDCRequestFromParams takes parameters, and builds an OIDC authorization request.
// parameters are validated before calling this method
func createOIDCRequestFromParams(params map[string]string) *oidc.AuthRequest {

	return &oidc.AuthRequest{
		Scopes:       strings.Split(params[scope], " "),
		ResponseType: oidc.ResponseType(params[responseType]),
		ClientID:     params[clientID],
		RedirectURI:  params[redirectURI],
		State:        params[state],
		Nonce:        params[nonce],
		ResponseMode: oidc.ResponseMode(params[responseMode]),
		LoginHint:    params[loginHint],
	}
}

// verifyParamsAndCreateRequest is an internal method used to parse a http request into a parameter table,
// verify that all the required parameters are present, and build an OIDC Auth Request object.
func verifyParamsAndCreateRequest(req *http.Request) (*oidc.AuthRequest, error) {

	// Parameters parsing
	params := make(map[string]string)
	params[responseType] = req.URL.Query().Get(responseType)
	params[redirectURI] = req.URL.Query().Get(redirectURI)
	params[nonce] = req.URL.Query().Get(nonce)
	params[scope] = req.URL.Query().Get(scope)
	params[clientID] = req.URL.Query().Get(clientID)
	params[loginHint] = req.URL.Query().Get(loginHint)

	var nilStrings = make([]string, 0)
	for k, v := range params {
		if v == "" {
			nilStrings = append(nilStrings, k)
		}
	}

	// at least a parameter is missing
	if len(nilStrings) != 0 {
		return nil, xerrors.Errorf("missing arguments %s", strings.Join(nilStrings, " "))
	}
	//optional parameters
	st := req.URL.Query().Get(state)
	if st != "" {
		params[state] = st

	}
	resMode := req.URL.Query().Get(responseMode)
	if resMode != "" {
		params[responseMode] = resMode
	}
	//build auth request
	oidcReq := createOIDCRequestFromParams(params)

	return oidcReq, nil
}
