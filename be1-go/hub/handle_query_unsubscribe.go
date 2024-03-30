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
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleUnsubscribe")
		return nil, errAnswer
	}

	if rootChannel == unsubscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot unsubscribe from root channel").Wrap("handleUnsubscribe")
		return &unsubscribe.ID, errAnswer
	}

	errAnswer := params.subs.unsubscribe(unsubscribe.Params.Channel, params.socket)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleUnsubscribe")
		return &unsubscribe.ID, errAnswer
	}

	return &unsubscribe.ID, nil
}
