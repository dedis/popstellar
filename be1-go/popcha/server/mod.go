package server

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
	"popstellar/popcha/storage"
	"strings"
	"sync"
	"time"
)

const (
	// formatting for auth request validation error
	errValidAuthFormat = "Error while validating the auth request: %s"

	//error message for invalid response type
	errInvalidRepsonseType = "response type is invalid for the implicit flow"

	// defaultValue for the ID token lifetime (in hours)
	tokenLifeTimeHour = 24

	//file path for valid QRCode Displaying page
	QRCodeDisplay = "../qrcode/popcha.html"

	//size of QRCode SVG
	QRSize = 10
)

// constant parameter names
const (
	ClientID     = "client_id"
	Nonce        = "nonce"
	Scope        = "scope"
	LaoID        = "lao_id"
	RedirectURI  = "redirect_uri"
	ResponseType = "response_type"
	State        = "state"
	ResTypeMulti = "id_token token"
	ResTypeId    = "id_token"
	OpenID       = "openid"
	Profile      = "profile"
)

// clientParams implements op.Client
// as PoPCHA don't support registered clients, clientParams is an alternative which defines  parameters
// for any client sending an auth request. As an example, any client requesting authorization has to use
// the implicit flow.
type clientParams struct {
	clientID      string
	redirectURIs  []string
	responseTypes []oidc.ResponseType
}

// returns the clientID
func (c clientParams) GetID() string {
	return c.clientID
}

func (c clientParams) RedirectURIs() []string {
	return c.redirectURIs
}

func (c clientParams) PostLogoutRedirectURIs() []string {
	return nil
}

func (c clientParams) ApplicationType() op.ApplicationType {
	return op.ApplicationTypeNative
}

func (c clientParams) AuthMethod() oidc.AuthMethod {
	return oidc.AuthMethodNone
}

func (c clientParams) ResponseTypes() []oidc.ResponseType {
	return c.responseTypes
}

func (c clientParams) GrantTypes() []oidc.GrantType {
	return []oidc.GrantType{oidc.GrantTypeImplicit}
}

func (c clientParams) LoginURL(_ string) string {
	return ""
}

func (c clientParams) AccessTokenType() op.AccessTokenType {
	return op.AccessTokenTypeBearer
}

func (c clientParams) IDTokenLifetime() time.Duration {
	return tokenLifeTimeHour * time.Hour
}

func (c clientParams) DevMode() bool {
	return false
}

func (c clientParams) RestrictAdditionalIdTokenScopes() func(scopes []string) []string {
	return nil
}

func (c clientParams) RestrictAdditionalAccessTokenScopes() func(scopes []string) []string {
	return nil
}

func (c clientParams) IsScopeAllowed(_ string) bool {
	return false
}

func (c clientParams) IDTokenUserinfoClaimsAssertion() bool {
	return true
}

func (c clientParams) ClockSkew() time.Duration {
	return 0
}

func internalClientParams(clientID string, redURIS []string, respTypes []oidc.ResponseType) *clientParams {
	return &clientParams{
		clientID:      clientID,
		redirectURIs:  redURIS,
		responseTypes: respTypes,
	}
}

type AuthorizationServer struct {
	log               zerolog.Logger
	store             *storage.Storage
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
	store, err := storage.NewStorage(log)
	if err != nil {
		return nil, err
	}

	as := AuthorizationServer{
		log: log.With().
			Str("role", "authorization server").Logger(),
		store:             store,
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
	params, oidcReq, err := verifyParamsAndCreateRequest(req)
	if err != nil {
		as.log.Fatal().Err(err)
		return
	}

	//validate the parameters of the authorization request
	err = as.ValidateAuthRequest(oidcReq)
	if err != nil {
		as.log.Fatal().Err(err).Msg("Error while validating auth request")
		return
	}

	// adding nonce to storage, marking it as used
	err = as.store.AddNonce(oidcReq.Nonce)
	if err != nil {
		as.log.Fatal().Err(err).Msg("Error while validating nonce")
		return
	}

	// generate PoPCHA QRCode
	err = as.generateQRCode(w, params)
	if err != nil {
		as.log.Fatal().Err(err).Msg("Error while generating PoPCHA QRCode")
	}
}

// generateQRCode builds a PoPCHA QRCode and executes an HTML template including it, given authorization parameters.
func (as *AuthorizationServer) generateQRCode(w http.ResponseWriter, params map[string]string) error {
	var buffer bytes.Buffer
	// new SVG buffer
	s := svg.New(&buffer)

	// getting the arguments
	serverURL := as.challengeServAddr
	clientID := params[ClientID]
	nonce := params[Nonce]
	laoID := params[LaoID]

	// creating the QRCode content
	data := strings.Join([]string{serverURL, laoID, clientID, nonce}, "|")
	qrCode, err := qr.Encode(data, qr.M, qr.Auto)
	if err != nil {
		return err
	}

	// Write QR code to SVG
	qs := goqrsvg.NewQrSVG(qrCode, QRSize)
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

	templateFile := QRCodeDisplay
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
	client := internalClientParams(req.ClientID, []string{req.RedirectURI}, []oidc.ResponseType{req.ResponseType})

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
	if !(rType == ResTypeId || rType == ResTypeMulti) {
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
	}
}

// verifyParamsAndCreateRequest is an internal method used to parse a http request into a parameter table,
// verify that all the required parameters are present, and build an OIDC Auth Request object.
func verifyParamsAndCreateRequest(req *http.Request) (map[string]string, *oidc.AuthRequest, error) {

	// Parameters parsing
	params := make(map[string]string)
	params[ResponseType] = req.URL.Query().Get(ResponseType)
	params[RedirectURI] = req.URL.Query().Get(RedirectURI)
	params[Nonce] = req.URL.Query().Get(Nonce)
	params[Scope] = req.URL.Query().Get(Scope)
	params[ClientID] = req.URL.Query().Get(ClientID)
	params[LaoID] = req.URL.Query().Get(LaoID)

	var nilStrings = make([]string, 0)
	for k, v := range params {
		if v == "" {
			nilStrings = append(nilStrings, k)
		}
	}

	// at least a parameter is missing
	if len(nilStrings) != 0 {
		return nil, nil, xerrors.Errorf("missing arguments %s", strings.Join(nilStrings, " "))
	}
	//optional parameters
	state := req.URL.Query().Get(State)
	if state != "" {
		params[State] = state

	}
	//build auth request
	oidcReq := createOIDCRequestFromParams(params)

	return params, oidcReq, nil
}
