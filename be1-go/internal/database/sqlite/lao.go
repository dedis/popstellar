package sqlite

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"errors"
	"go.dedis.ch/kyber/v3"
	poperrors "popstellar/internal/errors"
	messageHandler "popstellar/internal/handler/message"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"time"
)

func (s *SQLite) GetRollCallState(channelPath string) (string, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var state string
	err := s.database.QueryRow(selectLastRollCallMessage, messagedata.RollCallObject, channelPath).Scan(&state)
	if err != nil {
		return "", poperrors.NewDatabaseSelectErrorMsg(err.Error())
	}
	return state, nil
}

func (s *SQLite) CheckPrevOpenOrReopenID(channel, nextID string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var lastMsg []byte
	var lastAction string

	err := s.database.QueryRow(selectLastRollCallMessageInList, channel, messagedata.RollCallObject,
		messagedata.RollCallActionOpen, messagedata.RollCallActionReOpen).Scan(&lastMsg, &lastAction)

	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil {
		return false, poperrors.NewDatabaseSelectErrorMsg("last roll call open or re open action and message: %v", err)
	}

	switch lastAction {
	case messagedata.RollCallActionOpen:
		var rollCallOpen messagedata.RollCallOpen
		err = json.Unmarshal(lastMsg, &rollCallOpen)
		if err != nil {
			return false, poperrors.NewInternalServerError("failed to unmarshal last roll call open message: %v", err)
		}
		return rollCallOpen.UpdateID == nextID, nil
	case messagedata.RollCallActionReOpen:
		var rollCallReOpen messagedata.RollCallReOpen
		err = json.Unmarshal(lastMsg, &rollCallReOpen)
		if err != nil {
			return false, poperrors.NewInternalServerError("failed to unmarshal last roll call re open message: %v", err)
		}
		return rollCallReOpen.UpdateID == nextID, nil
	}

	return false, nil
}

func (s *SQLite) CheckPrevCreateOrCloseID(channel, nextID string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var lastMsg []byte
	var lastAction string

	err := s.database.QueryRow(selectLastRollCallMessageInList, channel, messagedata.RollCallObject,
		messagedata.RollCallActionCreate, messagedata.RollCallActionClose).Scan(&lastMsg, &lastAction)

	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil {
		return false, poperrors.NewDatabaseSelectErrorMsg("last roll call create or close action and message: %v", err)
	}

	switch lastAction {
	case messagedata.RollCallActionCreate:
		var rollCallCreate messagedata.RollCallCreate
		err = json.Unmarshal(lastMsg, &rollCallCreate)
		if err != nil {
			return false, poperrors.NewInternalServerError("failed to unmarshal last roll call create message: %v", err)
		}
		return rollCallCreate.ID == nextID, nil
	case messagedata.RollCallActionClose:
		var rollCallClose messagedata.RollCallClose
		err = json.Unmarshal(lastMsg, &rollCallClose)
		if err != nil {
			return false, poperrors.NewInternalServerError("failed to unmarshal last roll call close message: %v", err)
		}
		return rollCallClose.UpdateID == nextID, nil
	}

	return false, nil
}

func (s *SQLite) GetLaoWitnesses(laoPath string) (map[string]struct{}, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var witnesses []string
	err := s.database.QueryRow(selectLaoWitnesses, laoPath, messagedata.LAOObject, messagedata.LAOActionCreate).Scan(&witnesses)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("lao witnesses: %v", err)
	}

	var witnessesMap = make(map[string]struct{})
	for _, witness := range witnesses {
		witnessesMap[witness] = struct{}{}
	}

	return witnessesMap, nil
}

