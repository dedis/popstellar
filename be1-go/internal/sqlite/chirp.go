package sqlite

import (
	"encoding/base64"
	"encoding/json"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/message/query/method/message"
	"time"
)

func (s *SQLite) StoreChirpMessages(channel, generalChannel string, msg, generalMsg message.Message) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return poperrors.NewDatabaseTransactionBeginErrorMsg("%v", err)
	}
	defer tx.Rollback()

	msgBytes, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return err
	}
	generalMsgBytes, err := json.Marshal(generalMsg)
	if err != nil {
		return err
	}
	generalMessageData, err := base64.URLEncoding.DecodeString(generalMsg.Data)
	if err != nil {
		return err
	}
	storedTime := time.Now().UnixNano()

	err = s.insertMessageHelper(tx, msg.MessageID, msgBytes, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, channel, msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertMessage, generalMsg.MessageID, generalMsgBytes, generalMessageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, generalChannel, generalMsg.MessageID, false)
	if err != nil {
		return err
	}
	err = tx.Commit()
	return err
}
