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
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleCatchUp")
		return nil, errAnswer
	}

	return &catchup.ID, nil
}
