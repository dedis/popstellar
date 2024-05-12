package federation

import (
	"bytes"
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"popstellar/channel"
	"popstellar/channel/registry"
	"popstellar/crypto"
	"popstellar/inbox"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"strconv"
	"sync"
	"time"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

const (
	msgID             = "msg id"
	invalidStateError = "Invalid state %v for message %v"
)

type State int

const (
	None State = iota + 1
	ExpectConnect
	Initiating
	WaitResult
	Connected
)

type remoteOrganization struct {
	laoId       string
	fedChannel  string
	organizerPk string

	// store the pop tokens of the other lao
	// popTokens map[string]struct{}

	challenge    messagedata.FederationChallenge
	challengeMsg message.Message
	socket       socket.Socket

	state State
	sync.Mutex
}

// Channel is used to handle federation messages.
type Channel struct {
	sockets   channel.Sockets
	inbox     *inbox.Inbox
	channelID string
	hub       channel.HubFunctionalities
	log       zerolog.Logger
	registry  registry.MessageRegistry

	localOrganizerPk string

	// map remoteOrganizerPk -> remoteOrganization
	remoteOrganizations map[string]*remoteOrganization

	// list of challenge requested but not used yet
	challenges map[messagedata.FederationChallenge]struct{}

	sync.Mutex
}

// NewChannel returns a new initialized federation channel
func NewChannel(channelID string, hub channel.HubFunctionalities,
	log zerolog.Logger, organizerPk string) channel.Channel {
	box := inbox.NewInbox(channelID)
	log = log.With().Str("channel", "federation").Logger()

	newChannel := &Channel{
		sockets:             channel.NewSockets(),
		inbox:               box,
		channelID:           channelID,
		hub:                 hub,
		log:                 log,
		localOrganizerPk:    organizerPk,
		remoteOrganizations: make(map[string]*remoteOrganization),
		challenges:          make(map[messagedata.FederationChallenge]struct{}),
	}

	newChannel.registry = newChannel.NewFederationRegistry()

	return newChannel
}

// Subscribe is used to handle a subscribe message from the client.
func (c *Channel) Subscribe(socket socket.Socket, msg method.Subscribe) error {
	c.log.Info().Str(msgID, strconv.Itoa(msg.ID)).Msg("received a subscribe")
	c.sockets.Upsert(socket)

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *Channel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	c.log.Info().Str(msgID, strconv.Itoa(msg.ID)).Msg("received an unsubscribe")

	ok := c.sockets.Delete(socketID)
	if !ok {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Publish is used to handle publish messages in the federation channel.
func (c *Channel) Publish(publish method.Publish, socket socket.Socket) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	err := c.verifyMessage(publish.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message: %v", err)
	}

	err = c.handleMessage(publish.Params.Message, socket)
	if err != nil {
		return xerrors.Errorf("failed to handle a publish message: %v", err)
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	c.log.Info().Str(msgID, strconv.Itoa(catchup.ID)).Msg("received a catchup")

	return c.inbox.GetSortedMessages()
}

// Broadcast is used to handle a broadcast message.
func (c *Channel) Broadcast(broadcast method.Broadcast, socket socket.Socket) error {
	return answer.NewInvalidActionError("broadcast is not supported")
}

// NewFederationRegistry creates a new registry for the federation channel
func (c *Channel) NewFederationRegistry() registry.MessageRegistry {
	fedRegistry := registry.NewMessageRegistry()

	fedRegistry.Register(messagedata.FederationChallengeRequest{}, c.processChallengeRequest)
	fedRegistry.Register(messagedata.FederationExpect{}, c.processFederationExpect)
	fedRegistry.Register(messagedata.FederationInit{}, c.processFederationInit)
	fedRegistry.Register(messagedata.FederationChallenge{}, c.processFederationChallenge)
	fedRegistry.Register(messagedata.FederationResult{}, c.processFederationResult)

	return fedRegistry
}

func (c *Channel) handleMessage(msg message.Message,
	socket socket.Socket) error {
	err := c.registry.Process(msg, socket)
	if err != nil {
		return xerrors.Errorf("failed to process message: %v", err)
	}

	return nil
}

// verifyMessage checks if a message in a Publish or Broadcast method is valid
func (c *Channel) verifyMessage(msg message.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	// Verify the data
	err = c.hub.GetSchemaValidator().VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return xerrors.Errorf("failed to verify json schema: %w", err)
	}

	// Check if the message already exists
	if _, ok := c.inbox.GetMessage(msg.MessageID); ok {
		return answer.NewError(-3, "message already exists")
	}

	return nil
}

func (c *Channel) processFederationInit(msg message.Message,
	msgData interface{}, _ socket.Socket) error {

	_, ok := msgData.(*messagedata.FederationInit)
	if !ok {
		return xerrors.Errorf("message %v is not a federation#init message",
			msgData)
	}

	// check if it is from the local organizer
	if c.localOrganizerPk != msg.Sender {
		return answer.NewAccessDeniedError(
			"Only local organizer is allowed to send federation#init")
	}

	var federationInit messagedata.FederationInit

	err := msg.UnmarshalData(&federationInit)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal FederationInit data: %v", err)
	}

	var federationChallenge messagedata.FederationChallenge
	err = federationInit.ChallengeMsg.UnmarshalData(&federationChallenge)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal FederationChallenge data: %v", err)
	}

	remoteOrg := c.getRemoteOrganization(federationInit.PublicKey)
	remoteOrg.Lock()
	defer remoteOrg.Unlock()

	if remoteOrg.state != None {
		return answer.NewInternalServerError(invalidStateError, remoteOrg.state, msg)
	}

	remoteOrg.state = Initiating
	remoteOrg.organizerPk = federationInit.PublicKey
	remoteOrg.laoId = federationInit.LaoId
	remoteOrg.fedChannel = fmt.Sprintf("/root/%s/federation", federationInit.LaoId)
	remoteOrg.challenge = federationChallenge

	remoteOrg.socket, err = c.hub.ConnectToServerAsClient(federationInit.ServerAddress)
	if err != nil {
		remoteOrg.state = None
		return answer.NewInternalServerError(
			"failed to connect to server %v: %v",
			federationInit.ServerAddress, err)
	}

	// send the challenge to the other server
	challengePublish := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: remoteOrg.fedChannel,
			Message: federationInit.ChallengeMsg,
		},
	}

	buf, err := json.Marshal(challengePublish)
	if err != nil {
		remoteOrg.state = None
		return xerrors.Errorf("failed to marshal challenge: %v", err)
	}

	remoteOrg.socket.Send(buf)
	remoteOrg.state = WaitResult

	return nil
}

