package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/answer"
	"popstellar/message/query/method"
)

func handleUnsubscribe(params handlerParameters, msg []byte) (*int, error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal unsubscribe message: %v", err)
	}

	if rootChannel == unsubscribe.Params.Channel {
		return &unsubscribe.ID, answer.NewInvalidActionError("cannot unsubscribe from root channel")
	}

	err = params.subs.unsubscribe(unsubscribe.Params.Channel, params.socket)

	return &unsubscribe.ID, nil
}
