package hub

import (
	"encoding/json"
	"github.com/rs/zerolog/log"
	"golang.org/x/exp/slices"
	"golang.org/x/xerrors"
	"popstellar/hub/state"
	message2 "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
)

func (h *Hub) handleGreetServer(socket socket.Socket, byteMessage []byte) error {
	var greetServer method.GreetServer

	err := json.Unmarshal(byteMessage, &greetServer)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal greetServer message: %v", err)
	}

	// store information about the server
	err = h.peers.AddPeerInfo(socket.ID(), greetServer.Params)
	if err != nil {
		return xerrors.Errorf("failed to add peer info: %v", err)
	}

	if h.peers.IsPeerGreeted(socket.ID()) {
		return nil
	}

	err = h.SendGreetServer(socket)
	if err != nil {
		return xerrors.Errorf("failed to send greetServer message: %v", err)
	}
	return nil
}

func (h *Hub) handleHeartbeat(socket socket.Socket,
	byteMessage []byte,
) error {
	var heartbeat method.Heartbeat

	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal heartbeat message: %v", err)
	}

	receivedIds := heartbeat.Params

	missingIds := getMissingIds(receivedIds, h.hubInbox.GetIDsTable(), &h.blacklist)

	if len(missingIds) > 0 {
		err = h.sendGetMessagesByIdToServer(socket, missingIds)
		if err != nil {
			return xerrors.Errorf("failed to send getMessagesById message: %v", err)
		}
	}

	return nil
}

func (h *Hub) handleGetMessagesById(socket socket.Socket,
	byteMessage []byte,
) (map[string][]message.Message, int, error) {
	var getMessagesById method.GetMessagesById

	err := json.Unmarshal(byteMessage, &getMessagesById)
	if err != nil {
		return nil, 0, xerrors.Errorf("failed to unmarshal getMessagesById message: %v", err)
	}

	missingMessages, err := h.getMissingMessages(getMessagesById.Params)
	if err != nil {
		return nil, getMessagesById.ID, xerrors.Errorf("failed to retrieve messages: %v", err)
	}

	return missingMessages, getMessagesById.ID, nil
}

// handleAnswer handles the answer to a message sent by the server
func (h *Hub) handleAnswer(senderSocket socket.Socket, byteMessage []byte) error {
	var answerMsg answer.Answer

	err := json.Unmarshal(byteMessage, &answerMsg)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal answer: %v", err)
	}

	if answerMsg.Result == nil {
		h.log.Warn().Msg("received an error, nothing to handle")
		// don't send any error to avoid infinite error loop as a server will
		// send an error to another server that will create another error
		return nil
	}
	if answerMsg.Result.IsEmpty() {
		h.log.Info().Msg("result isn't an answer to a query, nothing to handle")
		return nil
	}

	err = h.queries.SetQueryReceived(*answerMsg.ID)
	if err != nil {
		return xerrors.Errorf("failed to set query state: %v", err)
	}

	err = h.handleGetMessagesByIdAnswer(senderSocket, answerMsg)
	if err != nil {
		return err
	}

	return nil
}

func (h *Hub) handleGetMessagesByIdAnswer(senderSocket socket.Socket, answerMsg answer.Answer) error {
	var err error
	messages := answerMsg.Result.GetMessagesByChannel()
	tempBlacklist := make([]string, 0)
	// Loops over the messages to process them until it succeeds or reaches
	// the max number of attempts
	for i := 0; i < maxRetry; i++ {
		tempBlacklist, err = h.loopOverMessages(&messages, senderSocket)
		if err == nil && len(tempBlacklist) == 0 {
			return nil
		}
	}
	// Add contents from tempBlacklist to h.blacklist
	h.blacklist.Append(tempBlacklist...)
	return xerrors.Errorf("failed to process messages: %v", err)
}

