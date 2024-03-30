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
	social    = "/social/"
	chirps    = "chirps"
	reactions = "reactions"
	consensus = "/consensus"
	coin      = "/coin"
	auth      = "/authentication"
)

func handleChannelRoot(params handlerParameters, msg message.Message) *answer.Error {
	object, action, err := verifyDataAndGetObjectAction(params, msg)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to verify message and get object action: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelRoot")
	}

	switch object + "#" + action {
	case messagedata.LAOObject + "#" + messagedata.LAOActionCreate:
		errAnswer = handleLaoCreate(msg, params)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelRoot")
		return errAnswer
	}
	return nil
}

func handleLaoCreate(msg message.Message, params handlerParameters) *answer.Error {
	var laoCreate messagedata.LaoCreate
	err := msg.UnmarshalData(&laoCreate)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message: %v", err)
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}

	laoPath := rootPrefix + laoCreate.ID
	organizerPubBuf, errAnswer := verifyLAOCreation(msg, laoCreate, laoPath, params)
	if err != nil {
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	errAnswer = createLao(laoPath, organizerPubBuf, msg, params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleLaoCreate")
		return errAnswer
	}
	return nil
}

func verifyLAOCreation(msg message.Message, laoCreate messagedata.LaoCreate, laoPath string, params handlerParameters) ([]byte, *answer.Error) {
	err := laoCreate.Verify()
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to verify message data: %v", err)
		errAnswer = errAnswer.Wrap("verifyLAOCreation")
		return nil, errAnswer
	}

	ok, err := params.db.Haslao(laoPath)
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

func createLao(laoPath string, organizerBuf []byte, msg message.Message, params handlerParameters) *answer.Error {
	err := params.db.StoreChannel(laoPath, organizerBuf)
	var errAnswer *answer.Error
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store lao channel: %v", err)
		errAnswer = errAnswer.Wrap("createLao")
		return errAnswer
	}

	err = params.db.StoreMessage(laoPath, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store lao#create message in lao channel: %v", err)
		errAnswer = errAnswer.Wrap("createLao")
		return errAnswer
	}

	generalChirpPath := laoPath + social + chirps
	errAnswer = createSubChannel(generalChirpPath, organizerBuf, params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("createLao")
		return errAnswer
	}
	reactionsPath := laoPath + social + reactions
	errAnswer = createSubChannel(reactionsPath, organizerBuf, params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("createLao")
		return errAnswer
	}
	consensusPath := laoPath + consensus
	errAnswer = createSubChannel(consensusPath, organizerBuf, params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("createLao")
		return errAnswer
	}

	errAnswer = createAndSendLaoGreet(laoPath, organizerBuf, params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("failed to create and send lao#greet message")
		return errAnswer
	}

	coinPath := laoPath + coin
	errAnswer = createSubChannel(coinPath, organizerBuf, params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("createLao")
		return errAnswer

	}
	authPath := laoPath + auth
	errAnswer = createSubChannel(authPath, organizerBuf, params)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("createLao")
		return errAnswer
	}
	params.subs.addChannel(laoPath)
	return nil
}

func createSubChannel(channelPath string, organizerBuf []byte, params handlerParameters) *answer.Error {
	err := params.db.StoreChannel(channelPath, organizerBuf)
	if err != nil {
		return answer.NewInternalServerError("failed to create %s channel: %v ", channelPath, err)
	}
	params.subs.addChannel(channelPath)
	return nil
}

func createAndSendLaoGreet(laoPath string, organizerBuf []byte, params handlerParameters) *answer.Error {
	peersInfo := params.peers.GetAllPeersInfo()
	peers := make([]messagedata.Peer, 0, len(peersInfo))
	var errAnswer *answer.Error

	for _, info := range peersInfo {
		peers = append(peers, messagedata.Peer{Address: info.ClientAddress})
	}

	clientServerAddress, err := params.db.GetClientServerAddress()
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get client server address: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return errAnswer
	}

	msgData := messagedata.LaoGreet{
		Object:   messagedata.LAOObject,
		Action:   messagedata.LAOActionGreet,
		LaoID:    laoPath,
		Frontend: base64.URLEncoding.EncodeToString(organizerBuf),
		Address:  clientServerAddress,
		Peers:    peers,
	}

	// Marshalls the message data
	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to marshal message data: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return errAnswer
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	// Marshalls the server public key
	serverPubBuf, err := params.db.GetServerPubKey()
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get server public key: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return errAnswer
	}

	// Sign the data
	signatureBuf, err := Sign(dataBuf, params)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to sign the message data: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return errAnswer
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	laoGreetMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	errAnswer = broadcastToAllClients(laoGreetMsg, params, laoPath)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return errAnswer
	}

	err = params.db.StoreMessage(laoPath, laoGreetMsg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store lao#greet message in lao channel: %v", err)
		errAnswer = errAnswer.Wrap("createAndSendLaoGreet")
		return errAnswer
	}

	return nil
}
