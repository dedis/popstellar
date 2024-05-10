package handler

import (
	"encoding/json"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/utils"
	"popstellar/message/answer"
	"popstellar/message/query/method/message"
	"sort"
)

const maxRetry = 10

func handleAnswer(msg []byte) *answer.Error {
	var answerMsg answer.Answer

	err := json.Unmarshal(msg, &answerMsg)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err)
		return errAnswer.Wrap("handleAnswer")
	}

	if answerMsg.Result == nil {
		utils.LogInfo("received an error, nothing to handle")
		// don't send any error to avoid infinite error loop as a server will
		// send an error to another server that will create another error
		return nil
	}
	if answerMsg.Result.IsEmpty() {
		utils.LogInfo("expected isn't an answer to a popquery, nothing to handle")
		return nil
	}

	errAnswer := state.SetQueryReceived(*answerMsg.ID)
	if errAnswer != nil {
		return errAnswer.Wrap("handleAnswer")
	}

	errAnswer = handleGetMessagesByIDAnswer(answerMsg)
	if errAnswer != nil {
		return errAnswer.Wrap("handleAnswer")
	}

	return nil
}

func handleGetMessagesByIDAnswer(msg answer.Answer) *answer.Error {
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

			errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err)
			utils.LogError(errAnswer.Wrap("handleGetMessagesByIDAnswer"))
		}

		if len(msgsByChan[channelID]) == 0 {
			delete(msgsByChan, channelID)
		}
	}

	// Handle every message and discard them if handled without error
	handleMessagesByChannel(msgsByChan)

	return nil
}

func handleMessagesByChannel(msgsByChannel map[string]map[string]message.Message) {
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
				errAnswer := handleChannel(channelID, msg)
				if errAnswer == nil {
					delete(msgsByChannel[channelID], msgID)
					continue
				}

				if errAnswer.Code == answer.InvalidMessageFieldErrorCode {
					delete(msgsByChannel[channelID], msgID)
				}

				errAnswer = errAnswer.Wrap(msgID).Wrap("handleGetMessagesByIDAnswer")
				utils.LogError(errAnswer)
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
