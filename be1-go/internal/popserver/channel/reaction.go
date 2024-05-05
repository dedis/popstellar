package channel

import (
	"popstellar/internal/popserver/database"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strings"
)

func handleChannelReaction(channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelReaction")
		return errAnswer
	}

	db, ok := database.GetReactionRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleChannelReaction")
		return errAnswer
	}

	channelID, _ := strings.CutPrefix(channel, "/")
	splitChannelID := strings.Split(channelID, "/")
	laoID := splitChannelID[1]

	isAttendee, err := db.IsAttendee(laoID, msg.Sender)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err).Wrap("handleChannelReaction")
		return errAnswer
	}
	if !isAttendee {
		errAnswer := answer.NewAccessDeniedError("user not inside roll-call").Wrap("handleChannelReaction")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionAdd:
		errAnswer = handleReactionAdd(msg)
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionDelete:
		errAnswer = handleReactionDelete(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelReaction")
		return errAnswer
	}

	err = db.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelReaction")
		return errAnswer
	}
	return nil

}

func handleReactionAdd(msg message.Message) *answer.Error {
	var reactMsg messagedata.ReactionAdd

	err := msg.UnmarshalData(&reactMsg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleReactionAdd")
		return errAnswer
	}

	err = reactMsg.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid message: %v", err).Wrap("handleReactionAdd")
		return errAnswer
	}

	return nil
}

func handleReactionDelete(msg message.Message) *answer.Error {
	var delReactMsg messagedata.ReactionDelete

	err := msg.UnmarshalData(&delReactMsg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleReactionDelete")
		return errAnswer
	}

	err = delReactMsg.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid message: %v", err).Wrap("handleReactionDelete")
		return errAnswer
	}

	db, ok := database.GetReactionRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleReactionDelete")
		return errAnswer
	}
	react, err := db.GetReaction(delReactMsg.ReactionID)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err).Wrap("handleReactionDelete")
		return errAnswer
	}

	if msg.Sender != react.Sender {
		errAnswer := answer.NewAccessDeniedError("only the owner of the reaction can delete it").Wrap("handleReactionDelete")
		return errAnswer
	}

	return nil
}
