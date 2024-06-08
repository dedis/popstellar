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
		return poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}
	defer tx.Rollback()

	msgBytes, err := json.Marshal(msg)
	if err != nil {
		return poperrors.NewJsonMarshalError("chirp message: %v", err)
	}
	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return poperrors.NewDecodeStringError("chirp message data: %v", err)
	}
	generalMsgBytes, err := json.Marshal(generalMsg)
	if err != nil {
		return poperrors.NewInternalServerError("failed to marshal general chirp message: %v", err)
	}
	generalMessageData, err := base64.URLEncoding.DecodeString(generalMsg.Data)
	if err != nil {
		return poperrors.NewInternalServerError("failed to decode general chirp message data: %v", err)
	}
	storedTime := time.Now().UnixNano()

	err = s.insertMessageHelper(tx, msg.MessageID, msgBytes, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, channel, msg.MessageID, true)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation chirp message and chirp channel: %v", err)
	}
	_, err = tx.Exec(insertMessage, generalMsg.MessageID, generalMsgBytes, generalMessageData, storedTime)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("general chirp message: %v", err)
	}
	_, err = tx.Exec(insertChannelMessage, generalChannel, generalMsg.MessageID, false)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation general chirp message and general chirp channel: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}

	return nil
}
