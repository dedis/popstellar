package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/query/method"
)

func handleGreetServer(params handlerParameters, byteMessage []byte) (*int, error) {
	var greetServer method.GreetServer

	err := json.Unmarshal(byteMessage, &greetServer)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal greetServer message: %v", err)
	}

	return nil, nil
}