func (c *Channel) processFederationExpect(msg message.Message,
	msgData interface{}, _ socket.Socket) error {

	_, ok := msgData.(*messagedata.FederationExpect)
	if !ok {
		return xerrors.Errorf("message %v is not a federation#expect message",
			msgData)
	}

	// check if it is from the local organizer
	if c.localOrganizerPk != msg.Sender {
		return answer.NewAccessDeniedError(
			"Only local organizer is allowed to send federation#expect")
	}

	var federationExpect messagedata.FederationExpect

	err := msg.UnmarshalData(&federationExpect)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal federationExpect data: %v", err)
	}
	remoteOrg := c.getRemoteOrganization(federationExpect.PublicKey)
	remoteOrg.Lock()
	defer remoteOrg.Unlock()

	if remoteOrg.state != None {
		return answer.NewInternalServerError(invalidStateError, remoteOrg.state, msg)
	}
	var federationChallenge messagedata.FederationChallenge
	err = federationExpect.ChallengeMsg.UnmarshalData(&federationChallenge)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal federationChallenge data: %v", err)
	}

	c.Lock()
	_, ok = c.challenges[federationChallenge]
	// always remove the challenge, if present, to avoid challenge reuse
	delete(c.challenges, federationChallenge)
	c.Unlock()

	if !ok {
		return answer.NewAccessDeniedError("Invalid challenge %v",
			federationChallenge)
	}

	remoteOrg.state = ExpectConnect
	remoteOrg.challenge = federationChallenge
	remoteOrg.organizerPk = federationExpect.PublicKey
	remoteOrg.laoId = federationExpect.LaoId
	remoteOrg.fedChannel = fmt.Sprintf("/root/%s/federation", federationExpect.LaoId)
	remoteOrg.challengeMsg = federationExpect.ChallengeMsg

	return nil
}

