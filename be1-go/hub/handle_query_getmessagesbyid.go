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

	result, err := params.db.GetResultForGetMessagesByID(getMessagesById.Params)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query db: %v", err).Wrap("handleGetMessageByID")
		return &getMessagesById.ID, errAnswer
	}

	params.socket.SendResult(getMessagesById.ID, nil, result)

	return &getMessagesById.ID, nil
}
