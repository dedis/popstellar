package hub

import (
	"encoding/json"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/message/query/method"
)

func handleHeartbeat(params handlerParameters, byteMessage []byte) (*int, *answer.Error) {
	var heartbeat method.Heartbeat

	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	result, err := params.db.GetGetMessageByIDParams(heartbeat.Params)
	if err != nil {
		answerErr := answer.NewInternalServerError("failed to query db: %v", err).Wrap("handleHeartbeat")
		return nil, answerErr
	}

	if len(result) == 0 {
		return nil, nil
	}

	errAnswer := sendGetMessagesByID(params, result)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	return nil, nil
}

// sendGetMessagesById sends a getMessagesById message to a server
func sendGetMessagesByID(params handlerParameters, missingIds map[string][]string) *answer.Error {
	queryId := params.queries.GetNextID()

	getMessagesById := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "get_messages_by_id",
		},
		ID:     queryId,
		Params: missingIds,
	}

	buf, err := json.Marshal(getMessagesById)
	if err != nil {
		return answer.NewInternalServerError("failed to marshal: %v", err).Wrap("sendGetMessagesByID")
	}

	params.socket.Send(buf)

	params.queries.AddQuery(queryId, getMessagesById)

	return nil
}
