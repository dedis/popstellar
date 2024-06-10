package sqlite

import (
	"database/sql"
	"encoding/json"
	"errors"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/message/method/mrumor"
	"popstellar/internal/message/mquery"
	"strings"
)

// GetAllMessagesFromChannel returns all the messages received + sent on a channel sorted by stored time.
func (s *SQLite) GetAllMessagesFromChannel(channelPath string) ([]mmessage.Message, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectAllMessagesFromChannel, channelPath)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("messages from channel %s: %v", channelPath, err)
	}
	defer rows.Close()

	messages := make([]mmessage.Message, 0)
	for rows.Next() {
		var messageByte []byte
		if err = rows.Scan(&messageByte); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg(err.Error())
		}
		var msg mmessage.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, poperrors.NewInternalServerError("failed to unmarshal message: %v", err)
		}
		messages = append(messages, msg)
	}

	if err = rows.Err(); err != nil {
		return nil, poperrors.NewDatabaseIteratorErrorMsg(err.Error())
	}

	return messages, nil
}

func (s *SQLite) GetResultForGetMessagesByID(params map[string][]string) (map[string][]mmessage.Message, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var interfaces []interface{}
	// isBaseChannel must be true
	interfaces = append(interfaces, true)
	for _, value := range params {
		for _, v := range value {
			interfaces = append(interfaces, v)
		}
	}

	if len(interfaces) == 1 {
		return make(map[string][]mmessage.Message), nil
	}

	rows, err := s.database.Query("SELECT message, channelPath "+
		"FROM message JOIN channelMessage on message.messageID = channelMessage.messageID "+
		"WHERE isBaseChannel = ? "+
		"AND message.messageID IN ("+strings.Repeat("?,", len(interfaces)-2)+"?"+") ", interfaces...)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("get messages by id: %v", err)
	}
	defer rows.Close()

	result := make(map[string][]mmessage.Message)
	for rows.Next() {
		var messageByte []byte
		var channelPath string
		if err = rows.Scan(&messageByte, &channelPath); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg(err.Error())
		}
		var msg mmessage.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, poperrors.NewInternalServerError("failed to unmarshal message: %v", err)
		}
		result[channelPath] = append(result[channelPath], msg)
	}

	if err = rows.Err(); err != nil {
		return nil, poperrors.NewDatabaseIteratorErrorMsg(err.Error())
	}

	return result, nil
}

func (s *SQLite) GetParamsForGetMessageByID(params map[string][]string) (map[string][]string, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var interfaces []interface{}
	// isBaseChannel must be true
	interfaces = append(interfaces, true)
	for _, value := range params {
		for _, v := range value {
			interfaces = append(interfaces, v)
		}
	}

	if len(interfaces) == 1 {
		return make(map[string][]string), nil
	}

	rows, err := s.database.Query("SELECT message.messageID, channelPath "+
		"FROM message JOIN channelMessage on message.messageID = channelMessage.messageID "+
		"WHERE isBaseChannel = ? "+
		"AND message.messageID IN ("+strings.Repeat("?,", len(interfaces)-2)+"?"+") ", interfaces...)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("get messages by id: %v", err)
	}
	defer rows.Close()

	result := make(map[string]struct{})
	for rows.Next() {
		var messageID string
		var channelPath string
		if err = rows.Scan(&messageID, &channelPath); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg(err.Error())
		}
		result[messageID] = struct{}{}
	}

	if err = rows.Err(); err != nil {
		return nil, poperrors.NewDatabaseIteratorErrorMsg(err.Error())
	}

	missingIDs := make(map[string][]string)
	for channel, messageIDs := range params {
		for _, messageID := range messageIDs {
			if _, ok := result[messageID]; !ok {
				missingIDs[channel] = append(missingIDs[channel], messageID)
			}
		}
	}
	return missingIDs, nil
}

func (s *SQLite) CheckRumor(senderID string, rumorID int) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var id int
	if rumorID == 0 {
		err := s.database.QueryRow(selectAnyRumor, senderID).Scan(&id)
		if err != nil && !errors.Is(err, sql.ErrNoRows) {
			return false, err
		} else if errors.Is(err, sql.ErrNoRows) {
			return true, nil
		}
		return false, nil
	}

	err := s.database.QueryRow(selectLastRumor, senderID).Scan(&id)
	if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return false, err
	} else if errors.Is(err, sql.ErrNoRows) {
		return false, nil
	}
	return id == rumorID-1, nil
}

//======================================================================================================================
// Rumor repository
//======================================================================================================================

