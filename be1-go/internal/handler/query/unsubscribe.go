package query

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/state"
)

func handleUnsubscribe(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleUnsubscribe")
	}

	if channel.Root == unsubscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot Unsubscribe from root channel")
		return &unsubscribe.ID, errAnswer.Wrap("handleUnsubscribe")
	}

	errAnswer := state.Unsubscribe(socket, unsubscribe.Params.Channel)
	if errAnswer != nil {
		return &unsubscribe.ID, errAnswer.Wrap("handleUnsubscribe")
	}

	socket.SendResult(unsubscribe.ID, nil, nil)

	return &unsubscribe.ID, nil
}
