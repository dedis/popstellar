package query

import (
	"encoding/json"
	"popstellar/internal/handler/high"
	"popstellar/internal/logger"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
	"strings"
)

const thresholdMessagesByRumor = 1

func handlePublish(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handlePublish")
	}

	errAnswer := high.HandleChannel(publish.Params.Channel, publish.Params.Message, false)
	if errAnswer != nil {
		return &publish.ID, errAnswer.Wrap("handlePublish")
	}

	socket.SendResult(publish.ID, nil, nil)

	if strings.Contains(publish.Params.Channel, "federation") {
		return nil, nil
	}

	db, errAnswer := database.GetRumorSenderRepositoryInstance()
	if errAnswer != nil {
		logger.Logger.Error().Err(errAnswer)
		return nil, nil
	}

	logger.Logger.Debug().Msgf("sender rumor need to add message %s", publish.Params.Message.MessageID)
	nbMessagesInsideRumor, err := db.AddMessageToMyRumor(publish.Params.Message.MessageID)
	if err != nil {
		logger.Logger.Error().Err(err)
		return nil, nil
	}

	if nbMessagesInsideRumor < thresholdMessagesByRumor {
		logger.Logger.Debug().Msgf("no enough message to send rumor %s", publish.Params.Message.MessageID)
		return nil, nil
	}

	errAnswer = state.NotifyResetRumorSender()
	if errAnswer != nil {
		logger.Logger.Error().Err(errAnswer)
	}

	return nil, nil
}
