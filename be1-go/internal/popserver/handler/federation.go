package handler

import (
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"github.com/gorilla/websocket"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar"
	"popstellar/crypto"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"strings"
	"time"
)

const (
	channelPattern = "/root/%s/federation"
)

func handleChannelFederation(channelPath string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelFederation")
	}

	if object != messagedata.FederationObject {
		errAnswer = answer.NewInvalidMessageFieldError("invalid object %v", object)
		return errAnswer.Wrap("handleChannelFederation")
	}

	switch action {
	case messagedata.FederationActionChallengeRequest:
		errAnswer = handleRequestChallenge(msg, channelPath)
	case messagedata.FederationActionInit:
		errAnswer = handleInit(msg, channelPath)
	case messagedata.FederationActionExpect:
		errAnswer = handleExpect(msg, channelPath)
	case messagedata.FederationActionChallenge:
		errAnswer = handleChallenge(msg, channelPath)
	case messagedata.FederationActionResult:
		errAnswer = handleResult(msg, channelPath)

	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelFederation")
	}

	return nil
}

// handleRequestChallenge expects the sender to be the organizer of the lao,
// a challenge message is then stored and broadcast on the same channel.
// The FederationChallengeRequest message is neither stored nor broadcast
func handleRequestChallenge(msg message.Message, channelPath string) *answer.Error {
	var requestChallenge messagedata.FederationChallengeRequest
	errAnswer := msg.UnmarshalMsgData(&requestChallenge)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationRequestChallenge")
	}

	errAnswer = verifyLocalOrganizer(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationRequestChallenge")
	}

	randomBytes := make([]byte, 32)
	_, err := rand.Read(randomBytes)
	if err != nil {
		errAnswer = answer.NewInternalServerError(
			"Failed to generate random bytes: %v", err)
		return errAnswer.Wrap("handleFederationRequestChallenge")
	}

	challengeValue := hex.EncodeToString(randomBytes)
	expirationTime := time.Now().Add(time.Minute * 5).Unix()
	federationChallenge := messagedata.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      challengeValue,
		ValidUntil: expirationTime,
	}

	// The challenge sent to the organizer is signed by the server but should
	// not be confused with the challenge that will be signed by the organizer
	challengeMsg, errAnswer := createMessage(federationChallenge)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationRequestChallenge")
	}

	db, errAnswer := database.GetFederationRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationRequestChallenge")
	}

	// store the generated challenge message, not the challenge request
	err = db.StoreMessageAndData(channelPath, challengeMsg)
	if err != nil {
		errAnswer = answer.NewStoreDatabaseError(err.Error())
		return errAnswer.Wrap("handleFederationRequestChallenge")
	}

	errAnswer = broadcastToAllClients(challengeMsg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationRequestChallenge")
	}

	return nil
}

// handleExpect checks that the message is from the local organizer and that
// it contains a valid challenge, then stores the msg
func handleExpect(msg message.Message, channelPath string) *answer.Error {
	var federationExpect messagedata.FederationExpect
	errAnswer := msg.UnmarshalMsgData(&federationExpect)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationExpect")
	}

	errAnswer = verifyLocalOrganizer(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationExpect")
	}

	// Both the FederationExpect and the embedded FederationChallenge need to
	// be signed by the local organizer
	errAnswer = verifyLocalOrganizer(federationExpect.ChallengeMsg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationExpect")
	}

	var challenge messagedata.FederationChallenge
	errAnswer = federationExpect.ChallengeMsg.UnmarshalMsgData(&challenge)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationExpect")
	}

	errAnswer = challenge.Verify()
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationExpect")
	}

	db, errAnswer := database.GetFederationRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationExpect")
	}

	serverPk, errAnswer := getServerPk()
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationExpect")
	}

	err := db.IsChallengeValid(serverPk, challenge, channelPath)
	if err != nil {
		errAnswer = answer.NewQueryDatabaseError("No valid challenge: %v", err)
		return errAnswer.Wrap("handleFederationExpect")
	}

	remoteChannel := fmt.Sprintf(channelPattern, federationExpect.LaoId)
	_ = state.AddChannel(remoteChannel)

	err = db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		errAnswer = answer.NewStoreDatabaseError(err.Error())
		return errAnswer.Wrap("handleFederationExpect")
	}

	return nil
}

