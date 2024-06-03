package sqlite

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/errors"
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
		return errors.NewDatabaseTransactionBeginErrorMsg("%v", err)
	}

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return errors.NewJsonMarshalError("lao create message: %v", err)
	}
	laoGreetMsgByte, err := json.Marshal(laoGreetMsg)
	if err != nil {
		return errors.NewInternalServerError("lao greet message: %v", err)
	}

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewDecodeStringError("lao create message data in database: %v", err)
	}
	laoGreetData, err := base64.URLEncoding.DecodeString(laoGreetMsg.Data)
	if err != nil {
		return errors.NewInternalServerError("failed to decode string: lao greet message data in database: %v", err)
	}

	storedTime := time.Now().UnixNano()

	for channel, channelType := range channels {
		_, err = tx.Exec(insertChannel, channel, channelTypeToID[channelType], laoPath)
		if err != nil {
			return errors.NewDatabaseInsertErrorMsg("channel %s: %v", channel, err)
		}
	}

	err = s.insertMessageHelper(tx, msg.MessageID, msgByte, messageData, storedTime)
	if err != nil {
		return errors.NewDatabaseInsertErrorMsg("lao create message: %v", err)
	}
	_, err = tx.Exec(insertChannelMessage, "/root", msg.MessageID, true)
	if err != nil {
		return errors.NewDatabaseInsertErrorMsg("association of lao create message with root channel: %v", err)
	}

	_, err = tx.Exec(insertChannelMessage, laoPath, msg.MessageID, false)
	if err != nil {
		return errors.NewDatabaseInsertErrorMsg("association of lao create message with lao channel: %v", err)
	}

	_, err = tx.Exec(insertPublicKey, laoPath, organizerPubBuf)
	if err != nil {
		return errors.NewDatabaseInsertErrorMsg("lao organizer public key: %v", err)
	}
	_, err = tx.Exec(insertMessage, laoGreetMsg.MessageID, laoGreetMsgByte, laoGreetData, storedTime)
	if err != nil {
		return errors.NewDatabaseInsertErrorMsg("lao greet message: %v", err)
	}
	_, err = tx.Exec(insertChannelMessage, laoPath, laoGreetMsg.MessageID, false)
	if err != nil {
		return errors.NewDatabaseInsertErrorMsg("association of lao greet message with lao channel: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return errors.NewDatabaseTransactionCommitErrorMsg("%v", err)
	}

	return nil
}
