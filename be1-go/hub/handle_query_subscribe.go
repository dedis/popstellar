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
		return nil, answer.NewInvalidMessageFieldError("failed to unmarshal subscribe message: %v",
			err).Wrap("handleSubscribe")
	}

	if rootChannel == subscribe.Params.Channel {
		return &subscribe.ID, answer.NewInvalidActionError("cannot subscribe to root channel" +
			"").Wrap("handleSubscribe")
	}

	errAnswer := params.subs.subscribe(subscribe.Params.Channel, params.socket)
	if errAnswer != nil {
		return &subscribe.ID, errAnswer.Wrap("handleSubscribe")
	}

	return &subscribe.ID, nil
}