func (s *SQLite) StoreRumor(rumorID int, sender string, unprocessed map[string][]mmessage.Message, processed []string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}

	_, err = tx.Exec(insertRumor, rumorID, sender)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("rumor: %v", err)
	}

	for channelPath, messages := range unprocessed {
		for _, msg := range messages {
			_, err = tx.Exec(insertUnprocessedMessage, msg.MessageID, channelPath, msg)
			if err != nil {
				return poperrors.NewDatabaseInsertErrorMsg("unprocessed message: %v", err)
			}
			_, err = tx.Exec(insertUnprocessedMessageRumor, msg.MessageID, rumorID, sender)
			if err != nil {
				return poperrors.NewDatabaseInsertErrorMsg("relation unprocessed message and rumor: %v", err)
			}
		}
	}

	for _, msgID := range processed {
		_, err = tx.Exec(insertMessageRumor, msgID, rumorID, sender)
		if err != nil {
			return poperrors.NewDatabaseInsertErrorMsg("relation message and rumor: %v", err)
		}
	}

	err = tx.Commit()
	if err != nil {
		return poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}
	return nil
}

func (s *SQLite) GetUnprocessedMessagesByChannel() (map[string][]mmessage.Message, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectAllUnprocessedMessages)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("all unprocessed messages: %v", err)
	}
	defer rows.Close()

	result := make(map[string][]mmessage.Message)

	for rows.Next() {
		var channelPath string
		var messageByte []byte
		if err = rows.Scan(&channelPath, &messageByte); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg("unprocessed message: %v", err)
		}
		var msg mmessage.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, poperrors.NewInternalServerError("failed to unmarshal unprocessed message: %v", err)
		}
		result[channelPath] = append(result[channelPath], msg)
	}
	return result, nil
}

func (s *SQLite) AddMessageToMyRumor(messageID string) (int, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return -1, poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}
	defer tx.Rollback()

	_, err = s.database.Exec(insertMessageToMyRumor, messageID, serverKeysPath)
	if err != nil {
		return -1, poperrors.NewDatabaseInsertErrorMsg("message to the current rumor: %v", err)
	}
	var count int
	err = s.database.QueryRow(selectCountMyRumor, serverKeysPath).Scan(&count)
	if err != nil {
		return -1, poperrors.NewDatabaseSelectErrorMsg("number of messages in the current rumor: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return -1, poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}
	return count, nil
}

func (s *SQLite) GetAndIncrementMyRumor() (bool, mrumor.Rumor, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return false, mrumor.Rumor{}, poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}
	defer tx.Rollback()

	rows, err := s.database.Query(selectMyRumorMessages, true, serverKeysPath, serverKeysPath)
	if err != nil {
		return false, mrumor.Rumor{}, poperrors.NewDatabaseSelectErrorMsg("current rumor messages: %v", err)
	}
	defer rows.Close()

	messages := make(map[string][]mmessage.Message)
	for rows.Next() {
		var msgBytes []byte
		var channelPath string
		if err = rows.Scan(&msgBytes, &channelPath); err != nil {
			return false, mrumor.Rumor{}, poperrors.NewDatabaseScanErrorMsg(err.Error())
		}

		var msg mmessage.Message
		if err = json.Unmarshal(msgBytes, &msg); err != nil {
			return false, mrumor.Rumor{}, poperrors.NewInternalServerError("failed to unmarshal current rumor message: %v", err)
		}

		messages[channelPath] = append(messages[channelPath], msg)
	}

	if len(messages) == 0 {
		return false, mrumor.Rumor{}, nil
	}

	var rumorID int
	var sender string
	err = tx.QueryRow(selectMyRumorInfos, serverKeysPath).Scan(&rumorID, &sender)
	if err != nil {
		return false, mrumor.Rumor{}, poperrors.NewDatabaseSelectErrorMsg("current rumor id and sender: %v", err)
	}

	rumor := newRumor(rumorID, sender, messages)

	_, err = tx.Exec(insertRumor, rumorID+1, sender)
	if err != nil {
		return false, mrumor.Rumor{}, poperrors.NewDatabaseInsertErrorMsg("rumor: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return false, mrumor.Rumor{}, poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}

	return true, rumor, nil
}

func newRumor(rumorID int, sender string, messages map[string][]mmessage.Message) mrumor.Rumor {
	params := mrumor.ParamsRumor{
		RumorID:  rumorID,
		SenderID: sender,
		Messages: messages,
	}

	return mrumor.Rumor{
		Base: mquery.Base{
			JSONRPCBase: mjsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "rumor",
		},
		Params: params,
	}
}
