package standard_hub

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/crypto"
	jsonrpc "popstellar/internal/message"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/network/socket"
	"popstellar/internal/old/hub/standard_hub/hub_state"
	"popstellar/internal/validation"

	"github.com/rs/zerolog/log"

	"go.dedis.ch/kyber/v3/sign/schnorr"

	"golang.org/x/exp/slices"
	"golang.org/x/xerrors"
)

const (
	publishError        = "failed to publish: %v"
	wrongMessageIdError = "message_id is wrong: expected %q found %q"
	maxRetry            = 10
)

// handleRootChannelPublishMessage handles an incoming publish message on the root channel.
func (h *Hub) handleRootChannelPublishMessage(sock socket.Socket, publish method.Publish) error {
	jsonData, err := base64.URLEncoding.DecodeString(publish.Params.Message.Data)
	if err != nil {
		err := answer.NewInvalidMessageFieldError("failed to decode message data: %v", err)

		return err
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		err := answer.NewInvalidMessageFieldError("failed to validate message against json schema: %v", err)
		return err
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		err := answer.NewInvalidMessageFieldError("failed to get object#action: %v", err)
		return err
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		err := answer.NewInvalidMessageFieldError("only lao#create is allowed on root, "+
			"but found %s#%s", object, action)
		return err
	}

	var laoCreate messagedata.LaoCreate

	err = publish.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		return err
	}

	err = laoCreate.Verify()
	if err != nil {
		h.log.Err(err).Msg("invalid lao#create message " + err.Error())
		return err
	}

	err = h.createLao(publish.Params.Message, laoCreate, sock)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		return err
	}

	h.hubInbox.StoreMessage(publish.Params.Channel, publish.Params.Message)
	return nil
}

// handleRootChannelPublishMessage handles an incoming publish message on the root channel.
func (h *Hub) handleRootChannelBroadcastMessage(sock socket.Socket,
	broadcast method.Broadcast,
) error {
	jsonData, err := base64.URLEncoding.DecodeString(broadcast.Params.Message.Data)
	if err != nil {
		err := xerrors.Errorf("failed to decode message data: %v", err)
		sock.SendError(nil, err)
		return err
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		err := xerrors.Errorf("failed to validate message against json schema: %v", err)
		sock.SendError(nil, err)
		return err
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		err := xerrors.Errorf("failed to get object#action: %v", err)
		sock.SendError(nil, err)
		return err
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		err := xerrors.Errorf("only lao#create is allowed on root, but found %s#%s",
			object, action)
		sock.SendError(nil, err)
		return err
	}

	var laoCreate messagedata.LaoCreate

	err = broadcast.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		sock.SendError(nil, err)
		return err
	}

	err = laoCreate.Verify()
	if err != nil {
		h.log.Err(err).Msg("invalid lao#create message")
		sock.SendError(nil, err)
		return err
	}

	err = h.createLao(broadcast.Params.Message, laoCreate, sock)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		sock.SendError(nil, err)
		return err
	}

	h.hubInbox.StoreMessage(broadcast.Params.Channel, broadcast.Params.Message)
	return nil
}

