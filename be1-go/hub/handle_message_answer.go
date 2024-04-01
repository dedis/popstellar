package hub

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method/message"
	"sort"
)

func handleGetMessagesByIDAnswer(params handlerParameters, msg answer.Answer) *answer.Error {
	result := msg.Result.GetMessagesByChannel()
	tmpResult := make(map[string]map[string]message.Message)

	// Sort the channelID by length (/root first, ...)
	sortedChannelID := make([]string, 0)
	for channelID := range result {
		sortedChannelID = append(sortedChannelID, channelID)
	}
	sort.Slice(sortedChannelID, func(i, j int) bool {
		return len(sortedChannelID[i]) < len(sortedChannelID[j])
	})

	// Handle each messages
	for _, channelID := range sortedChannelID {
		tmpResult[channelID] = make(map[string]message.Message)
		rawMsgs := result[channelID]
		for _, rawMsg := range rawMsgs {
			var msgData message.Message
			err := json.Unmarshal(rawMsg, &msgData)
			if err != nil {
				errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v",
					err).Wrap("handleGetMessagesByIDAnswer")
				params.log.Error().Msg(errAnswer.Error())
			}

			errAnswer := handleChannel(params, channelID, msgData)
			if errAnswer != nil {
				errAnswer = errAnswer.Wrap("handleGetMessagesByIDAnswer")
				params.log.Error().Msgf(errAnswer.Error())
				tmpResult[channelID][msgData.MessageID] = msgData
			}
		}

		if len(tmpResult[channelID]) == 0 {
			delete(tmpResult, channelID)
		}
	}

	// Retry failed msgs
	for i := 0; i < 10; i++ {
		for channelID, msgsData := range tmpResult {
			for msgID, msgData := range msgsData {
				errAnswer := handleChannel(params, channelID, msgData)
				if errAnswer != nil {
					errAnswer = errAnswer.Wrap("handleGetMessagesByIDAnswer")
					params.log.Error().Msgf(errAnswer.Error())
					continue
				}

				delete(tmpResult[channelID], msgID)
			}

			if len(tmpResult[channelID]) == 0 {
				delete(tmpResult, channelID)
			}
		}

		if len(tmpResult) == 0 {
			break
		}
	}

	if len(tmpResult) == 0 {
		return nil
	}

	errAnswer := params.db.AddNewBlackList(tmpResult)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleGetMessagesByIDAnswer")
	}

	return nil
}
