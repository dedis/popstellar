package channel

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
		errAnswer = errAnswer.Wrap("handleChannelRoot")
		return errAnswer
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
		errAnswer = errAnswer.Wrap("handleChannelRoot")
		return errAnswer
	}
	if storeMessage {
		db, ok := database.GetRootRepositoryInstance()
		if !ok {
			errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleChannelRoot")
			return errAnswer
		}

		err := db.StoreMessage(channel, msg)
		if err != nil {
			errAnswer = answer.NewInternalServerError("failed to store message in root channel: %v", err)
			errAnswer = errAnswer.Wrap("handleChannelRoot")
			return errAnswer
		}
	}

	return nil
}

func handleLaoCreate(msg message.Message) *answer.Error {
	var laoCreate messagedata.LaoCreate
	err := msg.UnmarshalData(&laoCreate)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}

	laoPath := RootPrefix + laoCreate.ID
	organizerPubBuf, errAnswer := verifyLaoCreation(msg, laoCreate, laoPath)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	laoGreetMsg, errAnswer := createLaoGreet(organizerPubBuf, laoPath)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	errAnswer = createLaoAndChannels(msg, laoGreetMsg, organizerPubBuf, laoPath)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	return nil
}

func verifyLaoCreation(msg message.Message, laoCreate messagedata.LaoCreate, laoPath string) ([]byte, *answer.Error) {
	var errAnswer *answer.Error

	db, ok := database.GetRootRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	ok, err := db.HasChannel(laoPath)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to check if lao already exists: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	} else if ok {
		errAnswer = answer.NewDuplicateResourceError("failed to create lao: duplicate lao path: %s", laoPath)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	err = laoCreate.Verify()
	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to verify message data: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	senderPubBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode public key of the sender: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderPubBuf)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to unmarshal public key of the sender: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	organizerPubBuf, err := base64.URLEncoding.DecodeString(laoCreate.Organizer)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode public key of the organizer: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerPubBuf)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to unmarshal public key of the organizer: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}
	// Check if the sender and organizer fields of the create#lao message are equal
	if !organizerPubKey.Equal(senderPubKey) {
		errAnswer = answer.NewAccessDeniedError("sender's public key does not match the organizer public key: %s != %s", senderPubKey, organizerPubKey)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	ownerPublicKey, ok := config.GetOwnerPublicKeyInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	// Check if the sender of the LAO creation message is the owner
	if ownerPublicKey != nil && !ownerPublicKey.Equal(senderPubKey) {
		errAnswer = answer.NewAccessDeniedError("sender's public key does not match the owner public key: %s != %s", senderPubKey, ownerPublicKey)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}
	return organizerPubBuf, nil
}

func createLaoAndChannels(msg, laoGreetMsg message.Message, organizerPubBuf []byte, laoPath string) *answer.Error {
	var errAnswer *answer.Error

	channels := map[string]string{
		laoPath:                      channelLao,
		laoPath + Social + Chirps:    ChannelChirp,
		laoPath + Social + Reactions: ChannelReaction,
		laoPath + Consensus:          ChannelConsensus,
		laoPath + Coin:               ChannelCoin,
		laoPath + Auth:               ChannelAuth,
	}

	db, ok := database.GetRootRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("createLaoAndSubChannels")
		return errAnswer
	}

	err := db.StoreChannelsAndMessageWithLaoGreet(channels, laoPath, organizerPubBuf, msg, laoGreetMsg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store lao and sub channels: %v", err)
		errAnswer = errAnswer.Wrap("createLaoAndSubChannels")
		return errAnswer
	}

	subs, ok := state.GetSubsInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get state").Wrap("createLaoAndSubChannels")
		return errAnswer
	}

	for _, channelPath := range channels {
		subs.AddChannel(channelPath)
	}
	return nil
}

func createLaoGreet(organizerBuf []byte, laoPath string) (message.Message, *answer.Error) {
	peers, ok := state.GetPeersInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get state").Wrap("createAndSendLaoGreet")
		return message.Message{}, errAnswer
	}

	peersInfo := peers.GetAllPeersInfo()
	knownPeers := make([]messagedata.Peer, 0, len(peersInfo))
	var errAnswer *answer.Error

	for _, info := range peersInfo {
		knownPeers = append(knownPeers, messagedata.Peer{Address: info.ClientAddress})
	}

	_, clientServerAddress, _, ok := config.GetServerInfo()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("createAndSendLaoGreet")
		return message.Message{}, errAnswer
	}

	msgData := messagedata.LaoGreet{
		Object:   messagedata.LAOObject,
		Action:   messagedata.LAOActionGreet,
		LaoID:    laoPath,
		Frontend: base64.URLEncoding.EncodeToString(organizerBuf),
		Address:  clientServerAddress,
		Peers:    knownPeers,
	}

	// Marshall the message data
	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to marshal message data: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return message.Message{}, errAnswer
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	serverPublicKey, ok := config.GetServerPublicKeyInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("createAndSendLaoGreet")
		return message.Message{}, errAnswer
	}

	// Marshall the server public key
	serverPubBuf, err := serverPublicKey.MarshalBinary()
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to marshal server public key: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return message.Message{}, errAnswer
	}

	// Sign the data
	signatureBuf, errAnswer := Sign(dataBuf)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return message.Message{}, errAnswer
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
