package hub

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method/message"
	"sort"
)

const maxRetry = 10

func handleGetMessagesByIDAnswer(params handlerParameters, msg answer.Answer) *answer.Error {
	blacklist, errorAnswers := handleMessagesByChannel(params, msg)
	if len(errorAnswers) != 0 {
		for _, errorAnswer := range errorAnswers {
			params.log.Error().Msg(errorAnswer.Error())
		}
	}

	errAnswer := params.db.AddNewBlackList(blacklist)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleGetMessagesByIDAnswer")
	}

	return nil
}

func handleMessagesByChannel(params handlerParameters, msg answer.Answer) (map[string]map[string]message.Message, []*answer.Error) {
	result := msg.Result.GetMessagesByChannel()
	blacklist := make(map[string]map[string]message.Message)
	errorAnswers := make([]*answer.Error, 0)

	// Unmarshal each message
	for channelID, rawMsgs := range result {
		blacklist[channelID] = make(map[string]message.Message)
		for _, rawMsg := range rawMsgs {
			var msg message.Message
			err := json.Unmarshal(rawMsg, &msg)
			if err == nil {
				blacklist[channelID][msg.MessageID] = msg
				continue
			}

			errorAnswers = append(errorAnswers, answer.NewInvalidMessageFieldError("failed to unmarshal: %v",
				err).Wrap("handleGetMessagesByIDAnswer"))
		}

		if len(blacklist[channelID]) == 0 {
			delete(blacklist, channelID)
		}
	}

	// Sort by channelID length
	sortedChannelIDs := make([]string, 0)
	for channelID := range blacklist {
		sortedChannelIDs = append(sortedChannelIDs, channelID)
	}
	sort.Slice(sortedChannelIDs, func(i, j int) bool {
		return len(sortedChannelIDs[i]) < len(sortedChannelIDs[j])
	})

	// Handle every messages
	for i := 0; i < maxRetry; i++ {
		for j, channelID := range sortedChannelIDs {
			msgs := blacklist[channelID]
			for msgID, msg := range msgs {
				errAnswer := handleChannel(params, channelID, msg)
				if errAnswer == nil {
					delete(blacklist[channelID], msgID)
					continue
				}

				if errAnswer.Code == answer.InvalidMessageFieldErrorCode {
					delete(blacklist[channelID], msgID)
				}

				errorAnswers = append(errorAnswers, errAnswer.Wrap(msgID).Wrap("handleGetMessagesByIDAnswer"))
			}

			if len(blacklist[channelID]) == 0 {
				delete(blacklist, channelID)
				sortedChannelIDs = append(sortedChannelIDs[:j], sortedChannelIDs[j+1:]...)
			}
		}

		if len(blacklist) == 0 {
			break
		}
	}

	return blacklist, errorAnswers
}
