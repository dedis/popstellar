package hub

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method"
)

func handleUnsubscribe(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		return nil, answer.NewInvalidMessageFieldError("failed to unmarshal unsubscribe message: %v", err)
	}

	if rootChannel == unsubscribe.Params.Channel {
		return &unsubscribe.ID, answer.NewInvalidActionError("cannot unsubscribe from root channel")
	}

	errAnswer := params.subs.unsubscribe(unsubscribe.Params.Channel, params.socket)
	if errAnswer != nil {
		return &unsubscribe.ID, errAnswer.Wrap("failed to handle unsubscribe")
	}

	return &unsubscribe.ID, nil
}
