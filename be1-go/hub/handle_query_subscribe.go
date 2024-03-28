package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/query/method"
)

func handleSubscribe(params handlerParameters, msg []byte) (*int, error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(msg, &subscribe)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal subscribe message: %v", err)
	}

	return &subscribe.ID, nil
}
