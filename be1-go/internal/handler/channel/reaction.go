package channel

import (
	"popstellar/internal/message/answer"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/database"
	"strings"
)

func handleChannelReaction(channelPath string, msg message.Message) *answer.Error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return answer.NewInternalServerError(err.Error())
	}

	db, err := database.GetReactionRepositoryInstance()
	if err != nil {
		return answer.NewInternalServerError(err.Error())
	}

	laoPath, _ := strings.CutSuffix(channelPath, Social+Reactions)
	isAttendee, err := db.IsAttendee(laoPath, msg.Sender)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("if is attendee: %v", err)
		return errAnswer.Wrap("handleChannelReaction")
	}
	if !isAttendee {
		errAnswer := answer.NewAccessDeniedError("user not inside roll-call")
		return errAnswer.Wrap("handleChannelReaction")
	}

	var errAnswer *answer.Error

	switch object + "#" + action {
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionAdd:
		errAnswer = handleReactionAdd(msg)
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionDelete:
		errAnswer = handleReactionDelete(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelReaction")
	}

	err = db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		errAnswer := answer.NewStoreDatabaseError(err.Error())
		return errAnswer.Wrap("handleChannelReaction")
	}

	err = broadcastToAllClients(msg, channelPath)
	if err != nil {
		return answer.NewInternalServerError(err.Error())
	}

	return nil

}

func handleReactionAdd(msg message.Message) *answer.Error {
	var reactMsg messagedata.ReactionAdd
	errAnswer := msg.UnmarshalMsgData(&reactMsg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleReactionAdd")
	}

	err := reactMsg.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid message: %v", err)
		return errAnswer.Wrap("handleReactionAdd")
	}

	return nil
}

func handleReactionDelete(msg message.Message) *answer.Error {
	var delReactMsg messagedata.ReactionDelete
	errAnswer := msg.UnmarshalMsgData(&delReactMsg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleReactionDelete")
	}

	err := delReactMsg.Verify()
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("invalid message: %v", err)
		return errAnswer.Wrap("handleReactionDelete")
	}

	db, err := database.GetReactionRepositoryInstance()
	if err != nil {
		return answer.NewInternalServerError(err.Error())
	}
	reactSender, err := db.GetReactionSender(delReactMsg.ReactionID)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("sender of the reaction %s: %v", delReactMsg.ReactionID, err)
		return errAnswer.Wrap("handleReactionDelete")
	}
	if reactSender == "" {
		errAnswer := answer.NewInvalidResourceError("unknown reaction")
		return errAnswer.Wrap("handleReactionDelete")
	}

	if msg.Sender != reactSender {
		errAnswer := answer.NewAccessDeniedError("only the owner of the reaction can delete it")
		return errAnswer.Wrap("handleReactionDelete")
	}

	return nil
}
