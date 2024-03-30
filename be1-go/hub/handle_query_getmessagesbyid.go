package hub

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method"
)

func handleGetMessagesByID(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var getMessagesById method.GetMessagesById

	err := json.Unmarshal(msg, &getMessagesById)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal message: %v",
			err).Wrap("handleGetMessageByID")
		return nil, errAnswer
	}

	return &getMessagesById.ID, nil
}
