package query

import (
	"encoding/json"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query"
	"popstellar/internal/network/socket"
)

func HandleQuery(socket socket.Socket, msg []byte) *answer.Error {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("HandleQuery")
		socket.SendError(nil, errAnswer)
		return errAnswer
	}

	var id *int = nil
	var errAnswer *answer.Error

	switch queryBase.Method {
	case query.MethodCatchUp:
		id, errAnswer = handleCatchUp(socket, msg)
	case query.MethodGetMessagesById:
		id, errAnswer = handleGetMessagesByID(socket, msg)
	case query.MethodGreetServer:
		id, errAnswer = handleGreetServer(socket, msg)
	case query.MethodHeartbeat:
		errAnswer = handleHeartbeat(socket, msg)
	case query.MethodPublish:
		id, errAnswer = handlePublish(socket, msg)
	case query.MethodSubscribe:
		id, errAnswer = handleSubscribe(socket, msg)
	case query.MethodUnsubscribe:
		id, errAnswer = handleUnsubscribe(socket, msg)
	case query.MethodRumor:
		id, errAnswer = handleRumor(socket, msg)
	default:
		errAnswer = answer.NewInvalidResourceError("unexpected method: '%s'", queryBase.Method)
	}

	if errAnswer != nil && queryBase.Method != query.MethodGreetServer && queryBase.Method != query.MethodHeartbeat {
		errAnswer = errAnswer.Wrap("HandleQuery")
		socket.SendError(id, errAnswer)
		return errAnswer
	}

	return nil
}
