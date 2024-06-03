package sqlite

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"errors"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/message/query/method/message"
	"time"
)

func (s *SQLite) StoreLaoWithLaoGreet(
	channels map[string]string,
	laoPath string,
	organizerPubBuf []byte,
	msg, laoGreetMsg message.Message) error {

	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return poperrors.NewDatabaseTransactionBeginErrorMsg("%v", err)
	}

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return poperrors.NewJsonMarshalError("lao create message: %v", err)
	}
	laoGreetMsgByte, err := json.Marshal(laoGreetMsg)
	if err != nil {
		return poperrors.NewInternalServerError("lao greet message: %v", err)
	}

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return poperrors.NewDecodeStringError("lao create message data in database: %v", err)
	}
	laoGreetData, err := base64.URLEncoding.DecodeString(laoGreetMsg.Data)
	if err != nil {
		return poperrors.NewInternalServerError("failed to decode string: lao greet message data in database: %v", err)
	}

	storedTime := time.Now().UnixNano()

	for channel, channelType := range channels {
		_, err = tx.Exec(insertChannel, channel, channelTypeToID[channelType], laoPath)
		if err != nil {
			return poperrors.NewDatabaseInsertErrorMsg("channel %s: %v", channel, err)
		}
	}

	err = s.insertMessageHelper(tx, msg.MessageID, msgByte, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, "/root", msg.MessageID, true)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation lao create message and root channel: %v", err)
	}

	_, err = tx.Exec(insertChannelMessage, laoPath, msg.MessageID, false)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation lao create message and lao channel: %v", err)
	}

	_, err = tx.Exec(insertPublicKey, laoPath, organizerPubBuf)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("lao organizer public key: %v", err)
	}
	_, err = tx.Exec(insertMessage, laoGreetMsg.MessageID, laoGreetMsgByte, laoGreetData, storedTime)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("lao greet message: %v", err)
	}
	_, err = tx.Exec(insertChannelMessage, laoPath, laoGreetMsg.MessageID, false)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation lao greet message lao channel: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return poperrors.NewDatabaseTransactionCommitErrorMsg("%v", err)
	}

	return nil
}

func (s *SQLite) HasChannel(channelPath string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var channel string
	err := s.database.QueryRow(selectChannelPath, channelPath).Scan(&channel)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return false, poperrors.NewDatabaseSelectErrorMsg("channel: %v", err)
	} else {
		return true, nil
	}
}
