package query

import (
	"encoding/json"

	"popstellar/internal/errors"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
)

func handleGetMessagesByID(socket socket.Socket, msg []byte) (*int, error) {
	var getMessagesById method.GetMessagesById
	err := json.Unmarshal(msg, &getMessagesById)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	db, err := database.GetQueryRepositoryInstance()
	if err != nil {
		return &getMessagesById.ID, err
	}

	result, err := db.GetResultForGetMessagesByID(getMessagesById.Params)
	if err != nil {
		return &getMessagesById.ID, err
	}

	socket.SendResult(getMessagesById.ID, nil, result)

	return &getMessagesById.ID, nil
}
