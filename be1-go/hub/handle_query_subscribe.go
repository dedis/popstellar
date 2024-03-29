package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/answer"
	"popstellar/message/query/method"
)

func handleSubscribe(params handlerParameters, msg []byte) (*int, error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(msg, &subscribe)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal subscribe message: %v", err)
	}

	if rootChannel == subscribe.Params.Channel {
		return &subscribe.ID, answer.NewInvalidActionError("cannot subscribe to root channel")
	}

	err = params.subs.subscribe(subscribe.Params.Channel, params.socket)

	return &subscribe.ID, err
}
