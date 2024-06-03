package query

import (
	"encoding/json"

	"popstellar/internal/errors"
	"popstellar/internal/message"
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
		return errors.NewQueryDatabaseError("params for get messages by id: %v", err)
	}

	if len(result) == 0 {
		return nil
	}

	queryId, err := state.GetNextID()
	if err != nil {
		return err
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
		return errors.NewJsonMarshalError(err.Error())
	}

	socket.Send(buf)

	return state.AddQuery(queryId, getMessagesById)
}
