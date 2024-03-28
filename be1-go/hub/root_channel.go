package hub

import (
	"encoding/base64"
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
)

const (
	social    = "/social/"
	chirps    = "chirps"
	reactions = "reactions"
	consensus = "/consensus"
	coin      = "/coin"
	auth      = "/authentication"
)

func HandleRootMessage(msg message.Message, params handlerParameters) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		err := answer.NewInvalidMessageFieldError("failed to decode message data: %v", err)
		return err
	}

	// validate message data against the json schema
	err = params.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to validate message against json schema: %v", err)
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to get object#action: %v", err)
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		return answer.NewInvalidMessageFieldError("only lao#create is allowed on root, "+
			"but found %s#%s", object, action)
	}

	var laoCreate messagedata.LaoCreate

	err = msg.UnmarshalData(&laoCreate)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal lao#create: %v", err)
	}

	err = laoCreate.Verify()
	if err != nil {
		return xerrors.Errorf("invalid lao#create message: %v", err)
	}

	err = createLao(msg, laoCreate, params)
	if err != nil {
		return xerrors.Errorf("failed to create lao: %v", err)
	}

	if err = params.db.StoreMessage(rootPrefix, msg); err != nil {
		return xerrors.Errorf("failed to store lao#create message in root channel: %v", err)
	}
	return nil
}

// createLao creates a new LAO
func createLao(msg message.Message, laoCreate messagedata.LaoCreate, params handlerParameters) error {
	laoChannelPath := rootPrefix + laoCreate.ID

	ok, err := params.db.Haslao(laoChannelPath)
	if err != nil {
		return xerrors.Errorf("failed to check if lao already exists: %v", err)
	} else if ok {
		return answer.NewDuplicateResourceError("failed to create lao: duplicate lao path: %q", laoChannelPath)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode public key of the sender: %v", err)
	}

	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to unmarshal public key of the sender: %v", err)
	}

	organizerBuf, err := base64.URLEncoding.DecodeString(laoCreate.Organizer)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode public key of the organizer: %v", err)
	}

	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerBuf)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to unmarshal public key of the organizer: %v", err)
	}

	// Check if the sender and organizer fields of the create lao message are equal
	if !organizerPubKey.Equal(senderPubKey) {
		return answer.NewAccessDeniedError("sender's public key does not match the organizer field: %q != %q", senderPubKey, organizerPubKey)
	}

	ownerPubKey, err := params.db.GetOwnerPubKey()
	if err != nil {
		return xerrors.Errorf("failed to get owner's public key: %v", err)
	}

	// Check if the sender of the LAO creation message is the owner
	if ownerPubKey != nil && !ownerPubKey.Equal(senderPubKey) {
		return answer.NewAccessDeniedError("sender's public key does not match the owner's: %q != %q", senderPubKey, ownerPubKey)
	}

	// Create the LAO channel
	err = createLaoChannel(laoChannelPath, organizerBuf, msg, params)
	if err != nil {
		return xerrors.Errorf("failed to create lao channel: %v", err)
	}
	return nil
}

func createLaoChannel(laoChannelPath string, organizerBuf []byte, msg message.Message, params handlerParameters) error {
	err := params.db.StoreChannel(laoChannelPath, organizerBuf)
	if err != nil {
		return xerrors.Errorf("failed to store lao channel: %v", err)
	}

	err = params.db.StoreMessage(laoChannelPath, msg)
	if err != nil {
		return xerrors.Errorf("failed to store lao#create message in lao channel: %v", err)

	}

	generalChannelPath := laoChannelPath + social + chirps
	err = createChannel(generalChannelPath, organizerBuf, params)
	if err != nil {
		return xerrors.Errorf("failed to create general chirping channel: %v", err)
	}
	reactionsChannelPath := laoChannelPath + social + reactions
	err = createChannel(reactionsChannelPath, organizerBuf, params)
	if err != nil {
		return xerrors.Errorf("failed to create reactions channel: %v", err)
	}
	consensusChannelPath := laoChannelPath + consensus
	err = createChannel(consensusChannelPath, organizerBuf, params)
	if err != nil {
		return xerrors.Errorf("failed to create consensus channel: %v", err)
	}
	err = createAndSendLaoGreet(laoChannelPath, organizerBuf, params)
	if err != nil {
		return xerrors.Errorf("failed to create and send lao#greet message: %v", err)

	}
	coinChannelPath := laoChannelPath + coin
	err = createChannel(coinChannelPath, organizerBuf, params)
	if err != nil {
		return xerrors.Errorf("failed to create coin channel: %v", err)

	}
	authChannelPath := laoChannelPath + auth
	err = createChannel(authChannelPath, organizerBuf, params)
	if err != nil {
		return xerrors.Errorf("failed to create authentication channel: %v", err)

	}

	params.subs[laoChannelPath] = make(map[socket.Socket]struct{})
	return nil

}

func createChannel(channelPath string, organizerBuf []byte, params handlerParameters) error {
	err := params.db.StoreChannel(channelPath, organizerBuf)
	if err != nil {
		return xerrors.Errorf("failed to store channel: %v", err)
	}
	params.subs[channelPath] = make(map[socket.Socket]struct{})
	return nil
}

func createAndSendLaoGreet(laoChannelPath string, organizerBuf []byte, params handlerParameters) error {
	peersInfo := params.peers.GetAllPeersInfo()
	peers := make([]messagedata.Peer, 0, len(peersInfo))

	for _, info := range peersInfo {
		peers = append(peers, messagedata.Peer{Address: info.ClientAddress})
	}

	clientServerAddress, err := params.db.GetClientServerAddress()
	if err != nil {
		return xerrors.Errorf("failed to get client server address: %v", err)
	}

	msgData := messagedata.LaoGreet{
		Object:   messagedata.LAOObject,
		Action:   messagedata.LAOActionGreet,
		LaoID:    laoChannelPath,
		Frontend: base64.URLEncoding.EncodeToString(organizerBuf),
		Address:  clientServerAddress,
		Peers:    peers,
	}

	// Marshalls the message data
	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		return xerrors.Errorf("failed to marshal the data: %v", err)
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	// Marshalls the server public key
	serverPubBuf, err := params.db.GetServerPubKey()
	if err != nil {
		return xerrors.Errorf("failed to get server public key: %v", err)
	}

	// Sign the data
	signatureBuf, err := Sign(dataBuf, params)
	if err != nil {
		return xerrors.Errorf("failed to sign the data: %v", err)
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	laoGreetMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	err = broadcastToAllClients(laoGreetMsg, params, laoChannelPath)
	if err != nil {
		return xerrors.Errorf("failed to broadcast greeting message: %v", err)
	}

	err = params.db.StoreMessage(laoChannelPath, laoGreetMsg)
	if err != nil {
		return xerrors.Errorf("failed to store lao#greet message in lao channel: %v", err)
	}

	return nil
}

func broadcastToAllClients(msg message.Message, params handlerParameters, channel string) error {
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
		return xerrors.Errorf("failed to marshal broadcast query: %v", err)
	}

	err = SendToAll(params.subs, buf, channel)
	if err != nil {
		return xerrors.Errorf("failed to send broadcast message to all clients: %v", err)
	}

	return nil
}