// handleRootCatchup handles an incoming catchup message on the root channel
func (h *Hub) handleRootCatchup(senderSocket socket.Socket,
	byteMessage []byte,
) ([]message.Message, int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	if catchup.Params.Channel != rootChannel {
		return nil, catchup.ID, xerrors.Errorf("server catchup message can only " +
			"be sent on /root channel")
	}

	messages := h.hubInbox.GetRootMessages()

	return messages, catchup.ID, nil
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

func (h *Hub) handlePublish(socket socket.Socket, byteMessage []byte) (int, error) {
	var publish method.Publish

	err := json.Unmarshal(byteMessage, &publish)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	signature := publish.Params.Message.Signature
	messageID := publish.Params.Message.MessageID
	data := publish.Params.Message.Data

	dataBytes, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return publish.ID, xerrors.Errorf("failed to decode data string: %v", err)
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(publish.Params.Message.Sender)
	if err != nil {
		h.log.Info().Msg("Sender is : " + publish.Params.Message.Sender)
		return publish.ID, answer.NewInvalidMessageFieldError("failed to decode public key string: %v", err)
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(signature)
	if err != nil {
		return publish.ID, answer.NewInvalidMessageFieldError("failed to decode signature string: %v", err)
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		return publish.ID, answer.NewInvalidMessageFieldError("failed to verify signature : %v", err)
	}

	expectedMessageID := messagedata.Hash(data, signature)
	if expectedMessageID != messageID {
		return publish.ID, answer.NewInvalidMessageFieldError(wrongMessageIdError,
			expectedMessageID, messageID)
	}

	if publish.Params.Channel == rootChannel {
		err := h.handleRootChannelPublishMessage(socket, publish)
		if err != nil {
			return publish.ID, err
		}
		h.hubInbox.StoreMessage(publish.Params.Channel, publish.Params.Message)
		return publish.ID, nil
	}

	_, alreadyReceived := h.hubInbox.GetMessage(publish.Params.Message.MessageID)
	if alreadyReceived {
		return publish.ID, xerrors.Errorf("message %s was already received", publish.Params.Message.MessageID)
	}

	channel, err := h.getChan(publish.Params.Channel)
	if err != nil {
		return publish.ID, answer.NewInvalidMessageFieldError(getChannelErr, err)
	}

	err = channel.Publish(publish, socket)
	if err != nil {
		return publish.ID, answer.NewInvalidMessageFieldError(publishError, err)
	}

	h.hubInbox.StoreMessage(publish.Params.Channel, publish.Params.Message)
	return publish.ID, nil
}

func (h *Hub) handleBroadcast(socket socket.Socket, byteMessage []byte) error {
	var broadcast method.Broadcast

	err := json.Unmarshal(byteMessage, &broadcast)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	signature := broadcast.Params.Message.Signature
	messageID := broadcast.Params.Message.MessageID
	data := broadcast.Params.Message.Data

	expectedMessageID := messagedata.Hash(data, signature)
	if expectedMessageID != messageID {
		return xerrors.Errorf(wrongMessageIdError,
			expectedMessageID, messageID)
	}

	_, ok := h.hubInbox.GetMessage(broadcast.Params.Message.MessageID)
	if ok {
		h.log.Info().Msg("message was already received")
		return nil
	}
	h.hubInbox.StoreMessage(broadcast.Params.Channel, broadcast.Params.Message)

	if err != nil {
		return xerrors.Errorf("failed to broadcast message: %v", err)
	}

	if broadcast.Params.Channel == rootChannel {
		err := h.handleRootChannelBroadcastMessage(socket, broadcast)
		if err != nil {
			return xerrors.Errorf(rootChannelErr, err)
		}
		return nil
	}

	channel, err := h.getChan(broadcast.Params.Channel)
	if err != nil {
		return xerrors.Errorf(getChannelErr, err)
	}

	err = channel.Broadcast(broadcast, socket)
	if err != nil {
		return xerrors.Errorf(publishError, err)
	}

	return nil
}

func (h *Hub) handleSubscribe(socket socket.Socket, byteMessage []byte) (int, error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(byteMessage, &subscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal subscribe message: %v", err)
	}

	channel, err := h.getChan(subscribe.Params.Channel)
	if err != nil {
		return subscribe.ID, xerrors.Errorf("failed to get subscribe channel: %v", err)
	}

	err = channel.Subscribe(socket, subscribe)
	if err != nil {
		return subscribe.ID, xerrors.Errorf(publishError, err)
	}

	return subscribe.ID, nil
}

func (h *Hub) handleUnsubscribe(socket socket.Socket, byteMessage []byte) (int, error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(byteMessage, &unsubscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal unsubscribe message: %v", err)
	}

	channel, err := h.getChan(unsubscribe.Params.Channel)
	if err != nil {
		return unsubscribe.ID, xerrors.Errorf("failed to get unsubscribe channel: %v", err)
	}

	err = channel.Unsubscribe(socket.ID(), unsubscribe)
	if err != nil {
		return unsubscribe.ID, xerrors.Errorf("failed to unsubscribe: %v", err)
	}

	return unsubscribe.ID, nil
}

func (h *Hub) handleCatchup(socket socket.Socket,
	byteMessage []byte,
) ([]message.Message, int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	if catchup.Params.Channel == rootChannel {
		return h.handleRootCatchup(socket, byteMessage)
	}

	channel, err := h.getChan(catchup.Params.Channel)
	if err != nil {
		return nil, catchup.ID, xerrors.Errorf("failed to get catchup channel: %v", err)
	}

	msg := channel.Catchup(catchup)
	if err != nil {
		return nil, catchup.ID, xerrors.Errorf("failed to catchup: %v", err)
	}

	return msg, catchup.ID, nil
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

//-----------------------Helper methods for message handling---------------------------

// getMissingIds compares two maps of channel Ids associated to slices of message Ids to
// determine the missing Ids from the storedIds map with respect to the receivedIds map
func getMissingIds(receivedIds map[string][]string, storedIds map[string][]string, blacklist *hub_state.ThreadSafeSlice[string]) map[string][]string {
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
			JSONRPCBase: jsonrpc.JSONRPCBase{
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
