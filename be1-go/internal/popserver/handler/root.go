package handler

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

const (
	rootChannel = "/root"
	RootPrefix  = "/root/"
	Social      = "/social"
	Chirps      = "/chirps"
	Reactions   = "/reactions"
	Consensus   = "/consensus"
	Coin        = "/coin"
	Auth        = "/authentication"
)

func handleChannelRoot(channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelRoot")
	}

	storeMessage := true
	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionCreate:
		storeMessage = false
		errAnswer = handleLaoCreate(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelRoot")
	}
	if storeMessage {
		db, errAnswer := database.GetRootRepositoryInstance()
		if errAnswer != nil {
			return errAnswer.Wrap("handleChannelRoot")
		}

		err := db.StoreMessageAndData(channel, msg)
		if err != nil {
			errAnswer := answer.NewStoreDatabaseError(err.Error())
			return errAnswer.Wrap("handleChannelRoot")
		}
	}

	return nil
}

func handleLaoCreate(msg message.Message) *answer.Error {
	var laoCreate messagedata.LaoCreate
	errAnswer := msg.UnmarshalMsgData(&laoCreate)
	if errAnswer != nil {
		return errAnswer.Wrap("handleLaoCreate")
	}

	laoPath := RootPrefix + laoCreate.ID
	organizerPubBuf, errAnswer := verifyLaoCreation(msg, laoCreate, laoPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleLaoCreate")
	}
	laoGreetMsg, errAnswer := createLaoGreet(organizerPubBuf, laoCreate.ID)
	if errAnswer != nil {
		return errAnswer.Wrap("handleLaoCreate")
	}
	errAnswer = createLaoAndChannels(msg, laoGreetMsg, organizerPubBuf, laoPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleLaoCreate")
	}
	return nil
}

func verifyLaoCreation(msg message.Message, laoCreate messagedata.LaoCreate, laoPath string) ([]byte, *answer.Error) {
	db, errAnswer := database.GetRootRepositoryInstance()
	if errAnswer != nil {
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}

	ok, err := db.HasChannel(laoPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("if lao already exists: %v", err)
		return nil, errAnswer.Wrap("verifyLAOCreation")
	} else if ok {
		errAnswer := answer.NewDuplicateResourceError("failed to create lao: duplicate lao path: %s", laoPath)
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}

	err = laoCreate.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidActionError("failed to verify message data: %v", err)
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}

	senderPubBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode public key of the sender: %v", err)
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}

	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderPubBuf)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal public key of the sender: %v", err)
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}

	organizerPubBuf, err := base64.URLEncoding.DecodeString(laoCreate.Organizer)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode public key of the organizer: %v", err)
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}

	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerPubBuf)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal public key of the organizer: %v", err)
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}
	// Check if the sender and organizer fields of the create#lao message are equal
	if !organizerPubKey.Equal(senderPubKey) {
		errAnswer := answer.NewAccessDeniedError("sender's public key does not match the organizer public key: %s != %s",
			senderPubKey, organizerPubKey)
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}

	ownerPublicKey, errAnswer := config.GetOwnerPublicKeyInstance()
	if errAnswer != nil {
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}

	// Check if the sender of the LAO creation message is the owner
	if ownerPublicKey != nil && !ownerPublicKey.Equal(senderPubKey) {
		errAnswer := answer.NewAccessDeniedError("sender's public key does not match the owner public key: %s != %s",
			senderPubKey, ownerPublicKey)
		return nil, errAnswer.Wrap("verifyLAOCreation")
	}
	return organizerPubBuf, nil
}

func createLaoAndChannels(msg, laoGreetMsg message.Message, organizerPubBuf []byte, laoPath string) *answer.Error {
	channels := map[string]string{
		laoPath:                      channelLao,
		laoPath + Social + Chirps:    ChannelChirp,
		laoPath + Social + Reactions: ChannelReaction,
		laoPath + Consensus:          ChannelConsensus,
		laoPath + Coin:               ChannelCoin,
		laoPath + Auth:               ChannelAuth,
	}

	db, errAnswer := database.GetRootRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("createLaoAndSubChannels")
	}

	err := db.StoreChannelsAndMessageWithLaoGreet(channels, laoPath, organizerPubBuf, msg, laoGreetMsg)
	if err != nil {
		errAnswer := answer.NewStoreDatabaseError("lao and sub channels: %v", err)
		return errAnswer.Wrap("createLaoAndSubChannels")
	}

	for channelPath := range channels {
		errAnswer := state.AddChannel(channelPath)
		if errAnswer != nil {
			return errAnswer.Wrap("createLaoAndSubChannels")
		}
	}
	return nil
}

func createLaoGreet(organizerBuf []byte, laoID string) (message.Message, *answer.Error) {
	peersInfo, errAnswer := state.GetAllPeersInfo()
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createAndSendLaoGreet")
	}

	knownPeers := make([]messagedata.Peer, 0, len(peersInfo))
	for _, info := range peersInfo {
		knownPeers = append(knownPeers, messagedata.Peer{Address: info.ClientAddress})
	}

	_, clientServerAddress, _, errAnswer := config.GetServerInfo()
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createAndSendLaoGreet")
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
		errAnswer := answer.NewInternalServerError("failed to marshal message data: %v", err)
		return message.Message{}, errAnswer.Wrap("createAndSendLaoGreet")
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPublicKey, errAnswer := config.GetServerPublicKeyInstance()
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createAndSendLaoGreet")
	}

	// Marshall the server public key
	serverPubBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal server public key: %v", err)
		return message.Message{}, errAnswer.Wrap("createAndSendLaoGreet")
	}

	// Sign the data
	signatureBuf, errAnswer := Sign(dataBuf)
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createAndSendLaoGreet")
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	laoGreetMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	return laoGreetMsg, nil
}
