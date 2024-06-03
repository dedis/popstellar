package query

import (
	"encoding/json"
	"popstellar/internal/errors"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
)

func handleCatchUp(socket socket.Socket, msg []byte) (*int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(msg, &catchup)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	db, err := database.GetQueryRepositoryInstance()
	if err != nil {
		return &catchup.ID, err
	}

	result, err := db.GetAllMessagesFromChannel(catchup.Params.Channel)
	if err != nil {
		return &catchup.ID, errors.NewQueryDatabaseError("all message from channel %s: %v", catchup.Params.Channel, err)
	}

	socket.SendResult(catchup.ID, result, nil)

	return &catchup.ID, nil
}
