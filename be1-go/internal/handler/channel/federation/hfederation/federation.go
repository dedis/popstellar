package hfederation

import (
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/federation/mfederation"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/publish/mpublish"
	"popstellar/internal/handler/query/mquery"
	"popstellar/internal/network/socket"
	"popstellar/internal/validation"
	"strings"
	"sync"
	"time"
)

const (
	channelPattern = "/root/%s/federation"
)

type Hub interface {
	GetMessageChan() chan socket.IncomingMessage
	GetClosedSockets() chan string
	GetWaitGroup() *sync.WaitGroup
	GetStopChan() chan struct{}
}

type Subscribers interface {
	BroadcastToAllClients(msg mmessage.Message, channel string) error
	AddChannel(channel string) error
	Subscribe(channel string, socket socket.Socket) error
	SendToAll(buf []byte, channel string) error
}

type Sockets interface {
	Upsert(socket socket.Socket)
}

type Config interface {
	GetServerInfo() (string, string, string, error)
}

type RumorStateSender interface {
	SendRumorStateTo(socket socket.Socket) error
}

type GreetServerSender interface {
	SendGreetServer(socket socket.Socket) error
}

type Repository interface {
	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)

	// GetOrganizerPubKey returns the organizer public key of a LAO.
	GetOrganizerPubKey(laoID string) (kyber.Point, error)

	// IsChallengeValid returns true if the challenge is valid and not used yet
	IsChallengeValid(senderPk string, challenge mfederation.FederationChallenge, channelPath string) error

	// RemoveChallenge removes the challenge from the database to avoid reuse
	RemoveChallenge(challenge mfederation.FederationChallenge) error

	// GetFederationExpect return a FederationExpect where the organizer is
	// the given public keys
	GetFederationExpect(senderPk string, remotePk string, Challenge mfederation.FederationChallenge, channelPath string) (mfederation.FederationExpect, error)

	// GetFederationInit return a FederationExpect where the organizer is
	// the given public keys
	GetFederationInit(senderPk string, remotePk string, Challenge mfederation.FederationChallenge, channelPath string) (mfederation.FederationInit, error)

	// GetServerKeys get the keys of the server
	GetServerKeys() (kyber.Point, kyber.Scalar, error)

	// StoreMessageAndData stores a message with an object and an action inside the database.
	StoreMessageAndData(channelID string, msg mmessage.Message) error
}

type Handler struct {
	hub     Hub
	subs    Subscribers
	sockets Sockets
	conf    Config
	db      Repository
	rumors  RumorStateSender
	greets  GreetServerSender
	schema  *validation.SchemaValidator
	log     zerolog.Logger
}

func New(hub Hub, subs Subscribers, sockets Sockets, conf Config,
	db Repository, rumors RumorStateSender, greets GreetServerSender,
	schema *validation.SchemaValidator, log zerolog.Logger) *Handler {
	return &Handler{
		hub:     hub,
		subs:    subs,
		sockets: sockets,
		conf:    conf,
		db:      db,
		rumors:  rumors,
		greets:  greets,
		schema:  schema,
		log:     log.With().Str("module", "federation").Logger(),
	}
}

func (h *Handler) Handle(channelPath string, msg mmessage.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = h.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := channel.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	if object != channel.FederationObject {
		return errors.NewInvalidMessageFieldError("invalid object %v", object)
	}

	if action != channel.FederationActionTokensExchange {
		return errors.NewInvalidActionError("invalid action %v", action)
	}

	return h.handleTokensExchange(msg, channelPath)
}

func (h *Handler) HandleWithSocket(channelPath string, msg mmessage.Message,
	socket socket.Socket) error {
	err := msg.VerifyMessage()
	if err != nil {
		return err
	}

	alreadyExist, err := h.db.HasMessage(msg.MessageID)
	if err != nil {
		return err
	}

	if alreadyExist {
		return nil
	}

	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = h.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := channel.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	if object != channel.FederationObject {
		return errors.NewInvalidMessageFieldError("invalid object %v", object)
	}

	switch action {
	case channel.FederationActionChallengeRequest:
		err = h.handleRequestChallenge(msg, channelPath)
	case channel.FederationActionInit:
		err = h.handleInit(msg, channelPath)
	case channel.FederationActionExpect:
		err = h.handleExpect(msg, channelPath)
	case channel.FederationActionChallenge:
		err = h.handleChallenge(msg, channelPath, socket)
	case channel.FederationActionResult:
		err = h.handleResult(msg, channelPath)
	case channel.FederationActionTokensExchange:
		err = h.handleTokensExchange(msg, channelPath)
	default:
		err = errors.NewInvalidMessageFieldError("failed to Handle %s#%s, invalid object#action", object, action)
	}

	return err
}

