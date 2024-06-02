package query

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/state"
)

func handleSubscribe(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(msg, &subscribe)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleSubscribe")
	}

	if channel.Root == subscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot Subscribe to root channel")
		return &subscribe.ID, errAnswer.Wrap("handleSubscribe")
	}

	errAnswer := state.Subscribe(socket, subscribe.Params.Channel)
	if errAnswer != nil {
		return &subscribe.ID, errAnswer.Wrap("handleSubscribe")
	}

	socket.SendResult(subscribe.ID, nil, nil)

	return &subscribe.ID, nil
}
