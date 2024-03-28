package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/query/method"
)

func handleUnsubscribe(params handlerParameters, msg []byte) (*int, error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal unsubscribe message: %v", err)
	}

	return &unsubscribe.ID, nil
}
