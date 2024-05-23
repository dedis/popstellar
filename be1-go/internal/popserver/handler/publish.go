package handler

import (
	"encoding/json"
	"popstellar"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/message/answer"
	"popstellar/message/query/method"
	"popstellar/network/socket"
	"strings"
)

const thresholdMessagesByRumor = 3

func handlePublish(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handlePublish")
	}

	errAnswer := handleChannel(publish.Params.Channel, publish.Params.Message, false)
	if errAnswer != nil {
		return &publish.ID, errAnswer.Wrap("handlePublish")
	}

	socket.SendResult(publish.ID, nil, nil)

	if strings.Contains(publish.Params.Channel, "federation") {
		return nil, nil
	}

	db, errAnswer := database.GetRumorSenderRepositoryInstance()
	if errAnswer != nil {
		popstellar.Logger.Error().Err(errAnswer)
		return nil, nil
	}

	popstellar.Logger.Debug().Msgf("sender rumor need to add message %s", publish.Params.Message.MessageID)
	nbMessagesInsideRumor, err := db.AddMessageToMyRumor(publish.Params.Message.MessageID)
	if err != nil {
		popstellar.Logger.Error().Err(err)
		return nil, nil
	}

	if nbMessagesInsideRumor < thresholdMessagesByRumor {
		popstellar.Logger.Debug().Msgf("no enough message to send rumor %s", publish.Params.Message.MessageID)
		return nil, nil
	}

	errAnswer = state.NotifyResetRumorSender()
	if errAnswer != nil {
		popstellar.Logger.Error().Err(errAnswer)
	}

	return nil, nil
}
