package message

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"popstellar/internal/popserver/channel"
	"popstellar/internal/popserver/singleton/database"
	"popstellar/internal/popserver/singleton/utils"
	"popstellar/internal/popserver/types"
	"popstellar/message/answer"
	"popstellar/message/query/method/message"
	"sort"
)

const maxRetry = 10

func handleGetMessagesByIDAnswer(params types.HandlerParameters, msg answer.Answer) *answer.Error {
	result := msg.Result.GetMessagesByChannel()
	msgsByChan := make(map[string]map[string]message.Message)

	log, ok := utils.GetLogInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get utils").Wrap("handleGetMessagesByIDAnswer")
		return errAnswer
	}

	// Unmarshal each message
	for channelID, rawMsgs := range result {
		msgsByChan[channelID] = make(map[string]message.Message)
		for _, rawMsg := range rawMsgs {
			var msg message.Message
			err := json.Unmarshal(rawMsg, &msg)
			if err == nil {
				msgsByChan[channelID][msg.MessageID] = msg
				continue
			}

			errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleGetMessagesByIDAnswer")
			log.Error().Msg(errAnswer.Error())
		}

		if len(msgsByChan[channelID]) == 0 {
			delete(msgsByChan, channelID)
		}
	}

	// Handle every message and discard them if handled without error
	handleMessagesByChannel(params, msgsByChan, log)

	db, ok := database.GetAnswerRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleGetMessagesByIDAnswer")
		return errAnswer
	}

	err := db.StorePendingMessages(msgsByChan)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: ", err).Wrap("handleGetMessagesByIDAnswer")
		return errAnswer
	}

	return nil
}

func handleMessagesByChannel(params types.HandlerParameters, msgsByChannel map[string]map[string]message.Message, log *zerolog.Logger) {
	// Handle every messages
	for i := 0; i < maxRetry; i++ {
		// Sort by channelID length
		sortedChannelIDs := make([]string, 0)
		for channelID := range msgsByChannel {
			sortedChannelIDs = append(sortedChannelIDs, channelID)
		}
		sort.Slice(sortedChannelIDs, func(i, j int) bool {
			return len(sortedChannelIDs[i]) < len(sortedChannelIDs[j])
		})

		for _, channelID := range sortedChannelIDs {
			msgs := msgsByChannel[channelID]
			for msgID, msg := range msgs {
				errAnswer := channel.HandleChannel(params, channelID, msg)
				if errAnswer == nil {
					delete(msgsByChannel[channelID], msgID)
					continue
				}

				if errAnswer.Code == answer.InvalidMessageFieldErrorCode {
					delete(msgsByChannel[channelID], msgID)
				}

				errAnswer = errAnswer.Wrap(msgID).Wrap("handleGetMessagesByIDAnswer")
				log.Error().Msg(errAnswer.Error())
			}

			if len(msgsByChannel[channelID]) == 0 {
				delete(msgsByChannel, channelID)
			}
		}

		if len(msgsByChannel) == 0 {
			return
		}
	}
}
