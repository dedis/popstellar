package handler

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
		return errAnswer.Wrap("handleChannelReaction")
	}

	db, errAnswer := database.GetReactionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelReaction")
	}

	laoPath, _ := strings.CutSuffix(channel, Social+Reactions)
	isAttendee, err := db.IsAttendee(laoPath, msg.Sender)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err)
		return errAnswer.Wrap("handleChannelReaction")
	}
	if !isAttendee {
		errAnswer := answer.NewAccessDeniedError("user not inside roll-call")
		return errAnswer.Wrap("handleChannelReaction")
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
		return errAnswer.Wrap("handleChannelReaction")
	}

	err = db.StoreMessageAndData(channel, msg)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to store message: %v", err)
		return errAnswer.Wrap("handleChannelReaction")
	}

	errAnswer = broadcastToAllClients(msg, channel)
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelReaction")
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

	db, errAnswer := database.GetReactionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleReactionDelete")
	}
	reactSender, err := db.GetReactionSender(delReactMsg.ReactionID)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err)
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
