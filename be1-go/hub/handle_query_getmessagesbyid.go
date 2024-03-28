package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/query/method"
)

func handleGetMessagesById(params handlerParameters, msg []byte) (*int, error) {
	var getMessagesById method.GetMessagesById

	err := json.Unmarshal(msg, &getMessagesById)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal getMessagesById message: %v", err)
	}

	return &getMessagesById.ID, nil
}