// handleInit checks that the message is from the local organizer and that
// it contains a valid challenge, then stores the msg,
// connect to the server and send the embedded challenge
func handleInit(msg message.Message, channelPath string) *answer.Error {
	var federationInit messagedata.FederationInit
	errAnswer := msg.UnmarshalMsgData(&federationInit)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	errAnswer = verifyLocalOrganizer(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	// Both the FederationInit and the embedded FederationChallenge need to
	// be signed by the local organizer
	errAnswer = verifyLocalOrganizer(federationInit.ChallengeMsg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	var challenge messagedata.FederationChallenge
	errAnswer = federationInit.ChallengeMsg.UnmarshalMsgData(&challenge)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	errAnswer = challenge.Verify()
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	db, errAnswer := database.GetFederationRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	err := db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		errAnswer = answer.NewStoreDatabaseError(err.Error())
		return errAnswer.Wrap("handleFederationInit")
	}

	remote, errAnswer := connectTo(federationInit.ServerAddress)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	//Force the remote server to be subscribed to /root/<remote_lao>/federation
	remoteChannel := fmt.Sprintf(channelPattern, federationInit.LaoId)
	_ = state.AddChannel(remoteChannel)
	errAnswer = state.Subscribe(remote, remoteChannel)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	subscribeMsg := method.Subscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "subscribe",
		},
		Params: method.SubscribeParams{Channel: channelPath},
	}

	subscribeBytes, err := json.Marshal(subscribeMsg)
	if err != nil {
		errAnswer = answer.NewInternalServerError(
			"failed to marshal subscribe: %v", err)
		return errAnswer.Wrap("handleFederationInit")
	}

	// Subscribe to /root/<local_lao>/federation on the remote server
	errAnswer = state.SendToAll(subscribeBytes, remoteChannel)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	// send the challenge to a channel where the remote server is subscribed to
	errAnswer = publishTo(federationInit.ChallengeMsg, remoteChannel)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationInit")
	}

	return nil
}

func handleChallenge(msg message.Message, channelPath string) *answer.Error {
	var federationChallenge messagedata.FederationChallenge
	errAnswer := msg.UnmarshalMsgData(&federationChallenge)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationChallenge")
	}

	db, errAnswer := database.GetFederationRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationChallenge")
	}

	organizerPk, errAnswer := getOrganizerPk(channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationChallenge")
	}

	federationExpect, err := db.GetFederationExpect(organizerPk, msg.Sender,
		federationChallenge, channelPath)
	if err != nil {
		errAnswer = answer.NewQueryDatabaseError(
			"failed to get federation expect: %v", err)
		return errAnswer.Wrap("handleFederationChallenge")
	}

	err = db.RemoveChallenge(federationChallenge)
	if err != nil {
		errAnswer = answer.NewQueryDatabaseError("failed to use challenge: %v", err)
		return errAnswer.Wrap("handleFederationChallenge")
	}

	if federationChallenge.ValidUntil < time.Now().Unix() {
		errAnswer = answer.NewAccessDeniedError(
			"This challenge has expired: %v", federationChallenge)
		return errAnswer.Wrap("handleFederationChallenge")
	}

	result := messagedata.FederationResult{
		Object:       messagedata.FederationObject,
		Action:       messagedata.FederationActionResult,
		Status:       "success",
		Reason:       "",
		PublicKey:    federationExpect.PublicKey,
		ChallengeMsg: federationExpect.ChallengeMsg,
	}

	resultMsg, errAnswer := createMessage(result)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationChallenge")
	}

	err = db.StoreMessageAndData(channelPath, resultMsg)
	if err != nil {
		errAnswer = answer.NewStoreDatabaseError(err.Error())
		return errAnswer.Wrap("handleFederationChallenge")
	}

	// publish the FederationResult to the other server
	remoteChannel := fmt.Sprintf(channelPattern, federationExpect.LaoId)
	errAnswer = publishTo(resultMsg, remoteChannel)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationChallenge")
	}

	// broadcast the FederationResult to the local organizer
	errAnswer = broadcastToAllClients(resultMsg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationChallenge")
	}

	return nil
}

