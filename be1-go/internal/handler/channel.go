package handler

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

func HandleChannel(channelPath string, msg message.Message) *answer.Error {
	db, errAnswer := database.GetChannelRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("HandleChannel")
	}

	channelType, err := db.GetChannelType(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("channel type: %v", err)
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
		errAnswer = answer.NewInvalidResourceError("unknown channel type for %s", channelPath)
	}

	if errAnswer != nil {
		return errAnswer.Wrap("HandleChannel")
	}

	return nil
}

// util for the channels

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
