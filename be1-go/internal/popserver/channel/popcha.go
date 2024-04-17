package channel

import (
	"popstellar/internal/popserver/singleton/database"
	"popstellar/internal/popserver/types"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

func handleChannelPopCha(params types.HandlerParameters, channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(params, msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelPopCha")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.AuthObject + "#" + messagedata.AuthAction:
		errAnswer = handleAuth(params, msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelPopCha")
		return errAnswer
	}

	db, ok := database.GetPopChaRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleChannelPopCha")
		return errAnswer
	}

	err := db.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelPopCha")
		return errAnswer
	}
	return nil
}

func handleAuth(params types.HandlerParameters, msg message.Message) *answer.Error {
	return nil
}
