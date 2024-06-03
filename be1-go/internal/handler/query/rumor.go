package query

import (
	"encoding/json"
	"sort"

	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/logger"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/network/socket"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
)

const maxRetry = 10

func handleRumor(socket socket.Socket, msg []byte) (*int, error) {
	var rumor method.Rumor
	err := json.Unmarshal(msg, &rumor)
	if err != nil {
		return nil, errors.NewJsonUnmarshalError(err.Error())
	}

	logger.Logger.Debug().Msgf("received rumor %s-%d from query %d",
		rumor.Params.SenderID, rumor.Params.RumorID, rumor.ID)

	db, err := database.GetQueryRepositoryInstance()
	if err != nil {
		return &rumor.ID, err
	}

	ok, err := db.CheckRumor(rumor.Params.SenderID, rumor.Params.RumorID)
	if err != nil {
		return &rumor.ID, err
	}
	if !ok {
		return &rumor.ID, errors.NewDuplicateResourceError("rumor [%s|%v] is not valid",
			rumor.Params.SenderID, rumor.Params.RumorID)
	}

	socket.SendResult(rumor.ID, nil, nil)

	SendRumor(socket, rumor)

	processedMsgs := tryHandlingMessagesByChannel(rumor.Params.Messages)

	err = db.StoreRumor(rumor.Params.RumorID, rumor.Params.SenderID, rumor.Params.Messages, processedMsgs)
	if err != nil {
		return nil, err
	}

	messages, err := db.GetUnprocessedMessagesByChannel()
	if err != nil {
		return nil, err
	}

	_ = tryHandlingMessagesByChannel(messages)

	return nil, nil
}

func tryHandlingMessagesByChannel(unprocessedMsgsByChannel map[string][]message.Message) []string {
	processedMsgs := make([]string, 0)

	sortedChannels := sortChannels(unprocessedMsgsByChannel)

	for _, channelPath := range sortedChannels {
		unprocessedMsgs, newProcessedMsgs := tryHandlingMessages(channelPath, unprocessedMsgsByChannel[channelPath])

		if len(newProcessedMsgs) > 0 {
			processedMsgs = append(processedMsgs, newProcessedMsgs...)
		}

		if len(unprocessedMsgs) > 0 {
			unprocessedMsgsByChannel[channelPath] = unprocessedMsgs
		} else {
			delete(unprocessedMsgsByChannel, channelPath)
		}
	}

	return processedMsgs
}

func tryHandlingMessages(channelPath string, unprocessedMsgs []message.Message) ([]message.Message, []string) {
	processedMsgs := make([]string, 0)

	for i := 0; i < maxRetry; i++ {
		nbProcessed := 0
		for index, msg := range unprocessedMsgs {
			err := channel.HandleChannel(channelPath, msg, true)
			if err == nil {
				unprocessedMsgs = removeMessage(index-nbProcessed, unprocessedMsgs)
				processedMsgs = append(processedMsgs, msg.MessageID)
				nbProcessed++
				continue
			}

			logger.Logger.Error().Err(err)
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
	id, err := state.GetNextID()
	if err != nil {
		logger.Logger.Error().Err(err)
		return
	}

	rumor.ID = id

	err = state.AddRumorQuery(id, rumor)
	if err != nil {
		logger.Logger.Error().Err(err)
		return
	}

	buf, err := json.Marshal(rumor)
	if err != nil {
		logger.Logger.Error().Err(err)
		return
	}

	logger.Logger.Debug().Msgf("sending rumor %s-%d query %d", rumor.Params.SenderID, rumor.Params.RumorID, rumor.ID)
	err = state.SendRumor(socket, rumor.Params.SenderID, rumor.Params.RumorID, buf)
	if err != nil {
		logger.Logger.Err(err)
	}
}
