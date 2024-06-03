package channel

import (
	"encoding/base64"
	"encoding/json"

	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
	"popstellar/internal/sqlite"
)

const (
	Root       = "/root"
	RootPrefix = "/root/"
	Social     = "/social"
	Chirps     = "/chirps"
	Reactions  = "/reactions"
	Consensus  = "/consensus"
	Coin       = "/coin"
	Auth       = "/authentication"
	Federation = "/federation"
)

func handleChannelRoot(msg message.Message) error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return err
	}

	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionCreate:
		err = handleLaoCreate(msg)
	default:
		err = errors.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	return err
}

func handleLaoCreate(msg message.Message) error {
	var laoCreate messagedata.LaoCreate
	err := msg.UnmarshalData(&laoCreate)
	if err != nil {
		return err
	}

	laoPath := RootPrefix + laoCreate.ID

	organizerPubBuf, err := verifyLaoCreation(msg, laoCreate, laoPath)
	if err != nil {
		return err
	}

	laoGreetMsg, err := createLaoGreet(organizerPubBuf, laoCreate.ID)
	if err != nil {
		return err
	}

	return createLaoAndChannels(msg, laoGreetMsg, organizerPubBuf, laoPath)
}

func verifyLaoCreation(msg message.Message, laoCreate messagedata.LaoCreate, laoPath string) ([]byte, error) {
	db, err := database.GetRootRepositoryInstance()
	if err != nil {
		return nil, err
	}

	ok, err := db.HasChannel(laoPath)
	if err != nil {
		return nil, err
	} else if ok {
		return nil, errors.NewDuplicateResourceError("duplicate lao path: %s", laoPath)
	}

	err = laoCreate.Verify()
	if err != nil {
		return nil, err
	}

	senderPubBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return nil, errors.NewInvalidMessageFieldError("failed to decode public key of the sender: %v", err)
	}

	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderPubBuf)
	if err != nil {
		return nil, errors.NewInvalidMessageFieldError("failed to unmarshal public key of the sender: %v", err)
	}

	organizerPubBuf, err := base64.URLEncoding.DecodeString(laoCreate.Organizer)
	if err != nil {
		return nil, errors.NewInvalidMessageFieldError("failed to decode public key of the organizer: %v", err)
	}

	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerPubBuf)
	if err != nil {
		return nil, errors.NewInvalidMessageFieldError("failed to unmarshal public key of the organizer: %v", err)
	}
	// Check if the sender and organizer fields of the create#lao message are equal
	if !organizerPubKey.Equal(senderPubKey) {
		return nil, errors.NewAccessDeniedError("sender's public key does not match the organizer public key: %s != %s",
			senderPubKey, organizerPubKey)
	}

	ownerPublicKey, err := config.GetOwnerPublicKeyInstance()
	if err != nil {
		return nil, err
	}

	// Check if the sender of the LAO creation message is the owner
	if ownerPublicKey != nil && !ownerPublicKey.Equal(senderPubKey) {
		return nil, errors.NewAccessDeniedError("sender's public key does not match the owner public key: %s != %s",
			senderPubKey, ownerPublicKey)
	}

	return organizerPubBuf, nil
}

func createLaoAndChannels(msg, laoGreetMsg message.Message, organizerPubBuf []byte, laoPath string) error {
	channels := map[string]string{
		laoPath:                      sqlite.LaoType,
		laoPath + Social + Chirps:    sqlite.ChirpType,
		laoPath + Social + Reactions: sqlite.ReactionType,
		laoPath + Consensus:          sqlite.ConsensusType,
		laoPath + Coin:               sqlite.CoinType,
		laoPath + Auth:               sqlite.AuthType,
		laoPath + Federation:         sqlite.FederationType,
	}

	db, err := database.GetRootRepositoryInstance()
	if err != nil {
		return err
	}

	err = db.StoreLaoWithLaoGreet(channels, laoPath, organizerPubBuf, msg, laoGreetMsg)
	if err != nil {
		return err
	}

	for channelPath := range channels {
		err = state.AddChannel(channelPath)
		if err != nil {
			return err
		}
	}

	return nil
}

func createLaoGreet(organizerBuf []byte, laoID string) (message.Message, error) {
	peersInfo, err := state.GetAllPeersInfo()
	if err != nil {
		return message.Message{}, err
	}

	knownPeers := make([]messagedata.Peer, 0, len(peersInfo))
	for _, info := range peersInfo {
		knownPeers = append(knownPeers, messagedata.Peer{Address: info.ClientAddress})
	}

	_, clientServerAddress, _, err := config.GetServerInfo()
	if err != nil {
		return message.Message{}, err
	}

	msgData := messagedata.LaoGreet{
		Object:   messagedata.LAOObject,
		Action:   messagedata.LAOActionGreet,
		LaoID:    laoID,
		Frontend: base64.URLEncoding.EncodeToString(organizerBuf),
		Address:  clientServerAddress,
		Peers:    knownPeers,
	}

	// Marshall the message data
	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		return message.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPublicKey, err := config.GetServerPublicKeyInstance()
	if err != nil {
		return message.Message{}, err
	}

	// Marshall the server public key
	serverPubBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewInternalServerError("failed to marshal server public key: %v", err)
	}

	// sign the data
	signatureBuf, err := sign(dataBuf)
	if err != nil {
		return message.Message{}, err
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	laoGreetMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         message.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	return laoGreetMsg, nil
}
