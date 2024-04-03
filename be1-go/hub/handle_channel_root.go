package hub

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/crypto"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

const (
	social    = "/social"
	chirps    = "/chirps"
	reactions = "/reactions"
	consensus = "/consensus"
	coin      = "/coin"
	auth      = "/authentication"
)

func handleChannelRoot(params handlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelRoot")
		return errAnswer
	}

	storeMessage := true
	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionCreate:
		storeMessage = false
		errAnswer = handleLaoCreate(msg, channel, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelRoot")
		return errAnswer
	}

	if storeMessage {
		err := params.db.StoreMessage(channel, msg)
		if err != nil {
			errAnswer = answer.NewInternalServerError("failed to store message in root channel: %v", err)
			errAnswer = errAnswer.Wrap("handleChannelRoot")
			return errAnswer
		}
	}
	return nil
}

func handleLaoCreate(msg message.Message, channel string, params handlerParameters) *answer.Error {
	var laoCreate messagedata.LaoCreate
	err := msg.UnmarshalData(&laoCreate)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}

	laoPath := rootPrefix + laoCreate.ID
	organizerPubBuf, errAnswer := verifyLaoCreation(params, msg, laoCreate, laoPath)
	if err != nil {
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	laoGreetMsg, errAnswer := createLaoGreet(params, organizerPubBuf, laoPath)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	errAnswer = createLaoAndSubChannels(params, channel, msg, laoGreetMsg, organizerPubBuf, laoPath)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	errAnswer = broadcastToAllClients(laoGreetMsg, params, laoPath)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	return nil
}

func verifyLaoCreation(params handlerParameters, msg message.Message, laoCreate messagedata.LaoCreate, laoPath string) ([]byte, *answer.Error) {
	err := laoCreate.Verify()
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to verify message data: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	ok, err := params.db.HasChannel(laoPath)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to check if lao already exists: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	} else if ok {
		errAnswer = answer.NewDuplicateResourceError("failed to create lao: duplicate lao path: %s", laoPath)
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
	// Check if the sender and organizer fields of the create lao message are equal
	if !organizerPubKey.Equal(senderPubKey) {
		errAnswer = answer.NewAccessDeniedError("sender's public key does not match the organizer public key: %s != %s", senderPubKey, organizerPubKey)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	ownerPubKey, err := params.db.GetOwnerPubKey()
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get owner's public key: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	// Check if the sender of the LAO creation message is the owner
	if ownerPubKey != nil && !ownerPubKey.Equal(senderPubKey) {
		errAnswer = answer.NewAccessDeniedError("sender's public key does not match the owner's: %s != %s", senderPubKey, ownerPubKey)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}
	return organizerPubBuf, nil
}

func createLaoAndSubChannels(params handlerParameters, channel string, msg, laoGreetMsg message.Message, organizerPubBuf []byte, laoPath string) *answer.Error {
	var errAnswer *answer.Error
	channelSlice := make([]string, 0, 6)
	channelSlice = append(channelSlice, laoPath)
	channelSlice = append(channelSlice, laoPath+social+chirps)
	channelSlice = append(channelSlice, laoPath+social+reactions)
	channelSlice = append(channelSlice, laoPath+consensus)
	channelSlice = append(channelSlice, laoPath+coin)
	channelSlice = append(channelSlice, laoPath+auth)

	err := params.db.StoreChannelsAndMessageWithLaoGreet(channelSlice, channel, laoPath, msg.MessageID, organizerPubBuf, msg, laoGreetMsg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store lao and sub channels: %v", err)
		errAnswer = errAnswer.Wrap("createLaoAndSubChannels")
	}

	for _, channelPath := range channelSlice {
		params.subs.addChannel(channelPath)
	}
	return nil
}

func createLaoGreet(params handlerParameters, organizerBuf []byte, laoPath string) (message.Message, *answer.Error) {
	peersInfo := params.peers.GetAllPeersInfo()
	peers := make([]messagedata.Peer, 0, len(peersInfo))
	var errAnswer *answer.Error

	for _, info := range peersInfo {
		peers = append(peers, messagedata.Peer{Address: info.ClientAddress})
	}

	msgData := messagedata.LaoGreet{
		Object:   messagedata.LAOObject,
		Action:   messagedata.LAOActionGreet,
		LaoID:    laoPath,
		Frontend: base64.URLEncoding.EncodeToString(organizerBuf),
		Address:  params.clientServerAddress,
		Peers:    peers,
	}

	// Marshall the message data
	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to marshal message data: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return message.Message{}, errAnswer
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	// Marshall the server public key
	serverPubBuf, err := params.db.GetServerPubKey()
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get server public key: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return message.Message{}, errAnswer
	}

	// Sign the data
	signatureBuf, errAnswer := Sign(dataBuf, params)
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
