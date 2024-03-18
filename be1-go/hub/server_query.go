package hub

import (
	"encoding/json"
	"golang.org/x/exp/slices"
	"golang.org/x/xerrors"
	"popstellar/hub/state"
	message2 "popstellar/message"
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
