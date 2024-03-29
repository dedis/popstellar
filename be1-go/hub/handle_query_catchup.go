package hub

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method"
)

func handleCatchUp(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var catchup method.Catchup

	err := json.Unmarshal(msg, &catchup)
	if err != nil {
		return nil, answer.NewInvalidMessageFieldError("failed to unmarshal catchup message: %v", err)
	}

	return &catchup.ID, nil
}
