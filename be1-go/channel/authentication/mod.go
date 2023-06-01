package authentication

import (
	"crypto/rsa"
	"encoding/base64"
	"github.com/golang-jwt/jwt/v4"
	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
	"net/url"
	"os"
	"popstellar/channel"
	"popstellar/channel/registry"
	"popstellar/crypto"
	"popstellar/inbox"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"strconv"
	"strings"
	"sync"
	"time"
)

const (
	msgID              = "msg id"
	failedToDecodeData = "failed to decode message data: %v"

	//expiration time for the ID Token
	expireDuration = time.Hour

	// type of token in the popcha protocol
	tokenBearer = "bearer"
)

// opaque string referencing a pop token for a given client ID
type identifier string

// ID of the client the user is trying to authenticate with
type clientID string

// Channel is used to handle authentication messages.
type Channel struct {
	sockets channel.Sockets
	inbox   *inbox.Inbox

	// channel path lao_id/authentication
	channelID string

	// clientID to pop token to identifier map
	popToID map[clientID]map[string]identifier

	popIDLock *sync.Mutex

	// mapping of pairwise identifiers, as described by the Pairwise Pseudonymous Identifier specifications of OpenID
	// connect core (§17.3 Correlation). The key is the pseudonymous identifier.
	ppidIdentifier map[identifier]identifier

	ppidLock *sync.Mutex

	// path to secret key
	skPath string
	// path to public key
	pkPath string

	latestRollCallMembers *rollCallMembers
	hub                   channel.HubFunctionalities
	log                   zerolog.Logger
	registry              registry.MessageRegistry
}

// NewChannel returns a new initialized authentication channel. This channel is used to transmit the
// authentication messages between the PoP App and the Back-end.
func NewChannel(channelPath string, hub channel.HubFunctionalities, log zerolog.Logger, secretKeyPath string, publicKeyPath string) *Channel {

	log = log.With().Str("channel", "authentication").Logger()
	newChannel := &Channel{
		sockets:               channel.NewSockets(),
		inbox:                 inbox.NewInbox(channelPath),
		channelID:             channelPath,
		popToID:               make(map[clientID]map[string]identifier),
		popIDLock:             &sync.Mutex{},
		ppidIdentifier:        make(map[identifier]identifier),
		ppidLock:              &sync.Mutex{},
		skPath:                secretKeyPath,
		pkPath:                publicKeyPath,
		latestRollCallMembers: newRollCallSet(),
		hub:                   hub,
		log:                   log,
	}

	newChannel.registry = newChannel.NewAuthenticationRegistry()

	return newChannel
}

// ---
// Publish-subscribe / channel.Channel implementation
// ---

// Subscribe : for authentication messages, we explicitly forbid subscription from clients.
func (c *Channel) Subscribe(_ socket.Socket, _ method.Subscribe) error {
	return xerrors.New("It is not possible to subscribe to the authentication channel.")
}

// Unsubscribe is also not usable in that context, as clients can't use Subscribe
func (c *Channel) Unsubscribe(_ string, _ method.Unsubscribe) error {
	return xerrors.New("It is not possible to unsubscribe from the authentication channel.")
}

// Publish is used to handle publish messages in the authentication channel.
func (c *Channel) Publish(publish method.Publish, socket socket.Socket) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	err := c.verifyMessage(publish.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on a "+
			"authentication channel: %w", err)
	}

	err = c.handleMessage(publish.Params.Message, socket)
	if err != nil {
		return xerrors.Errorf("failed to handle publish message: %v", err)
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	c.log.Error().Msg("Catchup is not allowed on the authentication channel")
	return nil
}

// Broadcast is forbidden, as authentication messages must be kept secret.
func (c *Channel) Broadcast(_ method.Broadcast, _ socket.Socket) error {
	return xerrors.New("Broadcasting is not allowed on the authentication channel")
}

// ---
// Message handling
// ---

// handleMessage handles a message received in a broadcast or publish method
func (c *Channel) handleMessage(msg message.Message, socket socket.Socket) error {
	err := c.registry.Process(msg, socket)
	if err != nil {
		return xerrors.Errorf("failed to process message: %w", err)
	}

	c.inbox.StoreMessage(msg)

	return nil
}

