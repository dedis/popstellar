package authentication

import (
	"encoding/base64"
	"encoding/json"
	"github.com/golang-jwt/jwt/v4"
	"github.com/gorilla/websocket"
	"github.com/rs/xid"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
	"io"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"popstellar/internal/crypto"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/authentication/mauthentication"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/broadcast/mbroadcast"
	"popstellar/internal/handler/method/greetserver/mgreetserver"
	method2 "popstellar/internal/handler/method/publish/mpublish"
	"popstellar/internal/logger"
	"popstellar/internal/network/socket"
	"popstellar/internal/old/oldchannel"
	"popstellar/internal/validation"
	"sync"
	"testing"
	"time"
)

const (
	relativeMsgDataExamplePath string = "../../../validation/protocol/examples/messageData"
	relativeQueryExamplePath   string = "../../../validation/protocol/examples/query"
	secPathTest                       = "../../../crypto/popcha.rsa"
	pubPathtest                       = "../../../crypto/popcha.rsa.pub"
)

// TestJWTToken creates a JWT token with arbitrary parameters, and parse it to assert its correctness.
func TestJWTToken(t *testing.T) {
	sk, pk, err := loadRSAKeys(secPathTest, pubPathtest)
	require.NoError(t, err)

	webAddr := "https://server.example.com"
	ppid := "ppid12564"
	cID := "cID122dw"
	nonce := "n0nc3"
	// creating the token
	idToken, err := createJWTString(webAddr, ppid, cID, nonce, sk)
	require.NoError(t, err)

	log.Info().Msg(idToken)
	// verifying the token
	token, err := jwt.Parse(idToken, func(jwtToken *jwt.Token) (interface{}, error) {
		_, ok := jwtToken.Method.(*jwt.SigningMethodRSA)
		require.True(t, ok)
		return pk, nil
	})

	claims, ok := token.Claims.(jwt.MapClaims)
	require.True(t, ok)
	//checking the parsing is correct
	require.NoError(t, err)
	require.True(t, token.Valid)
	require.Equal(t, claims["nonce"], nonce)
	require.NotEmptyf(t, claims["auth_time"], "Authentication time is nil")
	require.Equal(t, claims["aud"], cID)
	require.Equal(t, claims["iss"], webAddr)
	require.NotEmpty(t, claims["iat"], "Expiration time is nil")
	require.Equal(t, claims["sub"], ppid)

}

// TestURIParamSConstruction creates a fake authentication message, and assert
// whether the corresponding Redirect URI can be created without error.
func TestURIParamsConstruction(t *testing.T) {
	// creating a fake authorization message
	authMsg := &mauthentification.AuthenticateUser{
		Object:          channel.AuthObject,
		Action:          channel.AuthAction,
		ClientID:        "cl1ent",
		Nonce:           "n0nce",
		Identifier:      xid.New().String(),
		IdentifierProof: "pr00f",
		State:           "123state",
		ResponseMode:    "query",
		PopchaAddress:   "https://server.example.com",
	}
	// creating a fake oldchannel, we will not use it in this test
	c := NewChannel("", nil, zerolog.New(io.Discard), secPathTest, pubPathtest)
	_, err := constructRedirectURIParams(c, authMsg, authMsg.Nonce)
	require.NoError(t, err)
}