// handleRequestChallenge expects the sender to be the organizer of the lao,
// a challenge message is then stored and broadcast on the same channelPath.
// The FederationChallengeRequest message is neither stored nor broadcast
func (h *Handler) handleRequestChallenge(msg mmessage.Message, channelPath string) error {
	var requestChallenge mfederation.FederationChallengeRequest
	err := msg.UnmarshalData(&requestChallenge)
	if err != nil {
		return err
	}

	err = h.verifyLocalOrganizer(msg, channelPath)
	if err != nil {
		return err
	}

	randomBytes := make([]byte, 32)
	_, err = rand.Read(randomBytes)
	if err != nil {
		return errors.NewInternalServerError("Failed to generate random bytes: %v", err)
	}

	challengeValue := hex.EncodeToString(randomBytes)
	expirationTime := time.Now().Add(time.Minute * 5).Unix()
	federationChallenge := mfederation.FederationChallenge{
		Object:     channel.FederationObject,
		Action:     channel.FederationActionChallenge,
		Value:      challengeValue,
		ValidUntil: expirationTime,
	}

	// The challenge sent to the organizer is signed by the server but should
	// not be confused with the challenge that will be signed by the organizer
	challengeMsg, err := h.createMessage(federationChallenge)
	if err != nil {
		return err
	}

	// store the generated challenge message, not the challenge request
	err = h.db.StoreMessageAndData(channelPath, challengeMsg)
	if err != nil {
		return err
	}

	return h.subs.BroadcastToAllClients(challengeMsg, channelPath)
}

// handleExpect checks that the message is from the local organizer and that
// it contains a valid challenge, then stores the msg
func (h *Handler) handleExpect(msg mmessage.Message, channelPath string) error {
	var federationExpect mfederation.FederationExpect
	err := msg.UnmarshalData(&federationExpect)
	if err != nil {
		return err
	}

	err = h.verifyLocalOrganizer(msg, channelPath)
	if err != nil {
		return err
	}

	// Both the FederationExpect and the embedded FederationChallenge need to
	// be signed by the local organizer
	err = h.verifyLocalOrganizer(federationExpect.ChallengeMsg, channelPath)
	if err != nil {
		return err
	}

	err = federationExpect.ChallengeMsg.VerifyMessage()
	if err != nil {
		return err
	}

	var challenge mfederation.FederationChallenge
	err = federationExpect.ChallengeMsg.UnmarshalData(&challenge)
	if err != nil {
		return err
	}

	err = challenge.Verify()
	if err != nil {
		return err
	}

	serverPk, err := h.getServerPk()
	if err != nil {
		return err
	}

	err = h.db.IsChallengeValid(serverPk, challenge, channelPath)
	if err != nil {
		return err
	}

	return h.db.StoreMessageAndData(channelPath, msg)
}

// handleInit checks that the message is from the local organizer and that
// it contains a valid challenge, then stores the msg,
// connect to the server and send the embedded challenge
func (h *Handler) handleInit(msg mmessage.Message, channelPath string) error {
	var federationInit mfederation.FederationInit
	err := msg.UnmarshalData(&federationInit)
	if err != nil {
		return err
	}

	err = h.verifyLocalOrganizer(msg, channelPath)
	if err != nil {
		return err
	}

	// Both the FederationInit and the embedded FederationChallenge need to
	// be signed by the local organizer
	err = h.verifyLocalOrganizer(federationInit.ChallengeMsg, channelPath)
	if err != nil {
		return err
	}

	var challenge mfederation.FederationChallenge
	err = federationInit.ChallengeMsg.UnmarshalData(&challenge)
	if err != nil {
		return err
	}

	err = challenge.Verify()
	if err != nil {
		return err
	}

	err = h.db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	remoteChannel := fmt.Sprintf(channelPattern, federationInit.LaoId)
	if h.isOnSameServer(federationInit.ServerAddress) {
		// In the edge case where the two LAOs are on the same server,
		// there is no need to create a websocket connection to the other
		// server and message from one "server" to the "other" could be
		// directly handled.
		// In that case the ack result of the federation_init will be sent
		// only after any federation_result sent when handling the challenge.
		_ = h.handleChallenge(federationInit.ChallengeMsg, remoteChannel, nil)
		return nil
	}

	remote, err := h.connectTo(federationInit.ServerAddress)
	if err != nil {
		return err
	}

	// send the challenge to the remote channelPath on the remote socket
	return h.publishTo(federationInit.ChallengeMsg, remoteChannel, remote)
}

