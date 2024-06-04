package channel

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/utils"
	"popstellar/internal/validation"
)

const (
	RootType         = "root"
	LaoType          = "lao"
	ElectionType     = "election"
	ChirpType        = "chirp"
	ReactionType     = "reaction"
	ConsensusType    = "consensus"
	CoinType         = "coin"
	AuthType         = "auth"
	PopChaType       = "popcha"
	GeneralChirpType = "generalChirp"
	FederationType   = "federation"
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
	case RootType:
		err = handleChannelRoot(msg)
	case LaoType:
		err = handleChannelLao(channelPath, msg)
	case ElectionType:
		err = handleChannelElection(channelPath, msg)
	case ChirpType:
		err = handleChannelChirp(channelPath, msg)
	case ReactionType:
		err = handleChannelReaction(channelPath, msg)
	case CoinType:
		err = handleChannelCoin(channelPath, msg)
	case FederationType:
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

// generateKeys generates and returns a key pair
func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)
	return point, secret
}
