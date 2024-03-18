package hub

import (
	"encoding/base64"
	"encoding/json"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
)

func (h *Hub) handleSubscribe(socket socket.Socket, byteMessage []byte) (int, error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(byteMessage, &subscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal subscribe message: %v", err)
	}

	channel, err := h.getChan(subscribe.Params.Channel)
	if err != nil {
		return subscribe.ID, xerrors.Errorf("failed to get subscribe channel: %v", err)
	}

	err = channel.Subscribe(socket, subscribe)
	if err != nil {
		return subscribe.ID, xerrors.Errorf(publishError, err)
	}

	return subscribe.ID, nil
}

func (h *Hub) handleUnsubscribe(socket socket.Socket, byteMessage []byte) (int, error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(byteMessage, &unsubscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal unsubscribe message: %v", err)
	}

	channel, err := h.getChan(unsubscribe.Params.Channel)
	if err != nil {
		return unsubscribe.ID, xerrors.Errorf("failed to get unsubscribe channel: %v", err)
	}

	err = channel.Unsubscribe(socket.ID(), unsubscribe)
	if err != nil {
		return unsubscribe.ID, xerrors.Errorf("failed to unsubscribe: %v", err)
	}

	return unsubscribe.ID, nil
}

func (h *Hub) handleCatchup(socket socket.Socket,
	byteMessage []byte,
) ([]message.Message, int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	if catchup.Params.Channel == rootChannel {
		return h.handleRootCatchup(socket, byteMessage)
	}

	channel, err := h.getChan(catchup.Params.Channel)
	if err != nil {
		return nil, catchup.ID, xerrors.Errorf("failed to get catchup channel: %v", err)
	}

	msg := channel.Catchup(catchup)
	if err != nil {
		return nil, catchup.ID, xerrors.Errorf("failed to catchup: %v", err)
	}

	return msg, catchup.ID, nil
}

func (h *Hub) handleBroadcast(socket socket.Socket, byteMessage []byte) error {
	var broadcast method.Broadcast

	err := json.Unmarshal(byteMessage, &broadcast)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	signature := broadcast.Params.Message.Signature
	messageID := broadcast.Params.Message.MessageID
	data := broadcast.Params.Message.Data

	expectedMessageID := messagedata.Hash(data, signature)
	if expectedMessageID != messageID {
		return xerrors.Errorf(wrongMessageIdError,
			expectedMessageID, messageID)
	}

	_, ok := h.hubInbox.GetMessage(broadcast.Params.Message.MessageID)
	if ok {
		h.log.Info().Msg("message was already received")
		return nil
	}
	h.hubInbox.StoreMessage(broadcast.Params.Channel, broadcast.Params.Message)

	if err != nil {
		return xerrors.Errorf("failed to broadcast message: %v", err)
	}

	if broadcast.Params.Channel == rootChannel {
		err := h.handleRootChannelBroadcastMessage(socket, broadcast)
		if err != nil {
			return xerrors.Errorf(rootChannelErr, err)
		}
		return nil
	}

	channel, err := h.getChan(broadcast.Params.Channel)
	if err != nil {
		return xerrors.Errorf(getChannelErr, err)
	}

	err = channel.Broadcast(broadcast, socket)
	if err != nil {
		return xerrors.Errorf(publishError, err)
	}

	return nil
}