func handleResult(msg message.Message, channelPath string) *answer.Error {
	var result messagedata.FederationResult
	errAnswer := msg.UnmarshalMsgData(&result)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationResult")
	}

	// verify that the embedded challenge is correctly signed,
	// we compare the sender field of the challenge later
	errAnswer = verifyMessage(result.ChallengeMsg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationResult")
	}

	if result.Status != "success" {
		errAnswer = answer.NewInternalServerError(
			"failed to establish federated connection: %s", result.Reason)
		return errAnswer.Wrap("handleFederationResult")
	}

	db, errAnswer := database.GetFederationRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationResult")
	}

	organizerPk, errAnswer := getOrganizerPk(channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationResult")
	}

	// the result is from the other server if publicKey == organizer
	if result.PublicKey != organizerPk {
		errAnswer = answer.NewInvalidMessageFieldError(
			"invalid public key contained in FederationResult message")
		return errAnswer.Wrap("handleFederationResult")
	}

	var federationChallenge messagedata.FederationChallenge
	errAnswer = result.ChallengeMsg.UnmarshalMsgData(&federationChallenge)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationResult")
	}

	errAnswer = federationChallenge.Verify()
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationResult")
	}

	// try to get a matching FederationInit, if found then we know that
	// the local organizer was waiting this result
	_, err := db.GetFederationInit(organizerPk,
		result.ChallengeMsg.Sender, federationChallenge, channelPath)
	if err != nil {
		errAnswer = answer.NewQueryDatabaseError(
			"failed to get federation init: %v", err)
		return errAnswer.Wrap("handleFederationResult")
	}

	err = db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		errAnswer = answer.NewStoreDatabaseError(err.Error())
		return errAnswer.Wrap("handleFederationResult")
	}

	errAnswer = broadcastToAllClients(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleFederationChallenge")
	}

	return nil
}

func getOrganizerPk(federationChannel string) (string, *answer.Error) {
	db, errAnswer := database.GetFederationRepositoryInstance()
	if errAnswer != nil {
		return "", errAnswer.Wrap("getOrganizerPk")
	}

	laoChannel := strings.TrimSuffix(federationChannel, "/federation")

	organizerPk, err := db.GetOrganizerPubKey(laoChannel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get key")
		return "", errAnswer.Wrap("getOrganizerPk")
	}

	organizerPkBytes, err := organizerPk.MarshalBinary()
	if err != nil {
		errAnswer = answer.NewInternalServerError(
			"failed to marshal organizer key: %v", err)
		return "", errAnswer.Wrap("getOrganizerPk")
	}

	return base64.URLEncoding.EncodeToString(organizerPkBytes), nil
}

func getServerPk() (string, *answer.Error) {
	db, errAnswer := database.GetFederationRepositoryInstance()
	if errAnswer != nil {
		return "", errAnswer.Wrap("getServerPk")
	}

	serverPk, _, err := db.GetServerKeys()
	if err != nil {
		errAnswer = answer.NewInternalServerError(
			"Failed to get server keys: %v", err)
		return "", errAnswer.Wrap("getServerPk")
	}

	serverPkBytes, err := serverPk.MarshalBinary()
	if err != nil {
		errAnswer = answer.NewInternalServerError(
			"failed to marshal server pk: %v", err)
		return "", errAnswer.Wrap("getServerPk")
	}

	return base64.URLEncoding.EncodeToString(serverPkBytes), nil
}

