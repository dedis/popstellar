package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/query/method"
)

func handleCatchUp(params handlerParameters, msg []byte) (*int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(msg, &catchup)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	return &catchup.ID, nil
}