// Test_Authenticate_User test the correctness of the authentication oldchannel publish handling.
func Test_Authenticate_User(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := newFakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)
	name := "3hfd5xSty1VShCdcfLUDsgNF_EnMSRiFk74xvH5LRjM=/authenticate"
	// Create the oldchannel
	authCha := NewChannel("3hfd5xSty1VShCdcfLUDsgNF_EnMSRiFk74xvH5LRjM=/authenticate", fakeHub, logger.Logger, secPathTest, pubPathtest)

	fakeHub.RegisterNewChannel(name, authCha)
	_, found := fakeHub.channelByID[name]
	require.True(t, found)

	// Create the message
	file := filepath.Join(relativeMsgDataExamplePath, "popcha_authenticate",
		"popcha_authenticate.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	// oldchannel where the ws server signals when it has started listening
	msgCh := make(chan []byte, 1)
	// oldchannel where the ws server put message it received from the pop backend
	startCh := make(chan struct{}, 1)

	// creating dummy websocket server
	newWSServer(t, "localhost:19006", msgCh, startCh)

	// wait that the ws server has started listening
	<-startCh

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := mmessage.Message{
		Data:              buf64,
		Sender:            "OuAhDgVgD0M2PIMTs8wyqxkg7N_ScEQu87k35i4zCsg=",
		Signature:         "h",
		MessageID:         channel.Hash(buf64, "h"),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	relativePathCreatePub := filepath.Join(relativeQueryExamplePath, "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	// adding the pop token to the list of the latest roll call
	authCha.AddAttendee("OuAhDgVgD0M2PIMTs8wyqxkg7N_ScEQu87k35i4zCsg=")

	var msg method2.Publish

	err = json.Unmarshal(bufCreatePub, &msg)
	require.NoError(t, err)

	msg.Params.Message = m
	msg.Params.Channel = name

	err = authCha.Publish(msg, socket.ClientSocket{})
	require.NoError(t, err)

	select {
	case m := <-msgCh:
		require.NotNil(t, m)
	case <-time.After(time.Second):
		t.Errorf("No message received after 1 sec")
	}
}

// -----------------------------------------------------------------------------
// Utility functions

type keypair struct {
	public    kyber.Point
	publicBuf []byte
	private   kyber.Scalar
}

var nolog = zerolog.New(io.Discard)
var suite = crypto.Suite

func generateKeyPair(t *testing.T) keypair {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	pkbuf, err := point.MarshalBinary()
	require.NoError(t, err)

	return keypair{point, pkbuf, secret}
}

type fakeHub struct {
	messageChan chan socket.IncomingMessage

	sync.RWMutex
	channelByID map[string]oldchannel.Channel

	closedSockets chan string

	pubKeyOwner kyber.Point

	pubKeyServ kyber.Point
	secKeyServ kyber.Scalar

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	workers *semaphore.Weighted

	log zerolog.Logger

	laoFac oldchannel.LaoFactory
}

// newFakeHub returns a fake Hub.
func newFakeHub(publicOrg kyber.Point, log zerolog.Logger, laoFac oldchannel.LaoFactory) (*fakeHub, error) {

	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	pubServ, secServ := generateKeys()

	hub := fakeHub{
		messageChan:     make(chan socket.IncomingMessage),
		channelByID:     make(map[string]oldchannel.Channel),
		closedSockets:   make(chan string),
		pubKeyOwner:     publicOrg,
		pubKeyServ:      pubServ,
		secKeyServ:      secServ,
		schemaValidator: schemaValidator,
		stop:            make(chan struct{}),
		workers:         semaphore.NewWeighted(10),
		log:             log,
		laoFac:          laoFac,
	}

	return &hub, nil
}

func (h *fakeHub) RegisterNewChannel(channeID string, channel oldchannel.Channel) {
	h.Lock()
	h.channelByID[channeID] = channel
	h.Unlock()
}

func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	return point, secret
}

// GetPubKeyOwner implements oldchannel.HubFunctionalities
func (h *fakeHub) GetPubKeyOwner() kyber.Point {
	return h.pubKeyOwner
}

// GetPubKeyServ implements oldchannel.HubFunctionalities
func (h *fakeHub) GetPubKeyServ() kyber.Point {
	return h.pubKeyServ
}

// GetServerAddress implements oldchannel.HubFunctionalities
func (h *fakeHub) GetServerAddress() string {
	return ""
}

// GetClientServerAddress implements oldchannel.HubFunctionalities
func (h *fakeHub) GetClientServerAddress() string {
	return ""
}

// Sign implements oldchannel.HubFunctionalities
func (h *fakeHub) Sign(data []byte) ([]byte, error) {
	signatureBuf, err := schnorr.Sign(crypto.Suite, h.secKeyServ, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}
	return signatureBuf, nil
}

// NotifyWitnessMessage implements oldchannel.HubFunctionalities
func (h *fakeHub) NotifyWitnessMessage(_ string, _ string, _ string) {}

// GetPeersInfo implements oldchannel.HubFunctionalities
func (h *fakeHub) GetPeersInfo() []mgreetserver.GreetServerParams {
	return nil
}

func (h *fakeHub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

func (h *fakeHub) NotifyNewChannel(_ string, _ oldchannel.Channel, _ socket.Socket) {}

func (h *fakeHub) GetServerNumber() int {
	return 0
}

func (h *fakeHub) SendAndHandleMessage(_ mbroadcast.Broadcast) error {
	return nil
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

func websocketHandler(t *testing.T, msgCh chan []byte) func(http.ResponseWriter, *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		upgrader.CheckOrigin = func(r *http.Request) bool { return true }

		conn, err := upgrader.Upgrade(w, r, nil)
		require.NoError(t, err)

		// receive the message and send it to the message oldchannel
		_, msg, err := conn.ReadMessage()
		require.NoError(t, err)

		msgCh <- msg
		conn.Close()
	}
}

func newWSServer(t *testing.T, addr string, msgCh chan []byte, startCh chan struct{}) {
	http.HandleFunc("/", websocketHandler(t, msgCh))
	go func() {
		listener, err := net.Listen("tcp", addr)
		require.NoError(t, err)

		// signal that the ws server has started listening
		startCh <- struct{}{}

		err = http.Serve(listener, nil)
		require.NoError(t, err)
	}()
}