func (h *Hub) handlePublish(socket socket.Socket, byteMessage []byte) (int, error) {
	var publish method.Publish

	err := json.Unmarshal(byteMessage, &publish)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	signature := publish.Params.Message.Signature
	messageID := publish.Params.Message.MessageID
	data := publish.Params.Message.Data

	dataBytes, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return publish.ID, xerrors.Errorf("failed to decode data string: %v", err)
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(publish.Params.Message.Sender)
	if err != nil {
		h.log.Info().Msg("Sender is : " + publish.Params.Message.Sender)
		return publish.ID, answer.NewInvalidMessageFieldError("failed to decode public key string: %v", err)
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(signature)
	if err != nil {
		return publish.ID, answer.NewInvalidMessageFieldError("failed to decode signature string: %v", err)
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		return publish.ID, answer.NewInvalidMessageFieldError("failed to verify signature : %v", err)
	}

	expectedMessageID := messagedata.Hash(data, signature)
	if expectedMessageID != messageID {
		return publish.ID, answer.NewInvalidMessageFieldError(wrongMessageIdError,
			expectedMessageID, messageID)
	}

	if publish.Params.Channel == rootChannel {
		err := h.handleRootChannelPublishMessage(socket, publish)
		if err != nil {
			return publish.ID, err
		}
		h.hubInbox.StoreMessage(publish.Params.Channel, publish.Params.Message)
		return publish.ID, nil
	}

	_, alreadyReceived := h.hubInbox.GetMessage(publish.Params.Message.MessageID)
	if alreadyReceived {
		return publish.ID, xerrors.Errorf("message %s was already received", publish.Params.Message.MessageID)
	}

	channel, err := h.getChan(publish.Params.Channel)
	if err != nil {
		return publish.ID, answer.NewInvalidMessageFieldError(getChannelErr, err)
	}

	err = channel.Publish(publish, socket)
	if err != nil {
		return publish.ID, answer.NewInvalidMessageFieldError(publishError, err)
	}

	h.hubInbox.StoreMessage(publish.Params.Channel, publish.Params.Message)
	return publish.ID, nil
}

// handleRootChannelPublishMessage handles an incoming publish message on the root channel.
func (h *Hub) handleRootChannelPublishMessage(sock socket.Socket, publish method.Publish) error {
	jsonData, err := base64.URLEncoding.DecodeString(publish.Params.Message.Data)
	if err != nil {
		err := answer.NewInvalidMessageFieldError("failed to decode message data: %v", err)

		return err
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		err := answer.NewInvalidMessageFieldError("failed to validate message against json schema: %v", err)
		return err
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		err := answer.NewInvalidMessageFieldError("failed to get object#action: %v", err)
		return err
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		err := answer.NewInvalidMessageFieldError("only lao#create is allowed on root, "+
			"but found %s#%s", object, action)
		return err
	}

	var laoCreate messagedata.LaoCreate

	err = publish.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		return err
	}

	err = laoCreate.Verify()
	if err != nil {
		h.log.Err(err).Msg("invalid lao#create message " + err.Error())
		return err
	}

	err = h.createLao(publish.Params.Message, laoCreate, sock)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		return err
	}

	h.hubInbox.StoreMessage(publish.Params.Channel, publish.Params.Message)
	return nil
}

// handleRootChannelPublishMessage handles an incoming publish message on the root channel.
func (h *Hub) handleRootChannelBroadcastMessage(sock socket.Socket,
	broadcast method.Broadcast,
) error {
	jsonData, err := base64.URLEncoding.DecodeString(broadcast.Params.Message.Data)
	if err != nil {
		err := xerrors.Errorf("failed to decode message data: %v", err)
		sock.SendError(nil, err)
		return err
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		err := xerrors.Errorf("failed to validate message against json schema: %v", err)
		sock.SendError(nil, err)
		return err
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		err := xerrors.Errorf("failed to get object#action: %v", err)
		sock.SendError(nil, err)
		return err
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		err := xerrors.Errorf("only lao#create is allowed on root, but found %s#%s",
			object, action)
		sock.SendError(nil, err)
		return err
	}

	var laoCreate messagedata.LaoCreate

	err = broadcast.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		sock.SendError(nil, err)
		return err
	}

	err = laoCreate.Verify()
	if err != nil {
		h.log.Err(err).Msg("invalid lao#create message")
		sock.SendError(nil, err)
		return err
	}

	err = h.createLao(broadcast.Params.Message, laoCreate, sock)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		sock.SendError(nil, err)
		return err
	}

	h.hubInbox.StoreMessage(broadcast.Params.Channel, broadcast.Params.Message)
	return nil
}

// handleRootCatchup handles an incoming catchup message on the root channel
func (h *Hub) handleRootCatchup(senderSocket socket.Socket,
	byteMessage []byte,
) ([]message.Message, int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	if catchup.Params.Channel != rootChannel {
		return nil, catchup.ID, xerrors.Errorf("server catchup message can only " +
			"be sent on /root channel")
	}

	messages := h.hubInbox.GetRootMessages()

	return messages, catchup.ID, nil
}

//-----------------------Helper methods for message handling---------------------------

// createLao creates a new LAO using the data in the publish parameter.
func (h *Hub) createLao(msg message.Message, laoCreate messagedata.LaoCreate,
	socket socket.Socket,
) error {
	laoChannelPath := rootPrefix + laoCreate.ID

	if _, ok := h.channelByID.Get(laoChannelPath); ok {
		return answer.NewDuplicateResourceError("failed to create lao: duplicate lao path: %q", laoChannelPath)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode public key of the sender: %v", err)
	}

	// Check if the sender of the LAO creation message is the organizer
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

	// Check if the sender of the LAO creation message is the owner
	if h.GetPubKeyOwner() != nil && !h.GetPubKeyOwner().Equal(senderPubKey) {
		return answer.NewAccessDeniedError("sender's public key does not match the owner's: %q != %q", senderPubKey, h.GetPubKeyOwner())
	}

	laoCh, err := h.laoFac(laoChannelPath, h, msg, h.log, senderPubKey, socket)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to create the LAO: %v", err)
	}

	h.log.Info().Msgf("storing new channel '%s' %v", laoChannelPath, msg)

	h.NotifyNewChannel(laoChannelPath, laoCh, socket)

	return nil
}
