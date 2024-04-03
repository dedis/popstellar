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
	resultMsgs := make(map[string]map[string]message.Message)

	// Unmarshal each message
	for channelID, rawMsgs := range result {
		resultMsgs[channelID] = make(map[string]message.Message)
		for _, rawMsg := range rawMsgs {
			var msg message.Message
			err := json.Unmarshal(rawMsg, &msg)
			if err == nil {
				resultMsgs[channelID][msg.MessageID] = msg
				continue
			}

			errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleGetMessagesByIDAnswer")
			params.log.Error().Msg(errAnswer.Error())
		}

		if len(resultMsgs[channelID]) == 0 {
			delete(resultMsgs, channelID)
		}
	}

	// Handle every messages
	for i := 0; i < maxRetry; i++ {
		sortedChannelIDs := make([]string, 0)
		for channelID := range resultMsgs {
			sortedChannelIDs = append(sortedChannelIDs, channelID)
		}
		sort.Slice(sortedChannelIDs, func(i, j int) bool {
			return len(sortedChannelIDs[i]) < len(sortedChannelIDs[j])
		})

		for _, channelID := range sortedChannelIDs {
			msgs := resultMsgs[channelID]
			for msgID, msg := range msgs {
				errAnswer := handleChannel(params, channelID, msg)
				if errAnswer == nil {
					delete(resultMsgs[channelID], msgID)
					continue
				}

				if errAnswer.Code == answer.InvalidMessageFieldErrorCode {
					delete(resultMsgs[channelID], msgID)
				}

				errAnswer = errAnswer.Wrap("handleGetMessagesByIDAnswer")
				params.log.Error().Msgf(errAnswer.Error())
			}

			if len(resultMsgs[channelID]) == 0 {
				delete(resultMsgs, channelID)
			}
		}

		if len(resultMsgs) == 0 {
			return nil
		}
	}

	errAnswer := params.db.AddNewBlackList(resultMsgs)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleGetMessagesByIDAnswer")
	}

	return nil
}