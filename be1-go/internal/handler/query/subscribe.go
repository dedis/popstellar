package query

import (
	"encoding/json"

	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/state"
)

func handleSubscribe(socket socket.Socket, msg []byte) (*int, error) {
	var subscribe method.Subscribe
	err := json.Unmarshal(msg, &subscribe)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	if channel.Root == subscribe.Params.Channel {
		return &subscribe.ID, errors.NewAccessDeniedError("cannot Subscribe to root channel")
	}

	err = state.Subscribe(socket, subscribe.Params.Channel)
	if err != nil {
		return &subscribe.ID, err
	}

	socket.SendResult(subscribe.ID, nil, nil)

	return &subscribe.ID, nil
}
