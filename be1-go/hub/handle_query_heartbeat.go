package hub

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method"
)

func handleHeartbeat(params handlerParameters, byteMessage []byte) (*int, *answer.Error) {
	var heartbeat method.Heartbeat

	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		return nil, answer.NewInvalidMessageFieldError("failed to unmarshal heartbeat message: %v", err)
	}

	return nil, nil
}
