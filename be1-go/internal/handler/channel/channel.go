package channel

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/internal/crypto"
	jsonrpc "popstellar/internal/message"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
	"popstellar/internal/singleton/utils"
	"popstellar/internal/sqlite"
	"popstellar/internal/validation"
)

func HandleChannel(channelPath string, msg message.Message, fromRumor bool) *answer.Error {
	errAnswer := verifyMessage(msg)
	if errAnswer != nil {
		return errAnswer.Wrap("HandleChannel")
	}

	db, err := database.GetChannelRepositoryInstance()
	if err != nil {
		return answer.NewInternalServerError(err.Error())
	}

	msgAlreadyExists, err := db.HasMessage(msg.MessageID)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("if message exists: %v", err)
		return errAnswer.Wrap("HandleChannel")
	}
	if msgAlreadyExists && fromRumor {
		return nil
	}
	if msgAlreadyExists {
		errAnswer := answer.NewInvalidActionError("message %s was already received", msg.MessageID)
		return errAnswer.Wrap("HandleChannel")
	}

	channelType, err := db.GetChannelType(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("channelPath type: %v", err)
		return errAnswer.Wrap("HandleChannel")
	}

	switch channelType {
	case sqlite.RootType:
		errAnswer = handleChannelRoot(msg)
	case sqlite.LaoType:
		errAnswer = handleChannelLao(channelPath, msg)
	case sqlite.ElectionType:
		errAnswer = handleChannelElection(channelPath, msg)
	case sqlite.ChirpType:
		errAnswer = handleChannelChirp(channelPath, msg)
	case sqlite.ReactionType:
		errAnswer = handleChannelReaction(channelPath, msg)
	case sqlite.CoinType:
		errAnswer = handleChannelCoin(channelPath, msg)
	case sqlite.FederationType:
		errAnswer = handleChannelFederation(channelPath, msg)
	default:
		errAnswer = answer.NewInvalidResourceError("unknown channelPath type for %s", channelPath)
	}

	if errAnswer != nil {
		return errAnswer.Wrap("HandleChannel")
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

func verifyDataAndGetObjectAction(msg message.Message) (string, string, error) {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode message data: %v", err)
		return "", "", errAnswer.Wrap("verifyDataAndGetObjectAction")
	}

	// validate message data against the json schema
	err = utils.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return "", "", err
	}

	return messagedata.GetObjectAndAction(jsonData)
}

func Sign(data []byte) ([]byte, *answer.Error) {
	serverSecretKey, err := config.GetServerSecretKeyInstance()
	if err != nil {
		return nil, answer.NewInternalServerError(err.Error())
	}

	signatureBuf, err := schnorr.Sign(crypto.Suite, serverSecretKey, data)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to sign the data: %v", err)
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
