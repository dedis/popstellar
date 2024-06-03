package channel

import (
	"strings"

	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/database"
)

func handleChannelReaction(channelPath string, msg message.Message) error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return err
	}

	db, err := database.GetReactionRepositoryInstance()
	if err != nil {
		return err
	}

	laoPath, _ := strings.CutSuffix(channelPath, Social+Reactions)
	isAttendee, err := db.IsAttendee(laoPath, msg.Sender)
	if err != nil {
		return err
	}
	if !isAttendee {
		return errors.NewAccessDeniedError("user not inside roll-call")
	}

	switch object + "#" + action {
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionAdd:
		err = handleReactionAdd(msg)
	case messagedata.ReactionObject + "#" + messagedata.ReactionActionDelete:
		err = handleReactionDelete(msg)
	default:
		err = errors.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	err = db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	return broadcastToAllClients(msg, channelPath)
}

func handleReactionAdd(msg message.Message) error {
	var reactMsg messagedata.ReactionAdd
	err := msg.UnmarshalData(&reactMsg)
	if err != nil {
		return err
	}

	err = reactMsg.Verify()
	if err != nil {
		return err
	}

	return nil
}

func handleReactionDelete(msg message.Message) error {
	var delReactMsg messagedata.ReactionDelete
	err := msg.UnmarshalData(&delReactMsg)
	if err != nil {
		return err
	}

	err = delReactMsg.Verify()
	if err != nil {
		return err
	}

	db, err := database.GetReactionRepositoryInstance()
	if err != nil {
		return err
	}

	reactSender, err := db.GetReactionSender(delReactMsg.ReactionID)
	if err != nil {
		return err
	}
	if reactSender == "" {
		return errors.NewInvalidResourceError("unknown reaction")
	}

	if msg.Sender != reactSender {
		return errors.NewAccessDeniedError("only the owner of the reaction can delete it")
	}

	return nil
}
