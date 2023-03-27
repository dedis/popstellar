package server

import (
	"bytes"
	"crypto/sha256"
	"fmt"
	"github.com/aaronarduino/goqrsvg"
	"github.com/ajstarks/svgo"
	"github.com/boombuler/barcode/qr"
	"github.com/gorilla/mux"
	"github.com/rs/zerolog"
	"github.com/zitadel/oidc/v2/pkg/oidc"
	_ "github.com/zitadel/oidc/v2/pkg/op"
	"html/template"
	"net/http"
	"os"
	"popstellar"
	"popstellar/hub"
	"popstellar/network"
	"popstellar/network/socket"
	"popstellar/popcha/storage"
	"strings"
	"time"
)

// Storage defines an in-memory storage containing the different long term identifiers per clients,
// signature protocols and auth requests per client ID. it implements op.Storage

type AuthRequest struct {
	ID           string
	timestamp    time.Time
	responseType oidc.ResponseType
	clientID     string
	redirectURI  string
	Scopes       []string
	Nonce        string
	State        string

	isDone bool
}

func (a AuthRequest) GetID() string {
	return a.ID
}

func (a AuthRequest) GetACR() string {
	return "" // not handling ACR case
}

func (a AuthRequest) GetAMR() []string {
	return []string{""}
}

func (a AuthRequest) GetAudience() []string {
	return []string{""} // won't handle audiences for our protocol
}

func (a AuthRequest) GetAuthTime() time.Time {
	return a.timestamp
}

func (a AuthRequest) GetClientID() string {
	return a.clientID
}

func (a AuthRequest) GetCodeChallenge() *oidc.CodeChallenge {
	// TODO Implement QR challenge
	return nil
}

func (a AuthRequest) GetNonce() string {
	return a.Nonce
}

func (a AuthRequest) GetRedirectURI() string {
	return a.redirectURI
}

func (a AuthRequest) GetResponseType() oidc.ResponseType {
	return a.responseType
}

func (a AuthRequest) GetResponseMode() oidc.ResponseMode {
	return "" // not handling this for PopCHA protocol
}

func (a AuthRequest) GetScopes() []string {
	return a.Scopes
}

func (a AuthRequest) GetState() string {
	return a.State
}

func (a AuthRequest) GetSubject() string {
	return "" // not used for our flow
}

func (a AuthRequest) Done() bool {
	return a.isDone
}

type AuthorizationServer struct {
	log               zerolog.Logger
	server            *network.Server
	store             *storage.Storage
	challengeServAddr string
	key               [32]byte // key for encryption of data, optional?
}

func NewAuthServer(hub hub.Hub, webSocketAddr string, webSocketPort int, st socket.SocketType, httpAddr string, httpPort int,
	seed string, log zerolog.Logger) (*AuthorizationServer, error) {
	s := network.NewServer(hub, webSocketAddr, webSocketPort, st, log)

	// key for encryption
	key := sha256.Sum256([]byte(seed))
	store, err := storage.NewStorage()
	if err != nil {
		return nil, err
	}

	as := AuthorizationServer{
		log: popstellar.Logger.With().
			Str("role", "authorization server").Logger(),
		server:            s,
		store:             store,
		challengeServAddr: fmt.Sprintf("%s:%d", httpAddr, httpPort),
		key:               key,
	}

	as.newChallengeServer(st)
	return &as, nil
}

func (a *AuthorizationServer) newChallengeServer(endpoint socket.SocketType) {

	r := mux.NewRouter()
	r.PathPrefix(fmt.Sprintf("/%s", endpoint)).HandlerFunc(a.HandleValidateRequest).Methods("GET")

	go func() {
		err := http.ListenAndServe(a.challengeServAddr, r)
		if err != nil {
			a.log.Err(err).Msg("Error while starting the challenge server: %v")
		}
	}()
}

// HandleValidateRequest is a HTTP handler for the Challenge Server of PoPCHA. It only responds to GET request,
// parse them into authorization requests, validate them, and if validated, displays a QRCode containing the different
// information necessary for the client to be authenticated using its PoP App.
func (a *AuthorizationServer) HandleValidateRequest(w http.ResponseWriter, req *http.Request) {
	w.Header().Set("Content-Type", "text/html")
	nce := req.URL.Query().Get("nonce")
	cid := req.URL.Query().Get("client_id")
	laoID := req.URL.Query().Get("lao_id")
	var buffer bytes.Buffer

	// new SVG
	s := svg.New(&buffer)

	webserverURL := a.challengeServAddr
	clientID := cid
	nonce := nce
	data := strings.Join([]string{webserverURL, laoID, clientID, nonce}, "|")
	qrCode, _ := qr.Encode(data, qr.M, qr.Auto)

	// Write QR code to SVG
	qs := goqrsvg.NewQrSVG(qrCode, 10)
	qs.StartQrSVG(s)
	err := qs.WriteQrSVG(s)
	if err != nil {
		return
	}
	s.End()

	d := struct {
		SVGImage template.HTML
	}{
		SVGImage: template.HTML(buffer.String()),
	}

	templateFile := "be1-go/popcha/qrcode/popcha.html"
	templateContent, err := os.ReadFile(templateFile)
	if err != nil {
		fmt.Println("Error while reading template file:", err)
		os.Exit(1)
	}
	tmpl := template.Must(template.New("").Parse(string(templateContent)))
	err = tmpl.Execute(w, d)
	if err != nil {
		fmt.Println("Error while executing template:", err)
		os.Exit(1)
	}
}
