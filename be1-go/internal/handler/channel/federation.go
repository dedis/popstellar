package channel

import (
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"github.com/gorilla/websocket"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/logger"
	jsonrpc "popstellar/internal/message"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
	state "popstellar/internal/singleton/state"
	"strings"
	"time"
)

const (
	channelPattern = "/root/%s/federation"
)

func handleChannelFederation(channelPath string, msg message.Message) error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return err
	}

	if object != messagedata.FederationObject {
		return errors.NewInvalidMessageFieldError("invalid object %v", object)
	}

	switch action {
	case messagedata.FederationActionChallengeRequest:
		err = handleRequestChallenge(msg, channelPath)
	case messagedata.FederationActionInit:
		err = handleInit(msg, channelPath)
	case messagedata.FederationActionExpect:
		err = handleExpect(msg, channelPath)
	case messagedata.FederationActionChallenge:
		err = handleChallenge(msg, channelPath)
	case messagedata.FederationActionResult:
		err = handleResult(msg, channelPath)
	default:
		err = errors.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	return err
}

// handleRequestChallenge expects the sender to be the organizer of the lao,
// a challenge message is then stored and broadcast on the same channelPath.
// The FederationChallengeRequest message is neither stored nor broadcast
func handleRequestChallenge(msg message.Message, channelPath string) error {
	var requestChallenge messagedata.FederationChallengeRequest
	err := msg.UnmarshalData(&requestChallenge)
	if err != nil {
		return err
	}

	err = verifyLocalOrganizer(msg, channelPath)
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
	federationChallenge := messagedata.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      challengeValue,
		ValidUntil: expirationTime,
	}

	// The challenge sent to the organizer is signed by the server but should
	// not be confused with the challenge that will be signed by the organizer
	challengeMsg, err := createMessage(federationChallenge)
	if err != nil {
		return err
	}

	db, err := database.GetFederationRepositoryInstance()
	if err != nil {
		return err
	}

	// store the generated challenge message, not the challenge request
	err = db.StoreMessageAndData(channelPath, challengeMsg)
	if err != nil {
		return err
	}

	return broadcastToAllClients(challengeMsg, channelPath)
}

// handleExpect checks that the message is from the local organizer and that
// it contains a valid challenge, then stores the msg
func handleExpect(msg message.Message, channelPath string) error {
	var federationExpect messagedata.FederationExpect
	err := msg.UnmarshalData(&federationExpect)
	if err != nil {
		return err
	}

	err = verifyLocalOrganizer(msg, channelPath)
	if err != nil {
		return err
	}

	// Both the FederationExpect and the embedded FederationChallenge need to
	// be signed by the local organizer
	err = verifyLocalOrganizer(federationExpect.ChallengeMsg, channelPath)
	if err != nil {
		return err
	}

	var challenge messagedata.FederationChallenge
	err = federationExpect.ChallengeMsg.UnmarshalData(&challenge)
	if err != nil {
		return err
	}

	err = challenge.Verify()
	if err != nil {
		return err
	}

	db, err := database.GetFederationRepositoryInstance()
	if err != nil {
		return err
	}

	serverPk, err := getServerPk()
	if err != nil {
		return err
	}

	err = db.IsChallengeValid(serverPk, challenge, channelPath)
	if err != nil {
		return err
	}

	remoteChannel := fmt.Sprintf(channelPattern, federationExpect.LaoId)
	_ = state.AddChannel(remoteChannel)

	return db.StoreMessageAndData(channelPath, msg)
}

// handleInit checks that the message is from the local organizer and that
// it contains a valid challenge, then stores the msg,
// connect to the server and send the embedded challenge
func handleInit(msg message.Message, channelPath string) error {
	var federationInit messagedata.FederationInit
	err := msg.UnmarshalData(&federationInit)
	if err != nil {
		return err
	}

	err = verifyLocalOrganizer(msg, channelPath)
	if err != nil {
		return err
	}

	// Both the FederationInit and the embedded FederationChallenge need to
	// be signed by the local organizer
	err = verifyLocalOrganizer(federationInit.ChallengeMsg, channelPath)
	if err != nil {
		return err
	}

	var challenge messagedata.FederationChallenge
	err = federationInit.ChallengeMsg.UnmarshalData(&challenge)
	if err != nil {
		return err
	}

	err = challenge.Verify()
	if err != nil {
		return err
	}

	db, err := database.GetFederationRepositoryInstance()
	if err != nil {
		return err
	}

	err = db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	remote, err := connectTo(federationInit.ServerAddress)
	if err != nil {
		return err
	}

	//Force the remote server to be subscribed to /root/<remote_lao>/federation
	remoteChannel := fmt.Sprintf(channelPattern, federationInit.LaoId)
	_ = state.AddChannel(remoteChannel)
	err = state.Subscribe(remote, remoteChannel)
	if err != nil {
		return err
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
		return errors.NewJsonMarshalError(err.Error())
	}

	// Subscribe to /root/<local_lao>/federation on the remote server
	err = state.SendToAll(subscribeBytes, remoteChannel)
	if err != nil {
		return err
	}

	// send the challenge to a channelPath where the remote server is subscribed to
	return publishTo(federationInit.ChallengeMsg, remoteChannel)
}

