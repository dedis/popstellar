package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/query/method"
)

func handleHeartbeat(params handlerParameters, byteMessage []byte) (*int, error) {
	var heartbeat method.Heartbeat

	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal heartbeat message: %v", err)
	}

	return nil, nil
}
