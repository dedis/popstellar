package query

import (
	"encoding/json"
	"strings"

	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/logger"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
)

const thresholdMessagesByRumor = 1

func handlePublish(socket socket.Socket, msg []byte) (*int, error) {
	var publish method.Publish
	err := json.Unmarshal(msg, &publish)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	errAnswer := channel.HandleChannel(publish.Params.Channel, publish.Params.Message, false)
	if errAnswer != nil {
		return &publish.ID, errAnswer.Wrap("handlePublish")
	}

	socket.SendResult(publish.ID, nil, nil)

	if strings.Contains(publish.Params.Channel, "federation") {
		return nil, nil
	}

	db, err := database.GetRumorSenderRepositoryInstance()
	if err != nil {
		return nil, err
	}

	logger.Logger.Debug().Msgf("sender rumor need to add message %s", publish.Params.Message.MessageID)
	nbMessagesInsideRumor, err := db.AddMessageToMyRumor(publish.Params.Message.MessageID)
	if err != nil {
		return nil, err
	}

	if nbMessagesInsideRumor < thresholdMessagesByRumor {
		logger.Logger.Debug().Msgf("no enough message to send rumor %s", publish.Params.Message.MessageID)
		return nil, nil
	}

	return nil, state.NotifyResetRumorSender()
}