func (h *Handler) handleChallenge(msg mmessage.Message, channelPath string,
	socket socket.Socket) error {
	var federationChallenge mfederation.FederationChallenge
	err := msg.UnmarshalData(&federationChallenge)
	if err != nil {
		return err
	}

	organizerPk, err := h.getOrganizerPk(channelPath)
	if err != nil {
		return err
	}

	federationExpect, err := h.db.GetFederationExpect(organizerPk, msg.Sender, federationChallenge, channelPath)
	if err != nil {
		return err
	}

	err = h.db.RemoveChallenge(federationChallenge)
	if err != nil {
		return err
	}

	if federationChallenge.ValidUntil < time.Now().Unix() {
		return errors.NewAccessDeniedError("This challenge has expired: %v", federationChallenge)
	}

	result := mfederation.FederationResult{
		Object:       channel.FederationObject,
		Action:       channel.FederationActionResult,
		Status:       "success",
		Reason:       "",
		PublicKey:    federationExpect.PublicKey,
		ChallengeMsg: federationExpect.ChallengeMsg,
	}

	resultMsg, err := h.createMessage(result)
	if err != nil {
		return err
	}

	err = h.db.StoreMessageAndData(channelPath, resultMsg)
	if err != nil {
		return err
	}

	remoteChannel := fmt.Sprintf(channelPattern, federationExpect.LaoId)
	if h.isOnSameServer(federationExpect.ServerAddress) || socket == nil {
		// In the edge case where the two LAOs are on the same server, the
		// result message would already be stored and handleResult will not be
		// called => broadcast the result to both federation channels directly.
		_ = h.db.StoreMessageAndData(remoteChannel, resultMsg)
		_ = h.subs.BroadcastToAllClients(resultMsg, remoteChannel)

		h.log.Info().Msgf("A federation was created with the local LAO %s",
			federationExpect.LaoId)
	} else {
		// Add the socket to the list of server sockets
		h.sockets.Upsert(socket)

		// Send the rumor state directly to avoid delay while syncing
		err = h.rumors.SendRumorStateTo(socket)
		if err != nil {
			return err
		}

		// publish the FederationResult to the other server
		err = h.publishTo(resultMsg, remoteChannel, socket)
		if err != nil {
			return err
		}
		h.log.Info().Msgf("A federation was created with the LAO %s from: %s",
			federationExpect.LaoId, federationExpect.ServerAddress)
	}

	// broadcast the FederationResult to the local organizer
	return h.subs.BroadcastToAllClients(resultMsg, channelPath)
}

func (h *Handler) handleResult(msg mmessage.Message, channelPath string) error {
	var result mfederation.FederationResult
	err := msg.UnmarshalData(&result)
	if err != nil {
		return err
	}

	// verify that the embedded challenge is correctly signed,
	// we compare the sender field of the challenge later
	err = result.ChallengeMsg.VerifyMessage()
	if err != nil {
		return err
	}

	if result.Status != "success" {
		return errors.NewInternalServerError("failed to establish federated connection: %s", result.Reason)
	}

	organizerPk, err := h.getOrganizerPk(channelPath)
	if err != nil {
		return err
	}

	// the result is from the other server if publicKey == organizer
	if result.PublicKey != organizerPk {
		return errors.NewInvalidMessageFieldError("invalid public key contained in FederationResult message")
	}

	var federationChallenge mfederation.FederationChallenge
	err = result.ChallengeMsg.UnmarshalData(&federationChallenge)
	if err != nil {
		return err
	}

	err = federationChallenge.Verify()
	if err != nil {
		return err
	}

	// try to get a matching FederationInit, if found then we know that
	// the local organizer was waiting this result
	_, err = h.db.GetFederationInit(organizerPk, result.ChallengeMsg.Sender, federationChallenge, channelPath)
	if err != nil {
		return err
	}

	err = h.db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	return h.subs.BroadcastToAllClients(msg, channelPath)
}