func verifyLocalOrganizer(msg message.Message, channelPath string) *answer.Error {
	organizePk, errAnswer := getOrganizerPk(channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("verifyLocalOrganizer")
	}

	if organizePk != msg.Sender {
		errAnswer = answer.NewAccessDeniedError("sender is not the organizer of the channel")
		return errAnswer.Wrap("verifyLocalSender")
	}

	return nil
}

func connectTo(serverAddress string) (socket.Socket, *answer.Error) {
	ws, _, err := websocket.DefaultDialer.Dial(serverAddress, nil)
	if err != nil {
		errAnswer := answer.NewInternalServerError(
			"failed to connect to server %s: %v", serverAddress, err)
		return nil, errAnswer.Wrap("connectTo")
	}

	messageChan, errAnswer := state.GetMessageChan()
	if errAnswer != nil {
		return nil, errAnswer.Wrap("connectTo")
	}

	closedSockets, errAnswer := state.GetClosedSockets()
	if errAnswer != nil {
		return nil, errAnswer.Wrap("connectTo")
	}

	wg, errAnswer := state.GetWaitGroup()
	if errAnswer != nil {
		return nil, errAnswer.Wrap("connectTo")
	}

	stopChan, errAnswer := state.GetStopChan()
	if errAnswer != nil {
		return nil, errAnswer.Wrap("connectTo")
	}

	client := socket.NewClientSocket(messageChan, closedSockets, ws, wg,
		stopChan, popstellar.Logger)

	wg.Add(2)

	go client.WritePump()
	go client.ReadPump()

	return client, nil
}

func createMessage(data messagedata.MessageData) (message.Message, *answer.Error) {
	db, errAnswer := database.GetFederationRepositoryInstance()
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createMessage")
	}

	dataBytes, err := json.Marshal(data)
	if err != nil {
		errAnswer = answer.NewInternalServerError(
			"failed to marshal %v: %v", data, err)
		return message.Message{}, errAnswer.Wrap("createMessage")
	}
	dataBase64 := base64.URLEncoding.EncodeToString(dataBytes)

	serverPk, serverSk, err := db.GetServerKeys()
	if err != nil {
		errAnswer = answer.NewInternalServerError(
			"Failed to get server keys: %v", err)
		return message.Message{}, errAnswer.Wrap("createMessage")
	}

	senderBytes, err := serverPk.MarshalBinary()
	if err != nil {
		errAnswer = answer.NewInternalServerError(
			"failed to marshal key: %v", err)
		return message.Message{}, errAnswer.Wrap("createMessage")
	}
	sender := base64.URLEncoding.EncodeToString(senderBytes)

	signatureBytes, err := schnorr.Sign(crypto.Suite, serverSk, dataBytes)
	if err != nil {
		errAnswer = answer.NewInternalServerError(
			"failed to sign message: %v", err)
		return message.Message{}, errAnswer.Wrap("createMessage")
	}
	signature := base64.URLEncoding.EncodeToString(signatureBytes)

	msg := message.Message{
		Data:              dataBase64,
		Sender:            sender,
		Signature:         signature,
		MessageID:         messagedata.Hash(dataBase64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	return msg, nil
}

func publishTo(msg message.Message, channel string) *answer.Error {
	publishMsg := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},
		Params: method.PublishParams{
			Channel: channel,
			Message: msg,
		},
	}

	publishBytes, err := json.Marshal(&publishMsg)
	if err != nil {
		errAnswer := answer.NewInternalServerError(
			"failed to marshal publish: %v", err)
		return errAnswer.Wrap("publishTo")
	}

	errAnswer := state.SendToAll(publishBytes, channel)
	if errAnswer != nil {
		return errAnswer.Wrap("publishTo")
	}

	return nil
}