// NewAuthenticationRegistry creates a new registry for an authentication channel
func (c *Channel) NewAuthenticationRegistry() registry.MessageRegistry {
	newRegistry := registry.NewMessageRegistry()
	newRegistry.Register(messagedata.AuthenticateUser{}, c.auhenticateUser)
	return newRegistry
}

func (c *Channel) auhenticateUser(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.AuthenticateUser)
	if !ok {
		return xerrors.Errorf("message %v isn't a authentication message", msgData)
	}

	// verify signatures and validity of the pop Token.
	err := c.verifyAuthMessage(msg, *data)
	if err != nil {
		return xerrors.Errorf("failed to verify authentication message: %v", err)
	}

	if !c.latestRollCallMembers.isPresent(msg.Sender) {
		return xerrors.Errorf("Error while validating the authentication message: pop token is not part of the latest roll call")
	}

	encodedClientParams, err := constructRedirectURIParams(c, data)
	if err != nil {
		return xerrors.Errorf("Error while constructing the redirect URI parameters: %v", err)
	}

	// constructing the unique URL endpoint of the PoPCHA server

	laoID := strings.TrimPrefix(c.channelID, "/root/")
	popChaPath := strings.Join([]string{"/response", laoID, "v4l1d_ient_id", data.Nonce}, "/")

	popchaAddress := data.PopchaAddress
	if strings.HasPrefix(popchaAddress, "http://") {
		popchaAddr, err := url.Parse(data.PopchaAddress)
		if err != nil {
			return xerrors.Errorf("Error while parsing the address of the authorization server: %v", err)
		}
		popchaAddress = popchaAddr.Host
	}

	//  ws://popcha.example/lao_id/client_id/nonce
	popChaWsURL := url.URL{Scheme: "ws", Host: popchaAddress, Path: popChaPath}

	c.log.Info().Msgf("Sending the parameters to the webpage websocket %s", popChaWsURL.String())

	// instantiate connection with the popcha authorization server
	conn, _, err := websocket.DefaultDialer.Dial(popChaWsURL.String(), nil)
	if err != nil {
		return xerrors.Errorf("fail to instantiate a connection with the popcha webserver endpoint: %v", err)
	}

	// send the parameters of the query
	err = conn.WriteMessage(websocket.TextMessage, []byte(encodedClientParams))
	if err != nil {
		return xerrors.Errorf("fail to send the Authentication Response back to the popcha server: %v", err)
	}

	return conn.Close()
}

/*
*
*
*		HELPER METHODS
*
*
 */

// loadRSAKeys reads the rsa key-pair files into variables
func loadRSAKeys(privateKeyPath string, publicKeyPath string) (*rsa.PrivateKey, *rsa.PublicKey, error) {
	privBytes, err := os.ReadFile(privateKeyPath)
	if err != nil {
		return nil, nil, err
	}
	privKey, err := jwt.ParseRSAPrivateKeyFromPEM(privBytes)
	if err != nil {
		return nil, nil, err
	}

	pubBytes, err := os.ReadFile(publicKeyPath)
	if err != nil {
		return nil, nil, err
	}
	pubKey, err := jwt.ParseRSAPublicKeyFromPEM(pubBytes)
	if err != nil {
		return nil, nil, err
	}
	return privKey, pubKey, nil
}

// constructRedirectURIParams computes the redirect URI given the authentication message
func constructRedirectURIParams(c *Channel, data *messagedata.AuthenticateUser) (string, error) {

	c.log.Info().Msg("Constructing the URI Parameters")

	sk, _, err := loadRSAKeys(c.skPath, c.pkPath)
	if err != nil {
		return "", xerrors.Errorf("error while parsing RSA keys: %v", err)
	}

	// create ppid for the identifier
	ppid := base64.URLEncoding.EncodeToString([]byte(messagedata.Hash(data.Identifier)))

	// add the ppid entry for tracking the given identifier
	c.addPPIDEntry(identifier(data.Identifier), identifier(ppid))

	c.log.Info().Msg("Signing the JWT Token")
	idTokenString, err := createJWTString(data.PopchaAddress, ppid, data.ClientID, data.Nonce, sk)
	if err != nil {
		c.log.Err(err).Msg("Error while creating the JWT token")
		return "", xerrors.Errorf("Error while creating JWT token: %v", err)
	}

	state := data.State
	// create JWT token
	paramsRedirect := url.Values{}
	paramsRedirect.Add("token_type", tokenBearer)
	paramsRedirect.Add("id_token", idTokenString)
	paramsRedirect.Add("state", state)
	return paramsRedirect.Encode(), nil
}

