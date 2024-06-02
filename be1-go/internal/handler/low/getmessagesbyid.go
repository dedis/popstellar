package low

import (
	"encoding/json"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
)

func handleGetMessagesByID(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var getMessagesById method.GetMessagesById

	err := json.Unmarshal(msg, &getMessagesById)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleGetMessageByID")
	}

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		return &getMessagesById.ID, errAnswer.Wrap("handleGetMessageByID")
	}

	result, err := db.GetResultForGetMessagesByID(getMessagesById.Params)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("result for get messages by id: %v", err)
		return &getMessagesById.ID, errAnswer.Wrap("handleGetMessageByID")
	}

	socket.SendResult(getMessagesById.ID, nil, result)

	return &getMessagesById.ID, nil
}