func (c *Channel) processFederationChallenge(msg message.Message,
	msgData interface{}, s socket.Socket) error {

	_, ok := msgData.(*messagedata.FederationChallenge)
	if !ok {
		return xerrors.Errorf(
			"message %v is not a federation#challenge message", msgData)
	}

	var federationChallenge messagedata.FederationChallenge

	err := msg.UnmarshalData(&federationChallenge)
	if err != nil {
		return xerrors.Errorf(
			"failed to unmarshal federationChallenge data: %v", err)
	}

	// If not present, no FederationExpect was received for this organizer pk
	remoteOrg, ok := c.remoteOrganizations[msg.Sender]
	if !ok {
		return answer.NewAccessDeniedError("Unexpected challenge")
	}
	remoteOrg.Lock()
	defer remoteOrg.Unlock()

	// check if it is from the remote organizer
	if remoteOrg.organizerPk != msg.Sender {
		return answer.NewAccessDeniedError(
			"Only remote organizer is allowed to send federation#challenge")
	}

	if remoteOrg.state != ExpectConnect {
		return answer.NewInternalServerError(invalidStateError, remoteOrg, msg)
	}

	if remoteOrg.challenge.Value != federationChallenge.Value {
		return answer.NewAccessDeniedError("Invalid challenge %v",
			federationChallenge.Value)
	}

	if remoteOrg.challenge.ValidUntil < time.Now().Unix() {
		return answer.NewAccessDeniedError("This challenge has expired: %v",
			federationChallenge)
	}

	remoteOrg.state = WaitResult
	remoteOrg.socket = s

	federationResultData := messagedata.FederationResult{
		Object:       messagedata.FederationObject,
		Action:       messagedata.FederationActionResult,
		Status:       "success",
		PublicKey:    remoteOrg.organizerPk,
		ChallengeMsg: remoteOrg.challengeMsg,
	}

	dataBytes, err := json.Marshal(federationResultData)
	if err != nil {
		return xerrors.Errorf("failed to marshal federation result message data: %v", err)
	}

	dataBase64 := base64.URLEncoding.EncodeToString(dataBytes)
	signatureBytes, err := c.hub.Sign(dataBytes)
	if err != nil {
		return xerrors.Errorf("failed to sign federation result message: %v", err)
	}
	signatureBase64 := base64.URLEncoding.EncodeToString(signatureBytes)

	serverPubKey, err := c.hub.GetPubKeyServ().MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal public key of server: %v", err)

	}

	federationResultMsg := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubKey),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	rpcMessage := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: remoteOrg.fedChannel,
			Message: federationResultMsg,
		},
	}
	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		return xerrors.Errorf("failed to marshal publish query: %v", err)
	}

	remoteOrg.socket.Send(buf)

	return nil
}

func (c *Channel) processChallengeRequest(msg message.Message,
	msgData interface{}, s socket.Socket) error {

	_, ok := msgData.(*messagedata.FederationChallengeRequest)
	if !ok {
		return xerrors.Errorf(
			"message %v is not a federation#challenge_request message", msgData)
	}

	// check if it is from the local organizer
	if c.localOrganizerPk != msg.Sender {
		return answer.NewAccessDeniedError(
			"Only local organizer is allowed to send federation#challenge_request")
	}

	var federationChallengeRequest messagedata.FederationChallengeRequest

	err := msg.UnmarshalData(&federationChallengeRequest)
	if err != nil {
		return xerrors.Errorf(
			"failed to unmarshal federationChallengeRequest data: %v", err)
	}

	randomBytes := make([]byte, 32)
	_, err = rand.Read(randomBytes)
	if err != nil {
		return answer.NewInternalServerError("Failed to generate random bytes: %v", err)
	}
	challengeValue := hex.EncodeToString(randomBytes)
	expirationTime := time.Now().Add(time.Minute * 5).Unix()
	federationChallenge := messagedata.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      challengeValue,
		ValidUntil: expirationTime,
	}

	c.Lock()
	c.challenges[federationChallenge] = struct{}{}
	c.Unlock()

	challengeData, err := json.Marshal(federationChallenge)
	if err != nil {
		return xerrors.Errorf(
			"failed to marshal federationChallenge data: %v", err)
	}
	data := base64.URLEncoding.EncodeToString(challengeData)

	senderBytes, err := c.hub.GetPubKeyServ().MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal server public key: %v", err)
	}
	sender := base64.URLEncoding.EncodeToString(senderBytes)

	signatureBytes, err := c.hub.Sign(challengeData)
	if err != nil {
		return xerrors.Errorf("failed to sign message: %v", err)
	}
	signature := base64.URLEncoding.EncodeToString(signatureBytes)

	challengeMsg := message.Message{
		Data:              data,
		Sender:            sender,
		Signature:         signature,
		MessageID:         messagedata.Hash(data, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	rpcMessage := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "broadcast",
		},
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			c.channelID,
			challengeMsg,
		},
	}

	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		return xerrors.Errorf("failed to marshal broadcast query: %v", err)
	}

	// send back the challenge directly to the organizer only
	s.Send(buf)

	return nil
}

