package query

import (
	"encoding/json"
	"popstellar/internal/handler/channel"
	"popstellar/internal/logger"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
	"popstellar/internal/singleton/utils"
	"sort"
)

const maxRetry = 10

func handleRumor(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var rumor method.Rumor

	err := json.Unmarshal(msg, &rumor)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleRumor")
	}

	logger.Logger.Debug().Msgf("received rumor %s-%d from query %d",
		rumor.Params.SenderID, rumor.Params.RumorID, rumor.ID)

	db, err := database.GetQueryRepositoryInstance()
	if err != nil {
		return &rumor.ID, answer.NewInternalServerError(err.Error())
	}

	ok, err := db.CheckRumor(rumor.Params.SenderID, rumor.Params.RumorID)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("if rumor is not valid: %v", err)
		return &rumor.ID, errAnswer.Wrap("handleRumor")
	}
	if !ok {
		errAnswer := answer.NewInvalidResourceError("rumor %s: %v is not valid",
			rumor.Params.SenderID, rumor.Params.RumorID)
		return &rumor.ID, errAnswer
	}

	socket.SendResult(rumor.ID, nil, nil)

	SendRumor(socket, rumor)

	processedMsgs := tryHandlingMessagesByChannel(rumor.Params.Messages)

	err = db.StoreRumor(rumor.Params.RumorID, rumor.Params.SenderID, rumor.Params.Messages, processedMsgs)
	if err != nil {
		utils.LogError(err)
		return &rumor.ID, nil
	}

	messages, err := db.GetUnprocessedMessagesByChannel()
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("unprocessed messages: %v", err)
		return &rumor.ID, errAnswer.Wrap("handleRumor")
	}

	_ = tryHandlingMessagesByChannel(messages)

	return &rumor.ID, nil
}

func tryHandlingMessagesByChannel(unprocessedMsgsByChannel map[string][]message.Message) []string {
	processedMsgs := make([]string, 0)

	sortedChannels := sortChannels(unprocessedMsgsByChannel)

	for _, channel := range sortedChannels {
		unprocessedMsgs, newProcessedMsgs := tryHandlingMessages(channel, unprocessedMsgsByChannel[channel])

		if len(newProcessedMsgs) > 0 {
			processedMsgs = append(processedMsgs, newProcessedMsgs...)
		}

		if len(unprocessedMsgs) > 0 {
			unprocessedMsgsByChannel[channel] = unprocessedMsgs
		} else {
			delete(unprocessedMsgsByChannel, channel)
		}
	}

	return processedMsgs
}

func tryHandlingMessages(channelPath string, unprocessedMsgs []message.Message) ([]message.Message, []string) {
	processedMsgs := make([]string, 0)

	for i := 0; i < maxRetry; i++ {
		nbProcessed := 0
		for index, msg := range unprocessedMsgs {
			errAnswer := channel.HandleChannel(channelPath, msg, true)
			if errAnswer == nil {
				unprocessedMsgs = removeMessage(index-nbProcessed, unprocessedMsgs)
				processedMsgs = append(processedMsgs, msg.MessageID)
				nbProcessed++
				continue
			}

			errAnswer = errAnswer.Wrap(msg.MessageID).Wrap("tryHandlingMessages")
			logger.Logger.Error().Err(errAnswer)
		}

		if len(unprocessedMsgs) == 0 {
			break
		}
	}

	return unprocessedMsgs, processedMsgs
}

func removeMessage(index int, messages []message.Message) []message.Message {
	result := make([]message.Message, 0)
	result = append(result, messages[:index]...)
	return append(result, messages[index+1:]...)
}

func sortChannels(msgsByChannel map[string][]message.Message) []string {
	sortedChannelIDs := make([]string, 0)
	for channelID := range msgsByChannel {
		sortedChannelIDs = append(sortedChannelIDs, channelID)
	}
	sort.Slice(sortedChannelIDs, func(i, j int) bool {
		return len(sortedChannelIDs[i]) < len(sortedChannelIDs[j])
	})
	return sortedChannelIDs
}

func SendRumor(socket socket.Socket, rumor method.Rumor) {
	id, errAnswer := state.GetNextID()
	if errAnswer != nil {
		logger.Logger.Error().Err(errAnswer)
		return
	}

	rumor.ID = id

	errAnswer = state.AddRumorQuery(id, rumor)
	if errAnswer != nil {
		logger.Logger.Error().Err(errAnswer)
		return
	}

	buf, err := json.Marshal(rumor)
	if err != nil {
		logger.Logger.Error().Err(err)
		return
	}

	logger.Logger.Debug().Msgf("sending rumor %s-%d query %d", rumor.Params.SenderID, rumor.Params.RumorID, rumor.ID)
	errAnswer = state.SendRumor(socket, rumor.Params.SenderID, rumor.Params.RumorID, buf)
	if errAnswer != nil {
		logger.Logger.Err(errAnswer)
	}
}
