package query

import (
	"encoding/json"

	"popstellar/internal/errors"
	"popstellar/internal/message"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
)

func handleHeartbeat(socket socket.Socket, byteMessage []byte) error {
	var heartbeat method.Heartbeat
	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		return errors.NewJsonUnmarshalError(err.Error())
	}

	db, err := database.GetQueryRepositoryInstance()
	if err != nil {
		return err
	}

	result, err := db.GetParamsForGetMessageByID(heartbeat.Params)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("params for get messages by id: %v", err)
		return errAnswer.Wrap("handleHeartbeat")
	}

	if len(result) == 0 {
		return nil
	}

	queryId, err := state.GetNextID()
	if err != nil {
		return answer.NewInternalServerError(err.Error())
	}

	getMessagesById := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: message.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGetMessagesById,
		},
		ID:     queryId,
		Params: result,
	}

	buf, err := json.Marshal(getMessagesById)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal: %v", err)
		return errAnswer.Wrap("handleHeartbeat")
	}

	socket.Send(buf)

	err = state.AddQuery(queryId, getMessagesById)
	if err != nil {
		return answer.NewInternalServerError(err.Error())
	}

	return nil
}
