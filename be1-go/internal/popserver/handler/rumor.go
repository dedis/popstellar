package handler

import (
	"encoding/json"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/utils"
	"popstellar/message/answer"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"sort"
)

func handleRumor(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var rumor method.Rumor

	err := json.Unmarshal(msg, &rumor)
	if err != nil {
		errAnswer := answer.NewJsonUnmarshalError(err.Error())
		return nil, errAnswer.Wrap("handleRumor")
	}

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		return &rumor.ID, errAnswer.Wrap("handleRumor")
	}

	alreadyExists, err := db.HasRumor(rumor.Params.SenderID, rumor.Params.RumorID)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("if rumor exists: %v", err)
		return &rumor.ID, errAnswer.Wrap("handleRumor")
	}
	if alreadyExists {
		errAnswer := answer.NewInvalidResourceError("rumor %s-%v already exists",
			rumor.Params.SenderID, rumor.Params.RumorID)
		return &rumor.ID, errAnswer
	}

	socket.SendResult(rumor.ID, nil, nil)

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

func tryHandlingMessages(channel string, unprocessedMsgs []message.Message) ([]message.Message, []string) {
	processedMsgs := make([]string, 0)

	for i := 0; i < maxRetry; i++ {
		nbProcessed := 0
		for index, msg := range unprocessedMsgs {
			errAnswer := handleChannel(channel, msg, true)
			if errAnswer == nil {
				unprocessedMsgs = removeMessage(index-nbProcessed, unprocessedMsgs)
				processedMsgs = append(processedMsgs, msg.MessageID)
				nbProcessed++
				continue
			}

			errAnswer = errAnswer.Wrap(msg.MessageID).Wrap("tryHandlingMessages")
			utils.LogError(errAnswer)
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
