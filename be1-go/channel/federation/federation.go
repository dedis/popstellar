package federation

import (
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
	"popstellar/channel"
	"popstellar/channel/registry"
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
	"time"
)

const (
	msgID = "msg id"
)

type State int

const (
	None State = iota + 1
	ExpectConnect
	Initiating
	WaitResult
	Connected
)

// Channel is used to handle federation messages.
type Channel struct {
	sockets   channel.Sockets
	inbox     *inbox.Inbox
	channelID string
	hub       channel.HubFunctionalities
	log       zerolog.Logger
	registry  registry.MessageRegistry

	localOrganizerPk  string
	remoteOrganizerPk string

	remoteChannel string
	remoteServer  socket.Socket

	challenge messagedata.Challenge

	state State
}

// NewChannel returns a new initialized federation channel
func NewChannel(channelID string, hub channel.HubFunctionalities,
	log zerolog.Logger, organizerPk string) channel.Channel {
	box := inbox.NewInbox(channelID)
	log = log.With().Str("channel", "federation").Logger()

	newChannel := &Channel{
		sockets:          channel.NewSockets(),
		inbox:            box,
		channelID:        channelID,
		hub:              hub,
		log:              log,
		localOrganizerPk: organizerPk,
		state:            None,
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
	registry := registry.NewMessageRegistry()

	registry.Register(messagedata.FederationRequestChallenge{}, c.processChallengeRequest)
	registry.Register(messagedata.FederationExpect{}, c.processFederationExpect)
	registry.Register(messagedata.FederationInit{}, c.processFederationInit)
	registry.Register(messagedata.FederationChallenge{}, c.processFederationChallenge)

	return registry
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
	msgData interface{}, s socket.Socket) error {

	_, ok := msgData.(messagedata.FederationInit)
	if !ok {
		return xerrors.Errorf("message %v is not a federation#init message",
			msgData)
	}

	// check if it is from the local organizer
	if c.localOrganizerPk != msg.Sender {
		return answer.NewAccessDeniedError(
			"Only local organizer is allowed to send federation#init")
	}

	if c.state != None {
		return answer.NewInternalServerError("The current state is %v", c.state)
	}
	c.state = Initiating

	var federationInit messagedata.FederationInit

	err := msg.UnmarshalData(&federationInit)
	if err != nil {
		c.state = None
		return xerrors.Errorf("failed to unmarshal FederationInit data: %v", err)
	}

	c.remoteServer, err = c.hub.ConnectToServerAsClient(federationInit.ServerAddress)
	if err != nil {
		c.state = None
		return answer.NewInternalServerError(
			"failed to connect to server %v: %v",
			federationInit.ServerAddress, err)
	}

	// send the challenge to the other server
	c.remoteChannel = fmt.Sprintf("/root/%s/federation", federationInit.LaoId)

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
			Channel: c.remoteChannel,
			Message: federationInit.ChallengeMsg,
		},
	}

	buf, err := json.Marshal(challengePublish)
	if err != nil {
		c.state = None
		return xerrors.Errorf("failed to marshal challenge: %v", err)
	}

	c.remoteServer.Send(buf)

	return nil
}

func (c *Channel) processFederationExpect(msg message.Message,
	msgData interface{}, s socket.Socket) error {

	_, ok := msgData.(messagedata.FederationExpect)
	if !ok {
		return xerrors.Errorf("message %v is not a federation#expect message",
			msgData)
	}

	// check if it is from the local organizer
	if c.localOrganizerPk != msg.Sender {
		return answer.NewAccessDeniedError(
			"Only local organizer is allowed to send federation#expect")
	}

	if c.state != None {
		return answer.NewInternalServerError("The current state is %v", c.state)
	}

	var federationExpect messagedata.FederationExpect

	err := msg.UnmarshalData(&federationExpect)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal federationExpect data: %v", err)
	}

	c.state = ExpectConnect
	c.remoteOrganizerPk = federationExpect.PublicKey
	c.remoteChannel = fmt.Sprintf("/root/%s/federation", federationExpect.LaoId)

	return nil
}

func (c *Channel) processFederationChallenge(msg message.Message,
	msgData interface{}, s socket.Socket) error {

	_, ok := msgData.(messagedata.FederationChallenge)
	if !ok {
		return xerrors.Errorf(
			"message %v is not a federation#challenge message", msgData)
	}

	// check if it is from the remote organizer
	if c.remoteOrganizerPk != msg.Sender {
		return answer.NewAccessDeniedError(
			"Only remote organizer is allowed to send federation#challenge")
	}

	if c.state != ExpectConnect {
		return answer.NewInternalServerError("The current state is %v", c.state)
	}

	var federationChallenge messagedata.FederationChallenge

	err := msg.UnmarshalData(&federationChallenge)
	if err != nil {
		return xerrors.Errorf(
			"failed to unmarshal federationChallenge data: %v", err)
	}

	if c.challenge.Value != federationChallenge.Value {
		return answer.NewAccessDeniedError("Invalid challenge %v",
			federationChallenge.Value)
	}

	if c.challenge.ValidUntil < time.Now().Unix() {
		return answer.NewAccessDeniedError("This challenge has expired: %v",
			federationChallenge)
	}

	c.state = Connected
	c.remoteServer = s
	// Send Federation result to S1
	// c.remoteServer.Send(...)

	return nil
}

func (c *Channel) processChallengeRequest(msg message.Message,
	msgData interface{}, s socket.Socket) error {

	_, ok := msgData.(messagedata.FederationRequestChallenge)
	if !ok {
		return xerrors.Errorf(
			"message %v is not a federation#request_challenge message", msgData)
	}

	// check if it is from the local organizer
	if c.localOrganizerPk != msg.Sender {
		return answer.NewAccessDeniedError(
			"Only local organizer is allowed to send federation#request_challenge")
	}

	if c.state != None && c.state != ExpectConnect {
		return answer.NewInternalServerError("The current state is %v", c.state)
	}

	var federationRequestChallenge messagedata.FederationRequestChallenge

	err := msg.UnmarshalData(&federationRequestChallenge)
	if err != nil {
		return xerrors.Errorf(
			"failed to unmarshal federationRequestChallenge data: %v", err)
	}

	randomBytes := make([]byte, 32)
	_, err = rand.Read(randomBytes)
	if err != nil {
		return answer.NewInternalServerError("Failed to generate random bytes: %v", err)
	}
	challengeValue := hex.EncodeToString(randomBytes)
	expirationTime := time.Now().Add(time.Minute * 5).Unix()

	c.challenge = messagedata.Challenge{
		Value:      challengeValue,
		ValidUntil: expirationTime,
	}
	c.state = None

	federationChallenge := messagedata.FederationChallenge{
		Object:    "federation",
		Action:    "challenge",
		Value:     c.challenge.Value,
		Timestamp: c.challenge.ValidUntil,
	}

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
