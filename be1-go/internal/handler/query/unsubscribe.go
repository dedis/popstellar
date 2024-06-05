package query

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/handler/messagedata/root"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/state"
)

func handleUnsubscribe(socket socket.Socket, msg []byte) (*int, error) {
	var unsubscribe method.Unsubscribe
	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	if root.Root == unsubscribe.Params.Channel {
		return &unsubscribe.ID, errors.NewAccessDeniedError("cannot Unsubscribe from root channel")
	}

	err = state.Unsubscribe(socket, unsubscribe.Params.Channel)
	if err != nil {
		return &unsubscribe.ID, err
	}

	socket.SendResult(unsubscribe.ID, nil, nil)

	return &unsubscribe.ID, nil
}
