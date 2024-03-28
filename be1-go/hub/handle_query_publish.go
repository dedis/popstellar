package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/query/method"
)

func handlePublish(params handlerParameters, msg []byte) (*int, error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	return &publish.ID, nil
}