// loopOverMessages loops over the messages received from a getMessagesById answer to process them
// and update the list of messages to process during the next iteration with those that fail
func (h *Hub) loopOverMessages(messages *map[string][]json.RawMessage, senderSocket socket.Socket) ([]string, error) {
	var errMsg string
	tempBlacklist := make([]string, 0)
	for channel, messageArray := range *messages {
		newMessageArray := make([]json.RawMessage, 0)

		// Try to process each message
		for _, msg := range messageArray {
			var messageData message.Message
			err := json.Unmarshal(msg, &messageData)
			if err != nil {
				h.log.Error().Msgf("failed to unmarshal message during getMessagesById answer handling: %v", err)
				continue
			}

			if h.blacklist.Contains(messageData.MessageID) {
				break
			}

			err = h.handleReceivedMessage(senderSocket, messageData, channel)
			if err != nil {
				h.log.Error().Msgf("failed to handle message received from getMessagesById answer: %v", err)
				newMessageArray = append(newMessageArray, msg) // if there's an error, keep the message
				errMsg += err.Error()

				// Add the ID of the failed message to the blacklist
				tempBlacklist = append(tempBlacklist, messageData.MessageID)
			}
		}
		// Update the list of messages to process during the next iteration
		(*messages)[channel] = newMessageArray
		// if no messages left for the channel, remove the channel from the map
		if len(newMessageArray) == 0 {
			delete(*messages, channel)
		}
	}

	if errMsg != "" {
		return tempBlacklist, xerrors.New(errMsg)
	}
	return tempBlacklist, nil
}

// handleReceivedMessage handle a message obtained by the server receiving a
// getMessagesById result
func (h *Hub) handleReceivedMessage(socket socket.Socket, messageData message.Message, targetChannel string) error {
	signature := messageData.Signature
	messageID := messageData.MessageID
	data := messageData.Data
	log.Info().Msgf("Received message on %s", targetChannel)

	expectedMessageID := messagedata.Hash(data, signature)
	if expectedMessageID != messageID {
		return xerrors.Errorf(wrongMessageIdError,
			expectedMessageID, messageID)
	}

	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: message2.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "publish",
		},

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: targetChannel,
			Message: messageData,
		},
	}
	_, stored := h.hubInbox.GetMessage(publish.Params.Message.MessageID)
	if stored {
		h.log.Info().Msgf("Already stored message %s", publish.Params.Message.MessageID)
		return nil
	}

	if publish.Params.Channel == rootChannel {
		err := h.handleRootChannelPublishMessage(socket, publish)
		if err != nil {
			return xerrors.Errorf(rootChannelErr, err)
		}
		return nil
	}

	channel, err := h.getChan(publish.Params.Channel)
	if err != nil {
		return xerrors.Errorf(getChannelErr, err)
	}

	err = channel.Publish(publish, socket)
	if err != nil {
		return xerrors.Errorf(publishError, err)
	}

	h.hubInbox.StoreMessage(publish.Params.Channel, publish.Params.Message)
	return nil
}

//-----------------------Helper methods for message handling---------------------------

// getMissingIds compares two maps of channel Ids associated to slices of message Ids to
// determine the missing Ids from the storedIds map with respect to the receivedIds map
func getMissingIds(receivedIds map[string][]string, storedIds map[string][]string, blacklist *state.ThreadSafeSlice[string]) map[string][]string {
	missingIds := make(map[string][]string)
	for channelId, receivedMessageIds := range receivedIds {
		for _, messageId := range receivedMessageIds {
			blacklisted := blacklist.Contains(messageId)
			storedIdsForChannel, channelKnown := storedIds[channelId]
			if blacklisted {
				break
			}
			if channelKnown {
				contains := slices.Contains(storedIdsForChannel, messageId)
				if !contains {
					missingIds[channelId] = append(missingIds[channelId], messageId)
				}
			} else {
				missingIds[channelId] = append(missingIds[channelId], messageId)
			}
		}
	}
	return missingIds
}

// getMissingMessages retrieves the missing messages from the inbox given their Ids
func (h *Hub) getMissingMessages(missingIds map[string][]string) (map[string][]message.Message, error) {
	missingMsgs := make(map[string][]message.Message)
	for channelId, messageIds := range missingIds {
		for _, messageId := range messageIds {
			msg, exists := h.hubInbox.GetMessage(messageId)
			if !exists {
				return nil, xerrors.Errorf("Message %s not found in hub inbox", messageId)
			}
			missingMsgs[channelId] = append(missingMsgs[channelId], *msg)
		}
	}
	return missingMsgs, nil
}

// sendGetMessagesByIdToServer sends a getMessagesById message to a server
func (h *Hub) sendGetMessagesByIdToServer(socket socket.Socket, missingIds map[string][]string) error {
	queryId := h.queries.GetNextID()

	getMessagesById := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: message2.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "get_messages_by_id",
		},
		ID:     queryId,
		Params: missingIds,
	}

	buf, err := json.Marshal(getMessagesById)
	if err != nil {
		return xerrors.Errorf("failed to marshal getMessagesById query: %v", err)
	}

	socket.Send(buf)

	h.queries.AddQuery(queryId, getMessagesById)

	return nil
}
