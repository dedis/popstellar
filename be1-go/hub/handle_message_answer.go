package hub

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method/message"
	"sort"
)

const maxRetry = 10

func handleGetMessagesByIDAnswer(params handlerParameters, msg answer.Answer) *answer.Error {
	result := msg.Result.GetMessagesByChannel()
	msgsByChan := make(map[string]map[string]message.Message)

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
			params.log.Error().Msg(errAnswer.Error())
		}

		if len(msgsByChan[channelID]) == 0 {
			delete(msgsByChan, channelID)
		}
	}

	// Handle every message and discard them if handled without error
	handleMessagesByChannel(params, msgsByChan)

	err := params.db.AddNewBlackList(msgsByChan)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query db: ", err).Wrap("handleGetMessagesByIDAnswer")
		return errAnswer
	}

	return nil
}

func handleMessagesByChannel(params handlerParameters, msgsByChannel map[string]map[string]message.Message) {
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
				errAnswer := handleChannel(params, channelID, msg)
				if errAnswer == nil {
					delete(msgsByChannel[channelID], msgID)
					continue
				}

				if errAnswer.Code == answer.InvalidMessageFieldErrorCode {
					delete(msgsByChannel[channelID], msgID)
				}

				errAnswer = errAnswer.Wrap(msgID).Wrap("handleGetMessagesByIDAnswer")
				params.log.Error().Msg(errAnswer.Error())
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
