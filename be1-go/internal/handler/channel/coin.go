package channel

import (
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/database"
)

func handleChannelCoin(channelPath string, msg message.Message) error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return err
	}

	switch object + "#" + action {
	case messagedata.CoinObject + "#" + messagedata.CoinActionPostTransaction:
		err = handleCoinPostTransaction(msg)
	default:
		err = errors.NewInvalidActionError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	db, err := database.GetCoinRepositoryInstance()
	if err != nil {
		return err
	}

	err = db.StoreMessageAndData(channelPath, msg)
	if err != nil {
		return err
	}

	return broadcastToAllClients(msg, channelPath)
}

func handleCoinPostTransaction(msg message.Message) error {
	var data messagedata.PostTransaction
	err := msg.UnmarshalData(&data)
	if err != nil {
		return err
	}

	return data.Verify()
}
