package channel

import (
	"encoding/base64"
	"encoding/json"

	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"

	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	jsonrpc "popstellar/internal/message"
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

func HandleChannel(channelPath string, msg message.Message, fromRumor bool) error {
	err := msg.VerifyMessage()
	if err != nil {
		return err
	}

	db, err := database.GetChannelRepositoryInstance()
	if err != nil {
		return err
	}

	msgAlreadyExists, err := db.HasMessage(msg.MessageID)
	if err != nil {
		return err
	}
	if msgAlreadyExists && fromRumor {
		return nil
	}
	if msgAlreadyExists {
		return errors.NewDuplicateResourceError("message %s was already received", msg.MessageID)
	}

	channelType, err := db.GetChannelType(channelPath)
	if err != nil {
		return err
	}

	switch channelType {
	case sqlite.RootType:
		err = handleChannelRoot(msg)
	case sqlite.LaoType:
		err = handleChannelLao(channelPath, msg)
	case sqlite.ElectionType:
		err = handleChannelElection(channelPath, msg)
	case sqlite.ChirpType:
		err = handleChannelChirp(channelPath, msg)
	case sqlite.ReactionType:
		err = handleChannelReaction(channelPath, msg)
	case sqlite.CoinType:
		err = handleChannelCoin(channelPath, msg)
	case sqlite.FederationType:
		err = handleChannelFederation(channelPath, msg)
	default:
		err = errors.NewInvalidResourceError("unknown channelPath type for %s", channelPath)
	}

	return err
}

// util for the channels

func verifyDataAndGetObjectAction(msg message.Message) (string, string, error) {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return "", "", errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	// validate message data against the json schema
	err = utils.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return "", "", err
	}

	return messagedata.GetObjectAndAction(jsonData)
}

func sign(data []byte) ([]byte, error) {
	serverSecretKey, err := config.GetServerSecretKeyInstance()
	if err != nil {
		return nil, err
	}

	signatureBuf, err := schnorr.Sign(crypto.Suite, serverSecretKey, data)
	if err != nil {
		return nil, errors.NewInternalServerError("failed to sign the data: %v", err)
	}

	return signatureBuf, nil
}

// generateKeys generates and returns a key pair
func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	return point, secret
}

func broadcastToAllClients(msg message.Message, channel string) error {
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
		return errors.NewJsonMarshalError("broadcast query: %v", err)
	}

	err = state.SendToAll(buf, channel)
	if err != nil {
		return err
	}

	return nil
}