func handleChallenge(msg message.Message, channelPath string) error {
	var federationChallenge messagedata.FederationChallenge
	err := msg.UnmarshalData(&federationChallenge)
	if err != nil {
		return err
	}

	db, err := database.GetFederationRepositoryInstance()
	if err != nil {
		return err
	}

	organizerPk, err := getOrganizerPk(channelPath)
	if err != nil {
		return err
	}

	federationExpect, err := db.GetFederationExpect(organizerPk, msg.Sender, federationChallenge, channelPath)
	if err != nil {
		return err
	}

	err = db.RemoveChallenge(federationChallenge)
	if err != nil {
		return err
	}

	if federationChallenge.ValidUntil < time.Now().Unix() {
		return errors.NewAccessDeniedError("This challenge has expired: %v", federationChallenge)
	}

	result := messagedata.FederationResult{
		Object:       messagedata.FederationObject,
		Action:       messagedata.FederationActionResult,
		Status:       "success",
		Reason:       "",
		PublicKey:    federationExpect.PublicKey,
		ChallengeMsg: federationExpect.ChallengeMsg,
	}

	resultMsg, err := createMessage(result)
	if err != nil {
		return err
	}

	err = db.StoreMessageAndData(channelPath, resultMsg)
	if err != nil {
		return err
	}

	// publish the FederationResult to the other server
	remoteChannel := fmt.Sprintf(channelPattern, federationExpect.LaoId)
	err = publishTo(resultMsg, remoteChannel)
	if err != nil {
		return err
	}

	// broadcast the FederationResult to the local organizer
	return broadcastToAllClients(resultMsg, channelPath)
}

func handleResult(msg message.Message, channelPath string) error {
	var result messagedata.FederationResult
	err := msg.UnmarshalData(&result)
	if err != nil {
		return err
	}

	// verify that the embedded challenge is correctly signed,
	// we compare the sender field of the challenge later
	err = verifyMessage(result.ChallengeMsg)
	if err != nil {
		return err
	}

	if result.Status != "success" {
		return errors.NewInternalServerError("failed to establish federated connection: %s", result.Reason)
	}

	db, err := database.GetFederationRepositoryInstance()
	if err != nil {
		return err
	}

	organizerPk, err := getOrganizerPk(channelPath)
	if err != nil {
		return err
	}

	// the result is from the other server if publicKey == organizer
	if result.PublicKey != organizerPk {
		return errors.NewInvalidMessageFieldError("invalid public key contained in FederationResult message")
	}

	var federationChallenge messagedata.FederationChallenge
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
	_, err = db.GetFederationInit(organizerPk, result.ChallengeMsg.Sender, federationChallenge, channelPath)
	if err != nil {
		return err
	}

	err = db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	return broadcastToAllClients(msg, channelPath)
}

func getOrganizerPk(federationChannel string) (string, error) {
	db, err := database.GetFederationRepositoryInstance()
	if err != nil {
		return "", err
	}

	laoChannel := strings.TrimSuffix(federationChannel, "/federation")

	organizerPk, err := db.GetOrganizerPubKey(laoChannel)
	if err != nil {
		return "", err
	}

	organizerPkBytes, err := organizerPk.MarshalBinary()
	if err != nil {
		return "", errors.NewInternalServerError("failed to marshal organizer key: %v", err)
	}

	return base64.URLEncoding.EncodeToString(organizerPkBytes), nil
}

func getServerPk() (string, error) {
	db, err := database.GetFederationRepositoryInstance()
	if err != nil {
		return "", err
	}

	serverPk, _, err := db.GetServerKeys()
	if err != nil {
		return "", err
	}

	serverPkBytes, err := serverPk.MarshalBinary()
	if err != nil {
		return "", errors.NewInternalServerError("failed to marshal server pk: %v", err)
	}

	return base64.URLEncoding.EncodeToString(serverPkBytes), nil
}

func verifyLocalOrganizer(msg message.Message, channelPath string) error {
	organizePk, err := getOrganizerPk(channelPath)
	if err != nil {
		return err
	}

	if organizePk != msg.Sender {
		return errors.NewAccessDeniedError("sender is not the organizer of the channelPath")
	}

	return nil
}

func connectTo(serverAddress string) (socket.Socket, error) {
	ws, _, err := websocket.DefaultDialer.Dial(serverAddress, nil)
	if err != nil {
		return nil, errors.NewInternalServerError("failed to connect to server %s: %v", serverAddress, err)
	}

	messageChan, err := state.GetMessageChan()
	if err != nil {
		return nil, err
	}

	closedSockets, err := state.GetClosedSockets()
	if err != nil {
		return nil, err
	}

	wg, err := state.GetWaitGroup()
	if err != nil {
		return nil, err
	}

	stopChan, err := state.GetStopChan()
	if err != nil {
		return nil, err
	}

	client := socket.NewClientSocket(messageChan, closedSockets, ws, wg, stopChan, logger.Logger)

	wg.Add(2)

	go client.WritePump()
	go client.ReadPump()

	return client, nil
}

func createMessage(data messagedata.MessageData) (message.Message, error) {
	db, err := database.GetFederationRepositoryInstance()
	if err != nil {
		return message.Message{}, err
	}

	dataBytes, err := json.Marshal(data)
	if err != nil {
		return message.Message{}, errors.NewJsonMarshalError(err.Error())
	}
	dataBase64 := base64.URLEncoding.EncodeToString(dataBytes)

	serverPk, serverSk, err := db.GetServerKeys()
	if err != nil {
		return message.Message{}, err
	}

	senderBytes, err := serverPk.MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewInternalServerError("failed to marshal key: %v", err)
	}
	sender := base64.URLEncoding.EncodeToString(senderBytes)

	signatureBytes, err := schnorr.Sign(crypto.Suite, serverSk, dataBytes)
	if err != nil {
		return message.Message{}, errors.NewInternalServerError("failed to sign message: %v", err)
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

func publishTo(msg message.Message, channelPath string) error {
	publishMsg := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},
		Params: method.PublishParams{
			Channel: channelPath,
			Message: msg,
		},
	}

	publishBytes, err := json.Marshal(&publishMsg)
	if err != nil {
		return errors.NewJsonMarshalError(err.Error())
	}

	return state.SendToAll(publishBytes, channelPath)
}
