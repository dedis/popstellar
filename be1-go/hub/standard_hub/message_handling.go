package standard_hub

import (
	"encoding/base64"
	"encoding/json"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"

	"golang.org/x/xerrors"
)

const publishError = "failed to publish: %v"

// handleRootChannelPublishMesssage handles an incominxg publish message on the root channel.
func (h *Hub) handleRootChannelPublishMesssage(sock socket.Socket, publish method.Publish) error {
	jsonData, err := base64.URLEncoding.DecodeString(publish.Params.Message.Data)
	if err != nil {
		err := xerrors.Errorf("failed to decode message data: %v", err)
		sock.SendError(&publish.ID, err)
		return err
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		err := xerrors.Errorf("failed to validate message against json schema: %v", err)
		sock.SendError(&publish.ID, err)
		return err
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		err := xerrors.Errorf("failed to get object#action: %v", err)
		sock.SendError(&publish.ID, err)
		return err
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		err := answer.NewErrorf(publish.ID, "only lao#create is allowed on root, "+
			"but found %s#%s", object, action)
		sock.SendError(&publish.ID, err)
		return err
	}

	var laoCreate messagedata.LaoCreate

	err = publish.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		sock.SendError(&publish.ID, err)
		return err
	}

	err = laoCreate.Verify()
	if err != nil {
		h.log.Err(err).Msg("invalid lao#create message")
		sock.SendError(&publish.ID, err)
	}

	err = h.createLao(publish.Params.Message, laoCreate, sock)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		sock.SendError(&publish.ID, err)
		return err
	}

	h.rootInbox.StoreMessage(publish.Params.Message)

	return nil
}

// handleRootChannelPublishMesssage handles an incominxg publish message on the root channel.
func (h *Hub) handleRootChannelBroadcastMesssage(sock socket.Socket,
	broadcast method.Broadcast) error {

	id := -1

	jsonData, err := base64.URLEncoding.DecodeString(broadcast.Params.Message.Data)
	if err != nil {
		err := xerrors.Errorf("failed to decode message data: %v", err)
		sock.SendError(&id, err)
		return err
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		err := xerrors.Errorf("failed to validate message against json schema: %v", err)
		sock.SendError(&id, err)
		return err
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		err := xerrors.Errorf("failed to get object#action: %v", err)
		sock.SendError(&id, err)
		return err
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		err := answer.NewErrorf(id, "only lao#create is allowed on root, but found %s#%s",
			object, action)
		sock.SendError(&id, err)
		return err
	}

	var laoCreate messagedata.LaoCreate

	err = broadcast.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		sock.SendError(&id, err)
		return err
	}

	err = laoCreate.Verify()
	if err != nil {
		h.log.Err(err).Msg("invalid lao#create message")
		sock.SendError(&id, err)
		return err
	}

	err = h.createLao(broadcast.Params.Message, laoCreate, sock)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		sock.SendError(&id, err)
		return err
	}

	h.rootInbox.StoreMessage(broadcast.Params.Message)

	return nil
}

// handleRootCatchup handles an incoming catchup message on the root channel
func (h *Hub) handleRootCatchup(senderSocket socket.Socket,
	byteMessage []byte) ([]message.Message, int, error) {

	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	if catchup.Params.Channel != rootChannel {
		return nil, catchup.ID, xerrors.Errorf("server catchup message can only " +
			"be sent on /root channel")
	}

	messages := h.rootInbox.GetSortedMessages()

	return messages, catchup.ID, nil
}