// createJWTString creates and signs a json web token given multiple parameters, including a private key.
func createJWTString(webAddr string, ppid string, cID string, nonce string, sk *rsa.PrivateKey) (string, error) {
	// create the claims from the data of published by the PoP App

	claims := jwt.MapClaims{
		"iss":       webAddr,
		"sub":       ppid,
		"aud":       cID,
		"exp":       &jwt.NumericDate{Time: time.Now().Add(expireDuration)},
		"iat":       &jwt.NumericDate{Time: time.Now()},
		"nonce":     nonce,
		"auth_time": &jwt.NumericDate{Time: time.Now()},
	}

	// creating the token
	idToken := jwt.NewWithClaims(jwt.SigningMethodRS256, claims)

	// signing the token
	idTokenString, err := idToken.SignedString(sk)
	if err != nil {
		return "", err
	}
	return idTokenString, nil
}

// verifyMessage checks if a message in a Publish or Broadcast method is valid
func (c *Channel) verifyMessage(msg message.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf(failedToDecodeData, err)
	}

	// Verify the data
	err = c.hub.GetSchemaValidator().VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return xerrors.Errorf("failed to verify json schema: %w", err)
	}

	// Check if the message already exists
	_, ok := c.inbox.GetMessage(msg.MessageID)
	if ok {
		return answer.NewError(-3, "message already exists")
	}

	return nil
}

// verifyAuthMessage checks that the authentication message has valid identifier, signatures and format
func (c *Channel) verifyAuthMessage(msg message.Message, authMsg messagedata.AuthenticateUser) error {

	// checks that the identifier is not already associated with a (pop token, clientID) pair.
	err := c.checkIdentifier(clientID(authMsg.ClientID), msg.Sender, identifier(authMsg.Identifier))
	if err != nil {
		return xerrors.Errorf("invalid identifier: %v", err)
	}
	// verify base64 encoding and other core aspects of the message data
	err = authMsg.Verify()
	if err != nil {
		return xerrors.Errorf("invalid authentication message: %v", err)
	}

	// pop token
	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	// pop token into public key
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewError(-4, "invalid sender public key")
	}

	return nil
}

// maps methods

// checkIdentifier asserts that a given (cID, popTken,id) triplet is unique
func (c *Channel) checkIdentifier(cID clientID, popToken string, id identifier) error {
	c.popIDLock.Lock()
	defer c.popIDLock.Unlock()
	// check if there are tokens with the given clientID
	_, ok := c.popToID[cID]

	if !ok {
		c.popToID[cID] = make(map[string]identifier)
	} else {
		// check if for the given clientId and token, there is an identifier
		ide, ok := c.popToID[cID][popToken]
		if ok && ide != id {
			return xerrors.New("Error while verifying authentication message: identifier already existing")
		}
	}
	// if the identifier has not yet been used, assign it to the given clientID / popToken pair.
	c.popToID[cID][popToken] = id
	return nil
}

// addPPIDEntry adds a pairwise-identifier in the map
func (c *Channel) addPPIDEntry(id identifier, ppid identifier) {
	c.ppidLock.Lock()
	defer c.ppidLock.Unlock()
	c.ppidIdentifier[ppid] = id
}

// LaoFunctionalities methods

// AddAttendee adds an attendee to the reaction channel.
func (c *Channel) AddAttendee(key string) {
	c.latestRollCallMembers.add(key)
}

// rollCallMembers denotes the set of attendees of the latest roll-call
type rollCallMembers struct {
	sync.Mutex
	membersSet map[string]struct{}
}

// newRollCallSet returns a new instance of attendees.
func newRollCallSet() *rollCallMembers {
	return &rollCallMembers{
		membersSet: make(map[string]struct{}),
	}
}

// isPresent checks if a key representing a user is present in
// the list of attendees.
func (mb *rollCallMembers) isPresent(key string) bool {
	mb.Lock()
	defer mb.Unlock()

	_, ok := mb.membersSet[key]

	return ok
}

// add adds a member of the latest roll-call
func (mb *rollCallMembers) add(key string) {
	mb.Lock()
	defer mb.Unlock()

	mb.membersSet[key] = struct{}{}
}
