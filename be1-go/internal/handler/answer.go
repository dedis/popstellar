package handler

import (
	"encoding/json"
	"math/rand"
	"popstellar/internal/logger"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/state"
	"sort"
)

const (
	maxRetry          = 10
	continueMongering = 0.5
)

func HandleAnswer(msg []byte) *answer.Error {
	var answerMsg answer.Answer

	err := json.Unmarshal(msg, &answerMsg)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return errAnswer.Wrap("HandleAnswer")
	}

	isRumor, errAnswer := state.IsRumorQuery(*answerMsg.ID)
	if errAnswer != nil {
		return errAnswer
	}
	if isRumor {
		return handleRumorAnswer(answerMsg)
	}

	if answerMsg.Result == nil {
		logger.Logger.Info().Msg("received an error, nothing to handle")
		// don't send any error to avoid infinite error loop as a server will
		// send an error to another server that will create another error
		return nil
	}

	if answerMsg.Result.IsEmpty() {
		logger.Logger.Info().Msg("expected isn't an answer to a popquery, nothing to handle")
		return nil
	}

	errAnswer = state.SetQueryReceived(*answerMsg.ID)
	if errAnswer != nil {
		return errAnswer.Wrap("HandleAnswer")
	}

	errAnswer = handleGetMessagesByIDAnswer(answerMsg)
	if errAnswer != nil {
		return errAnswer.Wrap("HandleAnswer")
	}

	return nil
}

func handleRumorAnswer(msg answer.Answer) *answer.Error {
	errAnswer := state.SetQueryReceived(*msg.ID)
	if errAnswer != nil {
		return errAnswer
	}

	logger.Logger.Debug().Msgf("received an answer to rumor query %d", *msg.ID)

	if msg.Error != nil {
		logger.Logger.Debug().Msgf("received an answer error to rumor query %d", *msg.ID)
		if msg.Error.Code != answer.DuplicateResourceErrorCode {
			logger.Logger.Debug().Msgf("invalid error code to rumor query %d", *msg.ID)
			return nil
		}

		stop := rand.Float64() < continueMongering

		if stop {
			logger.Logger.Debug().Msgf("stop mongering rumor query %d", *msg.ID)
			return nil
		}

		logger.Logger.Debug().Msgf("continue mongering rumor query %d", *msg.ID)
	}

	logger.Logger.Debug().Msgf("sender rumor need to continue sending query %d", *msg.ID)
	rumor, ok, errAnswer := state.GetRumorFromPastQuery(*msg.ID)
	if errAnswer != nil {
		return errAnswer
	}
	if !ok {
		return answer.NewInternalServerError("rumor query %d doesn't exist", *msg.ID)
	}

	SendRumor(nil, rumor)

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
			logger.Logger.Error().Err(errAnswer)
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
		sortedChannelIDs := getSortedChannels(msgsByChannel)

		tryToHandleMessages(msgsByChannel, sortedChannelIDs)

		if len(msgsByChannel) == 0 {
			return
		}
	}
}

func tryToHandleMessages(msgsByChannel map[string]map[string]message.Message, sortedChannelIDs []string) {
	for _, channelID := range sortedChannelIDs {
		msgs := msgsByChannel[channelID]
		for msgID, msg := range msgs {
			errAnswer := HandleMessage(channelID, msg, false)
			if errAnswer == nil {
				delete(msgsByChannel[channelID], msgID)
				continue
			}

			if errAnswer.Code == answer.InvalidMessageFieldErrorCode {
				delete(msgsByChannel[channelID], msgID)
			}

			errAnswer = errAnswer.Wrap(msgID).Wrap("tryToHandleMessages")
			logger.Logger.Error().Err(errAnswer)
		}

		if len(msgsByChannel[channelID]) == 0 {
			delete(msgsByChannel, channelID)
		}
	}
}

func getSortedChannels(msgsByChannel map[string]map[string]message.Message) []string {
	sortedChannelIDs := make([]string, 0)
	for channelID := range msgsByChannel {
		sortedChannelIDs = append(sortedChannelIDs, channelID)
	}
	sort.Slice(sortedChannelIDs, func(i, j int) bool {
		return len(sortedChannelIDs[i]) < len(sortedChannelIDs[j])
	})
	return sortedChannelIDs
}
