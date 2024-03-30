package hub

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method"
)

func handleSubscribe(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(msg, &subscribe)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal subscribe message: %v",
			err).Wrap("handleSubscribe")
		return nil, errAnswer
	}

	if rootChannel == subscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot subscribe to root channel" +
			"").Wrap("handleSubscribe")
		return &subscribe.ID, errAnswer
	}

	errAnswer := params.subs.subscribe(subscribe.Params.Channel, params.socket)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleSubscribe")
		return &subscribe.ID, errAnswer
	}

	return &subscribe.ID, nil
}
