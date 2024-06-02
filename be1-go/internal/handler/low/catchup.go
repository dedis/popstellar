package low

import (
	"encoding/json"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
)

func handleCatchUp(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var catchup method.Catchup

	err := json.Unmarshal(msg, &catchup)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleCatchUp")
	}

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		return &catchup.ID, errAnswer.Wrap("handleCatchUp")
	}

	result, err := db.GetAllMessagesFromChannel(catchup.Params.Channel)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("all message from channel %s: %v", catchup.Params.Channel, err)
		return &catchup.ID, errAnswer.Wrap("handleCatchUp")
	}

	socket.SendResult(catchup.ID, result, nil)

	return &catchup.ID, nil
}
