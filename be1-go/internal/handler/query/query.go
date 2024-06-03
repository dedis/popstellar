package query

import (
	"encoding/json"
	"popstellar/internal/errors"

	"popstellar/internal/message/answer"
	"popstellar/internal/message/query"
	"popstellar/internal/network/socket"
)

func HandleQuery(socket socket.Socket, msg []byte) error {
	var queryBase query.Base

	err := json.Unmarshal(msg, &queryBase)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("HandleQuery")
		socket.SendError(nil, errAnswer)
		return errAnswer
	}

	var id *int = nil

	switch queryBase.Method {
	case query.MethodCatchUp:
		id, err = handleCatchUp(socket, msg)
	case query.MethodGetMessagesById:
		id, err = handleGetMessagesByID(socket, msg)
	case query.MethodGreetServer:
		id, err = handleGreetServer(socket, msg)
	case query.MethodHeartbeat:
		err = handleHeartbeat(socket, msg)
	case query.MethodPublish:
		id, err = handlePublish(socket, msg)
	case query.MethodSubscribe:
		id, err = handleSubscribe(socket, msg)
	case query.MethodUnsubscribe:
		id, err = handleUnsubscribe(socket, msg)
	case query.MethodRumor:
		id, err = handleRumor(socket, msg)
	default:
		err = errors.NewInvalidActionError("unexpected method: '%s'", queryBase.Method)
	}

	if err != nil && id != nil {
		socket.SendPopError(id, err)
	}

	return err
}
