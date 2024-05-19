package handler

import (
	"encoding/json"
	"popstellar/internal/popserver/state"
	"popstellar/message/answer"
	"popstellar/message/query/method"
	"popstellar/network/socket"
	"strings"
)

func handlePublish(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handlePublish")
	}

	errAnswer := handleChannel(publish.Params.Channel, publish.Params.Message, false)
	if errAnswer != nil {
		return &publish.ID, errAnswer.Wrap("handlePublish")
	}

	socket.SendResult(publish.ID, nil, nil)

	if strings.Contains(publish.Params.Channel, "federation") {
		return nil, nil
	}

	errAnswer = state.NotifyRumorSenderForNewMessage(publish.Params.Message.MessageID)
	if errAnswer != nil {
		return nil, errAnswer
	}

	return nil, nil
}