// handleAnswer handles the answer to a message sent by the server
func (h *Hub) handleAnswer(senderSocket socket.Socket, byteMessage []byte) error {
	var answerMsg answer.Answer

	err := json.Unmarshal(byteMessage, &answerMsg)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal answer: %v", err)
	}

	if answerMsg.Result == nil {
		h.log.Warn().Msg("received an error, nothing to handle")
		// don't send any error to avoid infinite error loop as a server will
		// send an error to another server that will create another error
		return nil
	}

	if answerMsg.Result.IsEmpty() {
		h.log.Info().Msg("result isn't an answer to a catchup, nothing to handle")
		return nil
	}

	h.queries.Lock()

	val := h.queries.state[*answerMsg.ID]
	if val == nil {
		h.queries.Unlock()
		return xerrors.Errorf("no query sent with id %v", answerMsg.ID)
	}

	if *val {
		h.queries.Unlock()
		return xerrors.Errorf("query %v already got an answer", answerMsg.ID)
	}

	channel := h.queries.queries[*answerMsg.ID].Params.Channel
	*h.queries.state[*answerMsg.ID] = true
	h.queries.Unlock()

	messages := answerMsg.Result.GetData()
	for msg := range messages {

		var messageData message.Message
		err = json.Unmarshal(messages[msg], &messageData)
		if err != nil {
			h.log.Error().Msgf("failed to unmarshal message during catchup: %v", err)
			continue
		}

		publish := method.Publish{
			Base: query.Base{
				JSONRPCBase: jsonrpc.JSONRPCBase{
					JSONRPC: "2.0",
				},
				Method: "publish",
			},

			Params: struct {
				Channel string          `json:"channel"`
				Message message.Message `json:"message"`
			}{
				Channel: channel,
				Message: messageData,
			},
		}

		err := h.handleDuringCatchup(senderSocket, publish)
		if err != nil {
			h.log.Error().Msgf("failed to handle message during catchup: %v", err)
		}
	}

	return nil
}

// handleDuringCatchup handle a message obtained by the server catching up
func (h *Hub) handleDuringCatchup(socket socket.Socket, publish method.Publish) error {
	h.Lock()
	_, stored := h.hubInbox.GetMessage(publish.Params.Message.MessageID)
	if stored {
		h.Unlock()
		return xerrors.Errorf("already stored this message")
	}
	h.hubInbox.StoreMessage(publish.Params.Message)
	h.Unlock()

	if publish.Params.Channel == rootChannel {
		err := h.handleRootChannelPublishMesssage(socket, publish)
		if err != nil {
			return xerrors.Errorf(rootChannelErr, err)
		}
		return nil
	}

	channel, err := h.getChan(publish.Params.Channel)
	if err != nil {
		return xerrors.Errorf(getChannelErr, err)
	}

	err = channel.Publish(publish, socket)
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

	expectedMessageID := messagedata.Hash(data, signature)
	if expectedMessageID != messageID {
		return publish.ID, xerrors.Errorf("message_id is wrong: expected %q found %q",
			expectedMessageID, messageID)
	}

	alreadyReceived, err := h.broadcastToServers(publish.Params.Message, publish.Params.Channel)
	if alreadyReceived {
		h.log.Info().Msg("message was already received")
		return publish.ID, nil
	}

	if err != nil {
		return -1, xerrors.Errorf("failed to broadcast message: %v", err)
	}

	if publish.Params.Channel == rootChannel {
		err := h.handleRootChannelPublishMesssage(socket, publish)
		if err != nil {
			return publish.ID, xerrors.Errorf(rootChannelErr, err)
		}
		return publish.ID, nil
	}

	channel, err := h.getChan(publish.Params.Channel)
	if err != nil {
		return publish.ID, xerrors.Errorf(getChannelErr, err)
	}

	err = channel.Publish(publish, socket)
	if err != nil {
		return publish.ID, xerrors.Errorf(publishError, err)
	}

	return publish.ID, nil
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
		return xerrors.Errorf("message_id is wrong: expected %q found %q",
			expectedMessageID, messageID)
	}

	h.Lock()
	_, ok := h.hubInbox.GetMessage(broadcast.Params.Message.MessageID)
	if ok {
		h.log.Info().Msg("message was already received")
		return nil
	}
	h.hubInbox.StoreMessage(broadcast.Params.Message)
	h.Unlock()

	if err != nil {
		return xerrors.Errorf("failed to broadcast message: %v", err)
	}

	if broadcast.Params.Channel == rootChannel {
		err := h.handleRootChannelBroadcastMesssage(socket, broadcast)
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
	byteMessage []byte) ([]message.Message, int, error) {

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
