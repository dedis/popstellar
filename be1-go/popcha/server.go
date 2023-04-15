package popcha

import (
	"bytes"
	"context"
	"crypto/sha256"
	"fmt"
	"github.com/aaronarduino/goqrsvg"
	"github.com/ajstarks/svgo"
	"github.com/boombuler/barcode/qr"
	"github.com/gorilla/mux"
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
	errInvalidRepsonseType = "response type is invalid for the implicit flow"

	//error message for unimplemented methods of op.Client
	errUnimplementedMethod = "this method is not implemented for our protocol"

	// defaultValue for the ID token lifetime (in hours)
	tokenLifeTimeHour = 24

	// qrCodeWebPage file path for valid QRCode Displaying page
	qrCodeWebPage = "qrcode/popcha.html"

	// qrSize size of QRCode SVG
	qrSize = 10

	badRequestCode = 400
)

// constant parameter names
const (
	ClientID     = "client_id"
	Nonce        = "nonce"
	Scope        = "scope"
	RedirectURI  = "redirect_uri"
	ResponseType = "response_type"
	State        = "state"
	ResTypeMulti = "id_token token"
	OpenID       = "openid"
	Profile      = "profile"
	LoginHint    = "login_hint"
)

// clientParams implements op.Client
// as PoPCHA don't support registered clients, clientParams is an alternative which defines  parameters
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

func (c clientParams) PostLogoutRedirectURIs() []string {
	panic(errUnimplementedMethod)
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

func (c clientParams) LoginURL(_ string) string {
	panic(errUnimplementedMethod)
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

func (c clientParams) RestrictAdditionalIdTokenScopes() func(scopes []string) []string {
	panic(errUnimplementedMethod)
}

func (c clientParams) RestrictAdditionalAccessTokenScopes() func(scopes []string) []string {
	panic(errUnimplementedMethod)
}

// IsScopeAllowed returns whether the given scope is allowed. We disable this feature by default to restrict the scopes
// to a pre-defined set.
func (c clientParams) IsScopeAllowed(_ string) bool {
	panic(errUnimplementedMethod)
}

func (c clientParams) IDTokenUserinfoClaimsAssertion() bool {
	panic(errUnimplementedMethod)
}

// ClockSkew is a parameter used to synchronize two auth servers. It is a required parameter, but will be set at 0 by
// default.
func (c clientParams) ClockSkew() time.Duration {
	return 0
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
	key               [32]byte // key for encryption of data, optional?
	Stopped           chan struct{}
	Started           chan struct{}
	closing           *sync.Mutex
}

func NewAuthServer(hub hub.Hub, st string, httpAddr string, httpPort int, seed string,
	log zerolog.Logger) (*AuthorizationServer, error) {

	// key for encryption
	key := sha256.Sum256([]byte(seed))

	as := AuthorizationServer{
		log: log.With().
			Str("role", "authorization server").Logger(),
		hub:               hub,
		challengeServAddr: fmt.Sprintf("%s:%d", httpAddr, httpPort),
		key:               key,
		Stopped:           make(chan struct{}, 1),
		Started:           make(chan struct{}, 1),
		closing:           &sync.Mutex{},
	}

	as.httpServer = as.newChallengeServer(st)
	return &as, nil
}

// Start launches a go routine to start the server
func (as *AuthorizationServer) Start() {
	go func() {
		as.log.Info().Msgf("starting the authorization server at: %s", as.challengeServAddr)
		as.Started <- struct{}{}
		err := as.httpServer.ListenAndServe()
		if err != nil && !xerrors.Is(err, http.ErrServerClosed) {
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

	err := as.httpServer.Shutdown(context.Background())
	if err != nil {
		return xerrors.Errorf("failed to shutdown authorization server: %v", err)
	}
	return nil
}

// newChallengeServer creates a new HTTP Server containing a multiplexing router,
// given an endpoint.
func (as *AuthorizationServer) newChallengeServer(endpoint string) *http.Server {

	r := mux.NewRouter()
	r.PathPrefix(fmt.Sprintf("/%s", endpoint)).HandlerFunc(as.HandleRequest)

	srv := &http.Server{
		Addr:    as.challengeServAddr,
		Handler: r,
	}
	return srv

}

// HandleRequest is a HTTP handler for the Challenge Server of PoPCHA. It only responds to GET request,
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
	err = as.generateQRCode(w, req)
	if err != nil {
		as.handleBadRequest(w, err, "Error while generating PoPCHA QRCode")
	}
}

func (as *AuthorizationServer) handleBadRequest(w http.ResponseWriter, err error, msg string) {
	as.log.Err(err).Msg(msg)
	w.WriteHeader(badRequestCode)
	_, err = w.Write([]byte(strconv.Itoa(badRequestCode) + " - " + err.Error()))
	if err != nil {
		as.log.Err(err).Msg("Error while writing error message in the HTTP repsonse body")
	}
}

// generateQRCode builds a PoPCHA QRCode and executes an HTML template including it, given authorization parameters.
func (as *AuthorizationServer) generateQRCode(w http.ResponseWriter, req *http.Request) error {
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

	// internal HTML template struct, including the svg buffer
	d := struct {
		SVGImage template.HTML
	}{
		SVGImage: template.HTML(buffer.String()),
	}

	templateFile := qrCodeWebPage
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

// ValidateAuthRequest takes an OpenID request, and validates its parameters according to the PoPCHA and Implicit
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

	// validate the response type according to the implicit flow protocol
	return as.validateImplicitFlowResponseType(client, req)
}

// validateImplicitFlowResponseType validates the response type structure, but also its validity according to the
// implicit flow protocol.
func (as *AuthorizationServer) validateImplicitFlowResponseType(params *clientParams, req *oidc.AuthRequest) error {
	rType := req.ResponseType
	// only two response types are allowed
	if rType != ResTypeMulti {
		return oidc.ErrInvalidRequest().WithDescription(errValidAuthFormat, errInvalidRepsonseType)
	}

	// further validation
	return op.ValidateAuthReqResponseType(params, req.ResponseType)
}

// createOIDCRequestFromParams takes parameters, and builds an OIDC authorization request.
// parameters are validated before calling this method
func createOIDCRequestFromParams(params map[string]string) *oidc.AuthRequest {

	return &oidc.AuthRequest{
		Scopes:       strings.Split(params[Scope], " "),
		ResponseType: oidc.ResponseType(params[ResponseType]),
		ClientID:     params[ClientID],
		RedirectURI:  params[RedirectURI],
		State:        params[State],
		Nonce:        params[Nonce],
		LoginHint:    params[LoginHint],
	}
}

// verifyParamsAndCreateRequest is an internal method used to parse a http request into a parameter table,
// verify that all the required parameters are present, and build an OIDC Auth Request object.
func verifyParamsAndCreateRequest(req *http.Request) (*oidc.AuthRequest, error) {

	// Parameters parsing
	params := make(map[string]string)
	params[ResponseType] = req.URL.Query().Get(ResponseType)
	params[RedirectURI] = req.URL.Query().Get(RedirectURI)
	params[Nonce] = req.URL.Query().Get(Nonce)
	params[Scope] = req.URL.Query().Get(Scope)
	params[ClientID] = req.URL.Query().Get(ClientID)
	params[LoginHint] = req.URL.Query().Get(LoginHint)

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
	state := req.URL.Query().Get(State)
	if state != "" {
		params[State] = state

	}
	//build auth request
	oidcReq := createOIDCRequestFromParams(params)

	return oidcReq, nil
}
