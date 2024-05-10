package handler

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/utils"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/validation"
)

func handleChannel(channelID string, msg message.Message) *answer.Error {
	errAnswer := verifyMessage(msg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannel")
	}

	db, errAnswer := database.GetChannelRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannel")
	}

	msgAlreadyExists, err := db.HasMessage(msg.MessageID)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("if message exists: %v", err)
		return errAnswer.Wrap("handleChannel")
	}
	if msgAlreadyExists {
		errAnswer := answer.NewInvalidActionError("message %s was already received", msg.MessageID)
		return errAnswer.Wrap("handleChannel")
	}

	channelType, err := db.GetChannelType(channelID)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("channel type: %v", err)
		return errAnswer.Wrap("handleChannel")
	}

	switch channelType {
	case channelRoot:
		errAnswer = handleChannelRoot(channelID, msg)
	case channelLao:
		errAnswer = handleChannelLao(channelID, msg)
	case channelElection:
		errAnswer = handleChannelElection(channelID, msg)
	case ChannelChirp:
		errAnswer = handleChannelChirp(channelID, msg)
	case ChannelReaction:
		errAnswer = handleChannelReaction(channelID, msg)
	case ChannelCoin:
		errAnswer = handleChannelCoin(channelID, msg)
	default:
		errAnswer = answer.NewInvalidResourceError("unknown channel type for %s", channelID)
	}

	if errAnswer != nil {
		return errAnswer.Wrap("handleChannel")
	}

	return nil
}

// util for the channels

func verifyMessage(msg message.Message) *answer.Error {
	dataBytes, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode data: %v", err)
		return errAnswer.Wrap("verifyMessage")
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode public key: %v", err)
		return errAnswer.Wrap("verifyMessage")
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(msg.Signature)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode signature: %v", err)
		return errAnswer.Wrap("verifyMessage")
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to verify signature : %v", err)
		return errAnswer.Wrap("verifyMessage")
	}

	expectedMessageID := messagedata.Hash(msg.Data, msg.Signature)
	if expectedMessageID != msg.MessageID {
		errAnswer := answer.NewInvalidActionError("messageID is wrong: expected %s found %s",
			expectedMessageID, msg.MessageID)
		return errAnswer.Wrap("verifyMessage")
	}
	return nil
}

func verifyDataAndGetObjectAction(msg message.Message) (string, string, *answer.Error) {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode message data: %v", err)
		return "", "", errAnswer.Wrap("verifyDataAndGetObjectAction")
	}

	// validate message data against the json schema
	errAnswer := utils.VerifyJSON(jsonData, validation.Data)
	if errAnswer != nil {
		return "", "", errAnswer.Wrap("verifyDataAndGetObjectAction")
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to get object#action: %v", err)
		return "", "", errAnswer.Wrap("verifyDataAndGetObjectAction")
	}
	return object, action, nil
}

func Sign(data []byte) ([]byte, *answer.Error) {
	var errAnswer *answer.Error

	serverSecretKey, errAnswer := config.GetServerSecretKeyInstance()
	if errAnswer != nil {
		return nil, errAnswer.Wrap("Sign")
	}

	signatureBuf, err := schnorr.Sign(crypto.Suite, serverSecretKey, data)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to sign the data: %v", err)
		return nil, errAnswer.Wrap("Sign")
	}
	return signatureBuf, nil
}

// generateKeys generates and returns a key pair
func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	return point, secret
}

func broadcastToAllClients(msg message.Message, channel string) *answer.Error {
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
			channel,
			msg,
		},
	}

	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal broadcast query: %v", err)
		return errAnswer.Wrap("broadcastToAllClients")
	}

	errAnswer := state.SendToAll(buf, channel)
	if errAnswer != nil {
		return errAnswer.Wrap("broadcastToAllClients")
	}

	return nil
}

const (
	channelRoot      = "root"
	channelLao       = "lao"
	channelElection  = "election"
	ChannelChirp     = "chirp"
	ChannelReaction  = "reaction"
	ChannelConsensus = "consensus"
	ChannelCoin      = "coin"
	ChannelAuth      = "auth"
)
