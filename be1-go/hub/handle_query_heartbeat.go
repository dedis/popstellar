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
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal heartbeat message: %v",
			err).Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	channelIDs := make([]string, 0)
	for k := range heartbeat.Params {
		channelIDs = append(channelIDs, k)
	}

	myMsgIDsPerChannel, err := params.db.GetMessageIDsPerChannel(channelIDs)
	if err != nil {
		answerErr := answer.NewInternalServerError("error while querying db: %v", err).Wrap("handleHeartbeat")
		return nil, answerErr
	}

	result := make(map[string][]string)

	for channelID, msgIDs := range heartbeat.Params {
		myMsgIDs, ok := myMsgIDsPerChannel[channelID]
		if !ok {
			continue
		}

		result[channelID] = make([]string, 0)

		for _, msgID := range msgIDs {
			_, ok := myMsgIDs[msgID]
			if !ok {
				result[channelID] = append(result[channelID], msgID)
			}
		}

		if len(result[channelID]) > 0 {
			continue
		}

		delete(result, channelID)
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
		return answer.NewInternalServerError("failed to marshal getMessagesById query: %v", err).Wrap("sendGetMessagesByID")
	}

	params.socket.Send(buf)

	params.queries.AddQuery(queryId, getMessagesById)

	return nil
}
