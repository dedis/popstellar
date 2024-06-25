package sqlite

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"errors"
	"go.dedis.ch/kyber/v3"
	_ "modernc.org/sqlite"
	"popstellar/internal/crypto"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	"strings"
	"time"
)

func (s *SQLite) GetServerKeys() (kyber.Point, kyber.Scalar, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var serverPubBuf64 string
	var serverSecBuf64 string
	err := s.database.QueryRow(selectKeys, serverKeysPath).Scan(&serverPubBuf64, &serverSecBuf64)
	if err != nil {
		return nil, nil, poperrors.NewDatabaseSelectErrorMsg("server keys: %v", err)
	}

	serverPubBuf, err := base64.URLEncoding.DecodeString(serverPubBuf64)
	if err != nil {
		return nil, nil, poperrors.NewInternalServerError("failed to decode server public key: %v", err)
	}

	serverSecBuf, err := base64.URLEncoding.DecodeString(serverSecBuf64)
	if err != nil {
		return nil, nil, poperrors.NewInternalServerError("failed to decode server secret key: %v", err)
	}

	serverPubKey := crypto.Suite.Point()
	err = serverPubKey.UnmarshalBinary(serverPubBuf)
	if err != nil {
		return nil, nil, poperrors.NewKeyMarshalError("server public key: %v", err)
	}
	serverSecKey := crypto.Suite.Scalar()
	err = serverSecKey.UnmarshalBinary(serverSecBuf)
	if err != nil {
		return nil, nil, poperrors.NewKeyMarshalError("server secret key: %v", err)
	}

	return serverPubKey, serverSecKey, nil
}

func (s *SQLite) GetOrganizerPubKey(laoPath string) (kyber.Point, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var organizerPubBuf []byte
	err := s.database.QueryRow(selectPublicKey, laoPath).Scan(&organizerPubBuf)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("organizer public key: %v", err)
	}
	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerPubBuf)
	if err != nil {
		return nil, poperrors.NewKeyMarshalError("organizer public key: %v", err)
	}
	return organizerPubKey, nil
}

func (s *SQLite) insertMessageHelper(tx *sql.Tx, messageID string, msg, messageData []byte, storedTime int64) error {
	_, err := tx.Exec(insertMessage, messageID, msg, messageData, storedTime)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg(err.Error())

	}
	_, err = tx.Exec(tranferUnprocessedMessageRumor, messageID)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation message rumor from relation unprocessed message rumor: %v", err)
	}
	_, err = tx.Exec(deleteUnprocessedMessageRumor, messageID)
	if err != nil {
		return poperrors.NewDatabaseDeleteErrorMsg("relation unprocessed message rumor: %v", err)
	}
	_, err = tx.Exec(deleteUnprocessedMessage, messageID)
	if err != nil {
		return poperrors.NewDatabaseDeleteErrorMsg("unprocessed message: %v", err)
	}

	return nil
}

func (s *SQLite) StoreMessageAndData(channelPath string, msg mmessage.Message) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return poperrors.NewDecodeStringError("message data: %v", err)
	}

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return poperrors.NewJsonMarshalError(err.Error())
	}
	err = s.insertMessageHelper(tx, msg.MessageID, msgByte, messageData, time.Now().UnixNano())
	if err != nil {
		return err
	}

	_, err = tx.Exec(insertChannelMessage, channelPath, msg.MessageID, true)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation message and channel: %v", err)

	}

	err = tx.Commit()
	if err != nil {
		return poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}

	return nil
}

func (s *SQLite) HasMessage(messageID string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var msgID string
	err := s.database.QueryRow(selectMessageID, messageID).Scan(&msgID)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return false, poperrors.NewDatabaseSelectErrorMsg("message ID: %v", err)
	} else {
		return true, nil
	}
}

func (s *SQLite) GetParamsHeartbeat() (map[string][]string, error) {
	return nil, nil
}

func (s *SQLite) GetMessagesByIDUtil(IDs []string) (map[string]mmessage.Message, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	if len(IDs) == 0 {
		return make(map[string]mmessage.Message), nil
	}

	IDsInterface := make([]interface{}, len(IDs))
	for i, v := range IDs {
		IDsInterface[i] = v
	}
	rows, err := s.database.Query("SELECT messageID, message "+
		"FROM message "+
		"WHERE messageID IN ("+strings.Repeat("?,", len(IDs)-1)+"?"+")", IDsInterface...)
	if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return nil, poperrors.NewDatabaseSelectErrorMsg("messages: %v", err)
	} else if errors.Is(err, sql.ErrNoRows) {
		return make(map[string]mmessage.Message), nil
	}
	defer rows.Close()

	messagesByID := make(map[string]mmessage.Message, len(IDs))
	for rows.Next() {
		var messageID string
		var messageByte []byte
		if err = rows.Scan(&messageID, &messageByte); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg(err.Error())
		}

		var msg mmessage.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, poperrors.NewJsonUnmarshalError(err.Error())
		}
		messagesByID[messageID] = msg
	}

	if err = rows.Err(); err != nil {
		return nil, poperrors.NewDatabaseIteratorErrorMsg("messages: %v", err)
	}
	return messagesByID, nil
}

// GetMessageByIDUtil returns a message by its ID.
func (s *SQLite) GetMessageByIDUtil(ID string) (mmessage.Message, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var messageByte []byte
	err := s.database.QueryRow(selectMessage, ID).Scan(&messageByte)
	if err != nil {
		return mmessage.Message{}, poperrors.NewDatabaseSelectErrorMsg(err.Error())
	}

	var msg mmessage.Message
	if err = json.Unmarshal(messageByte, &msg); err != nil {
		return mmessage.Message{}, poperrors.NewJsonUnmarshalError(err.Error())
	}
	return msg, nil
}

func (s *SQLite) StoreChannelUtil(channelPath, channelType, laoPath string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	_, err := s.database.Exec(insertChannel, channelPath, channelTypeToID[channelType], laoPath)

	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("channel %s: %v", channelPath, err)
	}
	return nil
}

func (s *SQLite) StorePubKeyUtil(keyPath string, key []byte) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	_, err := s.database.Exec(insertKeys, keyPath, base64.URLEncoding.EncodeToString(key), nil)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("key %s: %v", keyPath, err)
	}
	return nil
}
