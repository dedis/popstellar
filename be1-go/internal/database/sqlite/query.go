package sqlite

import (
	"database/sql"
	"encoding/json"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/query/mquery"
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
	for channelPath, messageIDs := range params {
		for _, messageID := range messageIDs {
			if _, ok := result[messageID]; !ok {
				missingIDs[channelPath] = append(missingIDs[channelPath], messageID)
			}
		}
	}
	return missingIDs, nil
}

func (s *SQLite) CheckRumor(senderID string, rumorID int, timestamp mrumor.RumorTimestamp) (bool, bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return false, false, poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}
	defer tx.Rollback()

	myTimestamp, err := s.GetRumorTimestampHelper(tx)
	if err != nil {
		return false, false, err
	}
	err = tx.Commit()
	if err != nil {
		return false, false, poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}

	isValid := false
	alreadyExist := false

	curRumorID, ok := myTimestamp[senderID]
	if ok && rumorID <= curRumorID {
		alreadyExist = true
	} else if (!ok && rumorID != 0) || (ok && rumorID > curRumorID+1) {
		isValid = false
	} else {
		isValid = true
		for senderID1, rumorID1 := range timestamp {
			rumorID2, ok := myTimestamp[senderID1]
			if !ok || rumorID1 > rumorID2 {
				isValid = false
				break
			}
		}
	}
	return isValid, alreadyExist, nil
}

func (s *SQLite) StoreRumor(rumorID int, sender string, timestamp mrumor.RumorTimestamp,
	unprocessed map[string][]mmessage.Message, processed []string) error {

	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}

	timestampBuf, err := json.Marshal(timestamp)
	if err != nil {
		return poperrors.NewJsonMarshalError("rumor timestamp: %v", err)
	}

	_, err = tx.Exec(insertRumor, rumorID, sender, timestampBuf)
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

	timestamp, err := s.GetRumorTimestampHelper(tx)
	if err != nil {
		return false, mrumor.Rumor{}, err
	}

	rumor := newRumor(rumorID, sender, messages, timestamp)
	timestamp[sender] = rumorID + 1
	timestampBuf, err := json.Marshal(timestamp)
	if err != nil {
		return false, mrumor.Rumor{}, poperrors.NewJsonMarshalError("rumor timestamp: %v", err)
	}

	_, err = tx.Exec(insertRumor, rumorID+1, sender, timestampBuf)
	if err != nil {
		return false, mrumor.Rumor{}, poperrors.NewDatabaseInsertErrorMsg("rumor: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return false, mrumor.Rumor{}, poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}

	return true, rumor, nil
}

func newRumor(rumorID int, sender string, messages map[string][]mmessage.Message, timestamp mrumor.RumorTimestamp) mrumor.Rumor {
	params := mrumor.ParamsRumor{
		RumorID:   rumorID,
		SenderID:  sender,
		Messages:  messages,
		Timestamp: timestamp,
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

func (s *SQLite) GetRumorTimestampHelper(tx *sql.Tx) (mrumor.RumorTimestamp, error) {

	rows, err := tx.Query(selectRumorState)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("rumor state: %v", err)
	}
	defer rows.Close()

	timestamp := make(mrumor.RumorTimestamp)

	for rows.Next() {
		var sender string
		var rumorID int
		if err = rows.Scan(&rumorID, &sender); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg(err.Error())
		}
		timestamp[sender] = rumorID
	}

	if err = rows.Err(); err != nil {
		return nil, poperrors.NewDatabaseIteratorErrorMsg(err.Error())
	}

	var rumorID int
	var sender string
	err = tx.QueryRow(selectMyRumorInfos, serverKeysPath).Scan(&rumorID, &sender)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("current rumor id and sender: %v", err)
	}

	if rumorID == 0 {
		delete(timestamp, sender)
	} else {
		timestamp[sender] = timestamp[sender] - 1
	}

	return timestamp, nil
}

func (s *SQLite) GetRumorTimestamp() (mrumor.RumorTimestamp, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return nil, poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}
	defer tx.Rollback()

	timestamp, err := s.GetRumorTimestampHelper(tx)
	if err != nil {
		return nil, err
	}

	err = tx.Commit()
	if err != nil {
		return nil, poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}

	return timestamp, nil
}

func (s *SQLite) GetAllRumors() ([]mrumor.Rumor, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectAllRumors)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("all rumors: %v", err)
	}
	defer rows.Close()

	rumors := make([]mrumor.Rumor, 0)
	for rows.Next() {
		var rumorID int
		var sender string
		var timestampByte []byte
		if err = rows.Scan(&rumorID, &sender, &timestampByte); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg(err.Error())
		}
		var timestamp mrumor.RumorTimestamp
		if err = json.Unmarshal(timestampByte, &timestamp); err != nil {
			return nil, poperrors.NewInternalServerError("failed to unmarshal timestamp: %v", err)
		}

		messages, err := s.GetMessagesFromRumor(rumorID, sender)
		if err != nil {
			return nil, err
		}

		rumor := newRumor(rumorID, sender, messages, timestamp)
		rumors = append(rumors, rumor)
	}

	if err = rows.Err(); err != nil {
		return nil, poperrors.NewDatabaseIteratorErrorMsg(err.Error())
	}

	return rumors, nil
}

func (s *SQLite) GetMessagesFromRumor(rumorID int, sender string) (map[string][]mmessage.Message, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectRumorMessages, true, sender, rumorID)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("messages from rumor %d: %v", rumorID, err)
	}
	defer rows.Close()

	messages := make(map[string][]mmessage.Message)
	for rows.Next() {
		var channelPath string
		var messageByte []byte
		if err = rows.Scan(&channelPath, &messageByte); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg(err.Error())
		}
		var msg mmessage.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, poperrors.NewInternalServerError("failed to unmarshal message: %v", err)
		}
		messages[channelPath] = append(messages[channelPath], msg)
	}

	if err = rows.Err(); err != nil {
		return nil, poperrors.NewDatabaseIteratorErrorMsg(err.Error())
	}

	return messages, nil
}