func (s *SQLite) StoreRollCallClose(channels []string, laoPath string, msg message.Message) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}
	defer tx.Rollback()

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return poperrors.NewDecodeStringError("roll call close message data: %v", err)
	}

	msgBytes, err := json.Marshal(msg)
	if err != nil {
		return poperrors.NewJsonMarshalError("roll call close message: %v", err)
	}

	err = s.insertMessageHelper(tx, msg.MessageID, msgBytes, messageData, time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, laoPath, msg.MessageID, true)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation roll call close message and lao channel: %v", err)
	}

	if len(channels) == 0 {
		err = tx.Commit()
		if err != nil {
			return poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
		}
		return nil
	}

	for _, channelPath := range channels {
		_, err = tx.Exec(insertChannel, channelPath, channelTypeToID[messageHandler.ChirpType], laoPath)
		if err != nil {
			return poperrors.NewDatabaseInsertErrorMsg("channel %s: %v", channelPath, err)
		}
	}
	err = tx.Commit()
	if err != nil {
		return poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}
	return nil

}

func (s *SQLite) storeElectionHelper(
	tx *sql.Tx,
	storedTime int64,
	laoPath, electionPath string,
	electionPubKey kyber.Point,
	electionSecretKey kyber.Scalar,
	msg message.Message) error {

	msgBytes, err := json.Marshal(msg)
	if err != nil {
		return poperrors.NewJsonMarshalError("election create message: %v", err)
	}
	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return poperrors.NewDecodeStringError("election create message data: %v", err)
	}

	electionPubBuf, err := electionPubKey.MarshalBinary()
	if err != nil {
		return poperrors.NewKeyMarshalError("election public key: %v", err)
	}
	electionSecretBuf, err := electionSecretKey.MarshalBinary()
	if err != nil {
		return poperrors.NewKeyMarshalError("election secret key: %v", err)
	}

	err = s.insertMessageHelper(tx, msg.MessageID, msgBytes, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, laoPath, msg.MessageID, true)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation election create message and lao channel: %v", err)
	}
	_, err = tx.Exec(insertChannel, electionPath, channelTypeToID[messageHandler.ElectionType], laoPath)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("election channel: %v", err)
	}
	_, err = tx.Exec(insertChannelMessage, electionPath, msg.MessageID, false)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation election create message and election channel: %v", err)
	}
	_, err = tx.Exec(insertKeys, electionPath, electionPubBuf, electionSecretBuf)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("election keys: %v", err)
	}

	return nil
}

func (s *SQLite) StoreElection(
	laoPath, electionPath string,
	electionPubKey kyber.Point,
	electionSecretKey kyber.Scalar,
	msg message.Message) error {

	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}
	defer tx.Rollback()

	storedTime := time.Now().UnixNano()

	err = s.storeElectionHelper(tx, storedTime, laoPath, electionPath, electionPubKey, electionSecretKey, msg)
	if err != nil {
		return err
	}

	err = tx.Commit()
	if err != nil {
		return poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}
	return nil
}

func (s *SQLite) StoreElectionWithElectionKey(
	laoPath, electionPath string,
	electionPubKey kyber.Point,
	electionSecretKey kyber.Scalar,
	msg, electionKeyMsg message.Message) error {

	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return poperrors.NewDatabaseTransactionBeginErrorMsg(err.Error())
	}
	defer tx.Rollback()

	storedTime := time.Now().UnixNano()

	err = s.storeElectionHelper(tx, storedTime, laoPath, electionPath, electionPubKey, electionSecretKey, msg)
	if err != nil {
		return err
	}

	electionKey, err := base64.URLEncoding.DecodeString(electionKeyMsg.Data)
	if err != nil {
		return poperrors.NewDecodeStringError("failed to decode election key message data: %v", err)
	}
	electionKeyMsgBytes, err := json.Marshal(electionKeyMsg)
	if err != nil {
		return poperrors.NewInternalServerError("failed to marshal election key message: %v", err)
	}

	_, err = tx.Exec(insertMessage, electionKeyMsg.MessageID, electionKeyMsgBytes, electionKey, storedTime)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("election key message: %v", err)
	}
	_, err = tx.Exec(insertChannelMessage, electionPath, electionKeyMsg.MessageID, false)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation election key message and election channel: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return poperrors.NewDatabaseTransactionCommitErrorMsg(err.Error())
	}
	return nil
}