func (c *Channel) processFederationResult(msg message.Message,
	msgData interface{}, s socket.Socket) error {
	_, ok := msgData.(*messagedata.FederationResult)
	if !ok {
		return xerrors.Errorf("message %v is not a federation#result message",
			msgData)
	}

	var federationResult messagedata.FederationResult

	err := msg.UnmarshalData(&federationResult)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal FederationResult data: %v", err)
	}

	if federationResult.Status != "success" {
		if len(federationResult.Reason) > 0 {
			return xerrors.Errorf("failed to establish federated connection: %v", federationResult.Reason)
		}
		return xerrors.Errorf("failed to establish federated connection")
	}

	var federationChallenge messagedata.FederationChallenge
	err = federationResult.ChallengeMsg.UnmarshalData(&federationChallenge)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal challenge from FederationResult data: %v", err)
	}

	challengeDataBytes, err := base64.URLEncoding.DecodeString(federationResult.ChallengeMsg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode challenge data in FederationResult: %v", err)

	}

	challengeSignatureBytes, err := base64.URLEncoding.DecodeString(federationResult.ChallengeMsg.Signature)
	if err != nil {
		return xerrors.Errorf("failed to decode challenge signature in FederationResult: %v", err)

	}

	remoteOrg := c.getRemoteOrganization(federationResult.ChallengeMsg.Sender)
	remoteOrg.Lock()
	defer remoteOrg.Unlock()

	if remoteOrg.state != WaitResult {
		return answer.NewInternalServerError(invalidStateError, remoteOrg.state, msg)

	}

	pkBytes, err := base64.URLEncoding.DecodeString(remoteOrg.organizerPk)
	if err != nil {
		return xerrors.Errorf("failed to decode remote organizers public key: %v", err)

	}

	remotePk := crypto.Suite.Point()

	err = remotePk.UnmarshalBinary(pkBytes)
	if err != nil {
		return xerrors.Errorf("failed to decode remote organizers public key: %v", err)

	}
	err = schnorr.Verify(crypto.Suite, remotePk, challengeDataBytes, challengeSignatureBytes)
	if err != nil {
		return xerrors.Errorf("failed to verify signature on challenge in FederationResult message: %v", err)

	}

	resultPkBytes, err := base64.URLEncoding.DecodeString(federationResult.PublicKey)
	if err != nil {
		return xerrors.Errorf("failed to decode local public key in FederationResult message: %v", err)

	}
	localPkBytes, err := c.hub.GetPubKeyOwner().MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal local organizer public key: %v", err)

	}
	if !(bytes.Equal(resultPkBytes, localPkBytes)) {
		return xerrors.Errorf("invalid public key contained in FederationResult message")

	}

	//err = schnorr.Verify(crypto.Suite, remotePk, localPkBinary, pkSignatureBytes)
	//if err != nil {
	//	return xerrors.Errorf("failed to verify remote signature on local organizer public key: %v", err)

	//}

	remoteOrg.state = Connected

	return nil
}

// getRemoteOrganization get the remoteOrganization for the given organizerPk
// or return a new empty one.
func (c *Channel) getRemoteOrganization(organizerPk string) *remoteOrganization {
	c.Lock()
	defer c.Unlock()

	org, ok := c.remoteOrganizations[organizerPk]
	if !ok {
		org = &remoteOrganization{state: None}
		c.remoteOrganizations[organizerPk] = org
	}

	return org
}
