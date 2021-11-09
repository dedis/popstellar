package hub

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"

	"golang.org/x/xerrors"
)

// handleRootChannelPublishMesssage handles an incoming publish message on the root channel.
func (h *Hub) handleRootChannelPublishMesssage(socket socket.Socket, publish method.Publish) error {
	jsonData, err := base64.URLEncoding.DecodeString(publish.Params.Message.Data)
	if err != nil {
		err := xerrors.Errorf("failed to decode message data: %v", err)
		socket.SendError(&publish.ID, err)
		return err
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		err := xerrors.Errorf("failed to validate message against json schema: %v", err)
		socket.SendError(&publish.ID, err)
		return err
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		err := xerrors.Errorf("failed to get object#action: %v", err)
		socket.SendError(&publish.ID, err)
		return err
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		err := answer.NewErrorf(-1, "only lao#create is allowed on root, "+
			"but found %s#%s", object, action)
		h.log.Err(err)
		socket.SendError(&publish.ID, err)
		return err
	}

	var laoCreate messagedata.LaoCreate

	err = publish.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		socket.SendError(&publish.ID, err)
		return err
	}

	err = laoCreate.Verify()
	if err != nil {
		h.log.Err(err).Msg("invalid lao#create message")
		socket.SendError(&publish.ID, err)
	}

	err = h.createLao(publish, laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		socket.SendError(&publish.ID, err)
		return err
	}

	return nil
}

// handleServerCatchup handles an incoming catchup message coming from a server
func (h *Hub) handleServerCatchup(senderSocket socket.Socket, byteMessage []byte) ([]string, int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	if catchup.Params.Channel != serverComChannel {
		return nil, catchup.ID, xerrors.Errorf("server catchup message can only be sent on /root/serverCom channel")
	}

	messages := h.inbox.GetSortedMessages()
	return messages, catchup.ID, nil
}

func (h *Hub) handleAnswer(senderSocket socket.Socket, byteMessage []byte) error {
	var answer method.ServerCatchupAnswer
	var result method.ServerResult

	err := json.Unmarshal(byteMessage, &result)
	if err == nil {
		h.log.Info().Msg("result isn't an answer to a catchup, nothing to handle")
		return nil
	}

	err = json.Unmarshal(byteMessage, &answer)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal answer: %v", err)
	}

	h.queries.mu.Lock()

	val := h.queries.queries[answer.ID]
	if val == nil {
		h.queries.mu.Unlock()
		return xerrors.Errorf("no query sent with id %v", answer.ID)
	} else if *val {
		h.queries.mu.Unlock()
		return xerrors.Errorf("query %v already got an answer", answer.ID)
	}

	*h.queries.queries[answer.ID] = true
	h.queries.mu.Unlock()

	for msg := range answer.Result {
		_, err := h.handlePublish(senderSocket, []byte(answer.Result[msg]))
		if err != nil {
			h.log.Err(err).Msgf("failed to handle message during catchup: %v", err)
		}
	}
	return nil
}

func (h *Hub) handlePublish(socket socket.Socket, byteMessage []byte) (int, error) {
	var publish method.Publish

	err := json.Unmarshal(byteMessage, &publish)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	alreadyReceived := h.broadcastToServers(publish, byteMessage)
	if alreadyReceived {
		h.log.Info().Msg("message was already received")
		return publish.ID, nil
	}

	if publish.Params.Channel == "/root" {
		err := h.handleRootChannelPublishMesssage(socket, publish)
		if err != nil {
			return -1, xerrors.Errorf("failed to handle root channel message: %v", err)
		}
		return publish.ID, nil
	}

	channel, err := h.getChan(publish.Params.Channel)
	if err != nil {
		return -1, xerrors.Errorf("failed to get channel: %v", err)
	}

	err = channel.Publish(publish)
	if err != nil {
		return -1, xerrors.Errorf("failed to publish: %v", err)
	}

	return publish.ID, nil
}

func (h *Hub) handleSubscribe(socket socket.Socket, byteMessage []byte) (int, error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(byteMessage, &subscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal subscribe message: %v", err)
	}

	channel, err := h.getChan(subscribe.Params.Channel)
	if err != nil {
		return -1, xerrors.Errorf("failed to get subscribe channel: %v", err)
	}

	err = channel.Subscribe(socket, subscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to publish: %v", err)
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
		return -1, xerrors.Errorf("failed to get unsubscribe channel: %v", err)
	}

	err = channel.Unsubscribe(socket.ID(), unsubscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unsubscribe: %v", err)
	}

	return unsubscribe.ID, nil
}

func (h *Hub) handleCatchup(socket socket.Socket, byteMessage []byte) ([]message.Message, int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	if catchup.Params.Channel == serverComChannel {
		if err != nil {
			return nil, catchup.ID, xerrors.Errorf("failed to handle root channel catchup message: %v", err)
		}
		return nil, catchup.ID, nil
	}

	channel, err := h.getChan(catchup.Params.Channel)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to get catchup channel: %v", err)
	}

	msg := channel.Catchup(catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to catchup: %v", err)
	}

	return msg, catchup.ID, nil
}