func (h *Handler) handleTokensExchange(msg mmessage.Message, channelPath string) error {
	var tokensExchange mfederation.FederationTokensExchange
	err := msg.UnmarshalData(&tokensExchange)
	if err != nil {
		return err
	}

	err = h.verifyLocalOrganizer(msg, channelPath)
	if err != nil {
		return err
	}

	err = h.db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	return h.subs.BroadcastToAllClients(msg, channelPath)
}

func (h *Handler) getOrganizerPk(federationChannel string) (string, error) {
	laoChannel := strings.TrimSuffix(federationChannel, "/federation")

	organizerPk, err := h.db.GetOrganizerPubKey(laoChannel)
	if err != nil {
		return "", err
	}

	organizerPkBytes, err := organizerPk.MarshalBinary()
	if err != nil {
		return "", errors.NewInternalServerError("failed to marshal organizer key: %v", err)
	}

	return base64.URLEncoding.EncodeToString(organizerPkBytes), nil
}

func (h *Handler) getServerPk() (string, error) {
	serverPk, _, err := h.db.GetServerKeys()
	if err != nil {
		return "", err
	}

	serverPkBytes, err := serverPk.MarshalBinary()
	if err != nil {
		return "", errors.NewInternalServerError("failed to marshal server pk: %v", err)
	}

	return base64.URLEncoding.EncodeToString(serverPkBytes), nil
}

func (h *Handler) verifyLocalOrganizer(msg mmessage.Message, channelPath string) error {
	organizePk, err := h.getOrganizerPk(channelPath)
	if err != nil {
		return err
	}

	if organizePk != msg.Sender {
		return errors.NewAccessDeniedError("sender is not the organizer of the channelPath")
	}

	return nil
}

func (h *Handler) isOnSameServer(address string) bool {
	_, clientServerAddress, serverServerAddress, _ := h.conf.GetServerInfo()

	isSameAddress := address == clientServerAddress || address == serverServerAddress

	h.log.Info().Msgf("isOnSameServer=%v, remote=%s, client=%s, server=%s",
		isSameAddress, address, clientServerAddress, serverServerAddress)

	return isSameAddress
}

func (h *Handler) connectTo(serverAddress string) (socket.Socket, error) {
	ws, _, err := websocket.DefaultDialer.Dial(serverAddress, nil)
	if err != nil {
		return nil, errors.NewInternalServerError("failed to connect to server %s: %v", serverAddress, err)
	}

	messageChan := h.hub.GetMessageChan()
	closedSockets := h.hub.GetClosedSockets()
	wg := h.hub.GetWaitGroup()
	stopChan := h.hub.GetStopChan()

	server := socket.NewServerSocket(messageChan, closedSockets, ws, wg, stopChan, h.log)
	h.sockets.Upsert(server)

	wg.Add(2)

	go server.WritePump()
	go server.ReadPump()

	err = h.greets.SendGreetServer(server)
	if err != nil {
		return nil, err
	}

	return server, h.rumors.SendRumorStateTo(server)
}

func (h *Handler) createMessage(data channel.MessageData) (mmessage.Message, error) {

	dataBytes, err := json.Marshal(data)
	if err != nil {
		return mmessage.Message{}, errors.NewJsonMarshalError(err.Error())
	}
	dataBase64 := base64.URLEncoding.EncodeToString(dataBytes)

	serverPk, serverSk, err := h.db.GetServerKeys()
	if err != nil {
		return mmessage.Message{}, err
	}

	senderBytes, err := serverPk.MarshalBinary()
	if err != nil {
		return mmessage.Message{}, errors.NewInternalServerError("failed to marshal key: %v", err)
	}
	sender := base64.URLEncoding.EncodeToString(senderBytes)

	signatureBytes, err := schnorr.Sign(crypto.Suite, serverSk, dataBytes)
	if err != nil {
		return mmessage.Message{}, errors.NewInternalServerError("failed to sign message: %v", err)
	}
	signature := base64.URLEncoding.EncodeToString(signatureBytes)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            sender,
		Signature:         signature,
		MessageID:         channel.Hash(dataBase64, signature),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	return msg, nil
}

func (h *Handler) publishTo(msg mmessage.Message, channelPath string,
	socket socket.Socket) error {
	publishMsg := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodPublish,
		},
		Params: mpublish.PublishParams{
			Channel: channelPath,
			Message: msg,
		},
	}

	publishBytes, err := json.Marshal(&publishMsg)
	if err != nil {
		return errors.NewJsonMarshalError(err.Error())
	}

	socket.Send(publishBytes)
	return nil
}
