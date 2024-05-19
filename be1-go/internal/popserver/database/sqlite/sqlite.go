package sqlite

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"errors"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
	_ "modernc.org/sqlite"
	"popstellar/crypto"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strings"
	"time"
)

func (s *SQLite) StoreServerKeys(electionPubKey kyber.Point, electionSecretKey kyber.Scalar) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	electionPubBuf, err := electionPubKey.MarshalBinary()
	if err != nil {
		return err
	}
	electionSecBuf, err := electionSecretKey.MarshalBinary()
	if err != nil {
		return err
	}

	_, err = tx.Exec(insertKeys, serverKeysPath, electionPubBuf, electionSecBuf)
	if err != nil {
		return err
	}

	return tx.Commit()
}

func (s *SQLite) GetServerKeys() (kyber.Point, kyber.Scalar, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var serverPubBuf []byte
	var serverSecBuf []byte
	err := s.database.QueryRow(selectKeys, serverKeysPath).Scan(&serverPubBuf, &serverSecBuf)
	if err != nil {
		return nil, nil, err
	}
	serverPubKey := crypto.Suite.Point()
	err = serverPubKey.UnmarshalBinary(serverPubBuf)
	if err != nil {
		return nil, nil, err
	}
	serverSecKey := crypto.Suite.Scalar()
	err = serverSecKey.UnmarshalBinary(serverSecBuf)
	if err != nil {
		return nil, nil, err
	}

	return serverPubKey, serverSecKey, nil
}

func (s *SQLite) StoreMessageAndData(channelPath string, msg message.Message) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if err = addPendingSignatures(tx, &msg); err != nil {
		return err
	}

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return err
	}

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertMessage, msg.MessageID, msgByte, messageData, time.Now().UnixNano())
	if err != nil {
		return err

	}
	_, err = tx.Exec(insertChannelMessage, channelPath, msg.MessageID, true)
	if err != nil {
		return err

	}
	return tx.Commit()
}

func addPendingSignatures(tx *sql.Tx, msg *message.Message) error {
	rows, err := tx.Query(selectPendingSignatures, msg.MessageID)
	if err != nil {
		return err
	}

	for rows.Next() {
		var witness string
		var signature string
		if err = rows.Scan(&witness, &signature); err != nil {
			return err
		}
		msg.WitnessSignatures = append(msg.WitnessSignatures, message.WitnessSignature{
			Witness:   witness,
			Signature: signature,
		})
	}

	if err = rows.Err(); err != nil {
		return err
	}

	_, err = tx.Exec(deletePendingSignatures, msg.MessageID)
	return err
}

// GetMessagesByID returns a set of messages by their IDs.
func (s *SQLite) GetMessagesByID(IDs []string) (map[string]message.Message, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	if len(IDs) == 0 {
		return make(map[string]message.Message), nil
	}

	IDsInterface := make([]interface{}, len(IDs))
	for i, v := range IDs {
		IDsInterface[i] = v
	}
	rows, err := s.database.Query("SELECT messageID, message "+
		"FROM message "+
		"WHERE messageID IN ("+strings.Repeat("?,", len(IDs)-1)+"?"+")", IDsInterface...)
	if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return nil, err
	} else if errors.Is(err, sql.ErrNoRows) {
		return make(map[string]message.Message), nil
	}

	messagesByID := make(map[string]message.Message, len(IDs))
	for rows.Next() {
		var messageID string
		var messageByte []byte
		if err = rows.Scan(&messageID, &messageByte); err != nil {
			return nil, err
		}

		var msg message.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, err
		}
		messagesByID[messageID] = msg
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}
	return messagesByID, nil
}

// GetMessageByID returns a message by its ID.
func (s *SQLite) GetMessageByID(ID string) (message.Message, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var messageByte []byte
	err := s.database.QueryRow(selectMessage, ID).Scan(&messageByte)
	if err != nil {
		return message.Message{}, err
	}

	var msg message.Message
	if err = json.Unmarshal(messageByte, &msg); err != nil {
		return message.Message{}, err
	}
	return msg, nil
}

// AddWitnessSignature stores a pending signature inside the SQLite database.
func (s *SQLite) AddWitnessSignature(messageID string, witness string, signature string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	witnessSignature, err := json.Marshal(message.WitnessSignature{
		Witness:   witness,
		Signature: signature,
	})
	if err != nil {
		return err
	}

	res, err := tx.Exec(updateMsg, witnessSignature, messageID)
	if err != nil {
		return err
	}
	changes, err := res.RowsAffected()
	if err != nil {
		return err
	}
	if changes == 0 {
		_, err := tx.Exec(insertPendingSignatures, messageID, witness, signature)
		if err != nil {
			return err
		}
	}
	return tx.Commit()
}

// StoreChannel mainly used for testing and storing the root channel
func (s *SQLite) StoreChannel(channelPath, channelType, laoPath string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	_, err := s.database.Exec(insertChannel, channelPath, channelTypeToID[channelType], laoPath)
	return err
}

func (s *SQLite) GetAllChannels() ([]string, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	rows, err := s.database.Query(selectAllChannels)
	if err != nil {
		return nil, err
	}

	var channels []string
	for rows.Next() {
		var channelPath string
		if err = rows.Scan(&channelPath); err != nil {
			return nil, err
		}
		channels = append(channels, channelPath)
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}

	return channels, nil
}

//======================================================================================================================
// QueryRepository interface implementation
//======================================================================================================================

// GetChannelType returns the type of the channelPath.
func (s *SQLite) GetChannelType(channelPath string) (string, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var channelType string
	err := s.database.QueryRow(selectChannelType, channelPath).Scan(&channelType)
	return channelType, err
}

// GetAllMessagesFromChannel returns all the messages received + sent on a channel sorted by stored time.
func (s *SQLite) GetAllMessagesFromChannel(channelPath string) ([]message.Message, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	rows, err := s.database.Query(selectAllMessagesFromChannel, channelPath)
	if err != nil {
		return nil, err
	}

	messages := make([]message.Message, 0)
	for rows.Next() {
		var messageByte []byte
		if err = rows.Scan(&messageByte); err != nil {
			return nil, err
		}
		var msg message.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, err
		}
		messages = append(messages, msg)
	}

	if rows.Err() != nil {
		return nil, err
	}

	return messages, nil
}

func (s *SQLite) GetResultForGetMessagesByID(params map[string][]string) (map[string][]message.Message, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var interfaces []interface{}
	// isBaseChannel must be true
	interfaces = append(interfaces, true)
	for _, value := range params {
		for _, v := range value {
			interfaces = append(interfaces, v)
		}
	}

	if len(interfaces) == 1 {
		return make(map[string][]message.Message), nil
	}

	rows, err := s.database.Query("SELECT message, channelPath "+
		"FROM message JOIN channelMessage on message.messageID = channelMessage.messageID "+
		"WHERE isBaseChannel = ? "+
		"AND message.messageID IN ("+strings.Repeat("?,", len(interfaces)-2)+"?"+") ", interfaces...)
	if err != nil {
		return nil, err
	}

	result := make(map[string][]message.Message)
	for rows.Next() {
		var messageByte []byte
		var channelPath string
		if err = rows.Scan(&messageByte, &channelPath); err != nil {
			return nil, err
		}
		var msg message.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, err
		}
		result[channelPath] = append(result[channelPath], msg)
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}

	return result, nil
}

func (s *SQLite) GetParamsHeartbeat() (map[string][]string, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	rows, err := s.database.Query(selectBaseChannelMessages, true)
	if err != nil {
		return nil, err
	}

	result := make(map[string][]string)
	for rows.Next() {
		var channelPath string
		var messageID string
		if err = rows.Scan(&messageID, &channelPath); err != nil {
			return nil, err
		}
		if _, ok := result[channelPath]; !ok {
			result[channelPath] = make([]string, 0)
		}
		result[channelPath] = append(result[channelPath], messageID)
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}

	return result, nil
}

func (s *SQLite) GetParamsForGetMessageByID(params map[string][]string) (map[string][]string, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

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
		return nil, err
	}

	result := make(map[string]struct{})
	for rows.Next() {
		var messageID string
		var channelPath string
		if err = rows.Scan(&messageID, &channelPath); err != nil {
			return nil, err
		}
		result[messageID] = struct{}{}
	}

	if err = rows.Err(); err != nil {
		return nil, err
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

//======================================================================================================================
// ChannelRepository interface implementation
//======================================================================================================================

func (s *SQLite) HasChannel(channelPath string) (bool, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var c string
	err := s.database.QueryRow(selectChannelPath, channelPath).Scan(&c)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return false, err
	} else {
		return true, nil
	}
}

func (s *SQLite) HasMessage(messageID string) (bool, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var msgID string
	err := s.database.QueryRow(selectMessageID, messageID).Scan(&msgID)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return false, err
	} else {
		return true, nil
	}
}

//======================================================================================================================
// RootRepository interface implementation
//======================================================================================================================

func (s *SQLite) StoreLaoWithLaoGreet(
	channels map[string]string,
	laoPath string,
	organizerPubBuf []byte,
	msg, laoGreetMsg message.Message) error {

	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	laoGreetMsgByte, err := json.Marshal(laoGreetMsg)
	if err != nil {
		return err
	}

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return err
	}
	laoGreetData, err := base64.URLEncoding.DecodeString(laoGreetMsg.Data)
	if err != nil {
		return err
	}

	storedTime := time.Now().UnixNano()

	for channel, channelType := range channels {
		_, err = tx.Exec(insertChannel, channel, channelTypeToID[channelType], laoPath)
		if err != nil {
			return err
		}
	}

	_, err = tx.Exec(insertMessage, msg.MessageID, msgByte, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, "/root", msg.MessageID, true)
	if err != nil {
		return err
	}

	_, err = tx.Exec(insertChannelMessage, laoPath, msg.MessageID, false)
	if err != nil {
		return err
	}

	_, err = tx.Exec(insertPublicKey, laoPath, organizerPubBuf)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertMessage, laoGreetMsg.MessageID, laoGreetMsgByte, laoGreetData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, laoPath, laoGreetMsg.MessageID, false)
	if err != nil {
		return err
	}

	err = tx.Commit()
	if err != nil {
		return err
	}

	defer tx.Rollback()
	return nil
}

//======================================================================================================================
// LaoRepository interface implementation
//======================================================================================================================

func (s *SQLite) GetOrganizerPubKey(laoPath string) (kyber.Point, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var organizerPubBuf []byte
	err := s.database.QueryRow(selectPublicKey, laoPath).Scan(&organizerPubBuf)
	if err != nil {
		return nil, err
	}
	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerPubBuf)
	if err != nil {
		return nil, err
	}
	return organizerPubKey, nil
}

func (s *SQLite) GetRollCallState(channelPath string) (string, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var state string
	err := s.database.QueryRow(selectLastRollCallMessage, messagedata.RollCallObject, channelPath).Scan(&state)
	if err != nil {
		return "", err
	}
	return state, nil
}

func (s *SQLite) CheckPrevOpenOrReopenID(channel, nextID string) (bool, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var lastMsg []byte
	var lastAction string

	err := s.database.QueryRow(selectLastRollCallMessageInList, channel, messagedata.RollCallObject,
		messagedata.RollCallActionOpen, messagedata.RollCallActionReOpen).Scan(&lastMsg, &lastAction)

	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil {
		return false, err
	}

	switch lastAction {
	case messagedata.RollCallActionOpen:
		var rollCallOpen messagedata.RollCallOpen
		err = json.Unmarshal(lastMsg, &rollCallOpen)
		if err != nil {
			return false, err
		}
		return rollCallOpen.UpdateID == nextID, nil
	case messagedata.RollCallActionReOpen:
		var rollCallReOpen messagedata.RollCallReOpen
		err = json.Unmarshal(lastMsg, &rollCallReOpen)
		if err != nil {
			return false, err
		}
		return rollCallReOpen.UpdateID == nextID, nil
	}

	return false, nil
}

func (s *SQLite) CheckPrevCreateOrCloseID(channel, nextID string) (bool, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var lastMsg []byte
	var lastAction string

	err := s.database.QueryRow(selectLastRollCallMessageInList, channel, messagedata.RollCallObject,
		messagedata.RollCallActionCreate, messagedata.RollCallActionClose).Scan(&lastMsg, &lastAction)

	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil {
		return false, err
	}

	switch lastAction {
	case messagedata.RollCallActionCreate:
		var rollCallCreate messagedata.RollCallCreate
		err = json.Unmarshal(lastMsg, &rollCallCreate)
		if err != nil {
			return false, err
		}
		return rollCallCreate.ID == nextID, nil
	case messagedata.RollCallActionClose:
		var rollCallClose messagedata.RollCallClose
		err = json.Unmarshal(lastMsg, &rollCallClose)
		if err != nil {
			return false, err
		}
		return rollCallClose.UpdateID == nextID, nil
	}

	return false, nil
}

func (s *SQLite) GetLaoWitnesses(laoPath string) (map[string]struct{}, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var witnesses []string
	err := s.database.QueryRow(selectLaoWitnesses, laoPath, messagedata.LAOObject, messagedata.LAOActionCreate).Scan(&witnesses)
	if err != nil {
		return nil, err
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
		return err
	}
	defer tx.Rollback()

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return err
	}

	msgBytes, err := json.Marshal(msg)
	if err != nil {
		return err
	}

	_, err = tx.Exec(insertMessage, msg.MessageID, msgBytes, messageData, time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, laoPath, msg.MessageID, true)
	if err != nil {
		return err
	}
	for _, channel := range channels {
		_, err = tx.Exec(insertChannel, channel, channelTypeToID[ChirpType], laoPath)
		if err != nil {
			return err
		}
	}
	err = tx.Commit()
	if err != nil {
		return err
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
		return err
	}
	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return err
	}

	electionPubBuf, err := electionPubKey.MarshalBinary()
	if err != nil {
		return err
	}
	electionSecretBuf, err := electionSecretKey.MarshalBinary()
	if err != nil {
		return err
	}

	_, err = tx.Exec(insertMessage, msg.MessageID, msgBytes, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, laoPath, msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannel, electionPath, channelTypeToID[ElectionType], laoPath)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, electionPath, msg.MessageID, false)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertKeys, electionPath, electionPubBuf, electionSecretBuf)
	if err != nil {
		return err
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
		return err
	}
	defer tx.Rollback()

	storedTime := time.Now().UnixNano()

	err = s.storeElectionHelper(tx, storedTime, laoPath, electionPath, electionPubKey, electionSecretKey, msg)
	if err != nil {
		return err
	}

	return tx.Commit()
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
		return err
	}
	defer tx.Rollback()

	storedTime := time.Now().UnixNano()

	err = s.storeElectionHelper(tx, storedTime, laoPath, electionPath, electionPubKey, electionSecretKey, msg)
	if err != nil {
		return err
	}

	electionKey, err := base64.URLEncoding.DecodeString(electionKeyMsg.Data)
	if err != nil {
		return err
	}
	electionKeyMsgBytes, err := json.Marshal(electionKeyMsg)
	if err != nil {
		return err
	}

	_, err = tx.Exec(insertMessage, electionKeyMsg.MessageID, electionKeyMsgBytes, electionKey, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, electionPath, electionKeyMsg.MessageID, false)
	if err != nil {
		return err
	}

	return tx.Commit()
}

//======================================================================================================================
// ElectionRepository interface implementation
//======================================================================================================================

func (s *SQLite) GetLAOOrganizerPubKey(electionPath string) (kyber.Point, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	tx, err := s.database.Begin()
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	var electionPubBuf []byte
	err = tx.QueryRow(selectLaoOrganizerKey, electionPath).Scan(&electionPubBuf)
	if err != nil {
		return nil, err
	}
	electionPubKey := crypto.Suite.Point()
	err = electionPubKey.UnmarshalBinary(electionPubBuf)
	if err != nil {
		return nil, err
	}

	err = tx.Commit()
	if err != nil {
		return nil, err
	}

	return electionPubKey, nil
}

func (s *SQLite) GetElectionSecretKey(electionPath string) (kyber.Scalar, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var electionSecretBuf []byte
	err := s.database.QueryRow(selectSecretKey, electionPath).Scan(&electionSecretBuf)
	if err != nil {
		return nil, err
	}

	electionSecretKey := crypto.Suite.Scalar()
	err = electionSecretKey.UnmarshalBinary(electionSecretBuf)
	if err != nil {
		return nil, err
	}
	return electionSecretKey, nil
}

func (s *SQLite) getElectionState(electionPath string) (string, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var state string
	err := s.database.QueryRow(selectLastElectionMessage, electionPath, messagedata.ElectionObject, messagedata.VoteActionCastVote).Scan(&state)
	if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return "", err
	}
	return state, nil
}

func (s *SQLite) IsElectionStartedOrEnded(electionPath string) (bool, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	state, err := s.getElectionState(electionPath)
	if err != nil {
		return false, err
	}

	return state == messagedata.ElectionActionOpen || state == messagedata.ElectionActionEnd, nil
}

func (s *SQLite) IsElectionStarted(electionPath string) (bool, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	state, err := s.getElectionState(electionPath)
	if err != nil {
		return false, err
	}
	return state == messagedata.ElectionActionOpen, nil
}

func (s *SQLite) IsElectionEnded(electionPath string) (bool, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	state, err := s.getElectionState(electionPath)
	if err != nil {
		return false, err
	}
	return state == messagedata.ElectionActionEnd, nil
}

func (s *SQLite) GetElectionCreationTime(electionPath string) (int64, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var creationTime int64
	err := s.database.QueryRow(selectElectionCreationTime, electionPath, messagedata.ElectionObject, messagedata.ElectionActionSetup).Scan(&creationTime)
	if err != nil {
		return 0, err
	}
	return creationTime, nil
}

func (s *SQLite) GetElectionType(electionPath string) (string, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var electionType string
	err := s.database.QueryRow(selectElectionType, electionPath, messagedata.ElectionObject, messagedata.ElectionActionSetup).Scan(&electionType)
	if err != nil {
		return "", err
	}
	return electionType, nil
}

func (s *SQLite) GetElectionAttendees(electionPath string) (map[string]struct{}, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var rollCallCloseBytes []byte
	err := s.database.QueryRow(selectElectionAttendees,
		electionPath,
		messagedata.RollCallObject,
		messagedata.RollCallActionClose,
		messagedata.RollCallObject,
		messagedata.RollCallActionClose,
	).Scan(&rollCallCloseBytes)
	if err != nil {
		return nil, err
	}

	var rollCallClose messagedata.RollCallClose
	err = json.Unmarshal(rollCallCloseBytes, &rollCallClose)
	if err != nil {
		return nil, err
	}

	attendeesMap := make(map[string]struct{})
	for _, attendee := range rollCallClose.Attendees {
		attendeesMap[attendee] = struct{}{}
	}
	return attendeesMap, nil
}

func (s *SQLite) getElectionSetup(electionPath string, tx *sql.Tx) (messagedata.ElectionSetup, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var electionSetupBytes []byte
	err := tx.QueryRow(selectElectionSetup, electionPath, messagedata.ElectionObject, messagedata.ElectionActionSetup).Scan(&electionSetupBytes)
	if err != nil {
		return messagedata.ElectionSetup{}, err
	}

	var electionSetup messagedata.ElectionSetup
	err = json.Unmarshal(electionSetupBytes, &electionSetup)
	if err != nil {
		return messagedata.ElectionSetup{}, err
	}
	return electionSetup, nil

}

func (s *SQLite) GetElectionQuestions(electionPath string) (map[string]types.Question, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	tx, err := s.database.Begin()
	if err != nil {
		return nil, err

	}
	defer tx.Rollback()

	electionSetup, err := s.getElectionSetup(electionPath, tx)
	if err != nil {
		return nil, err

	}
	questions, err := getQuestionsFromMessage(electionSetup)
	if err != nil {
		return nil, err
	}

	err = tx.Commit()
	if err != nil {
		return nil, err

	}
	return questions, nil
}

func (s *SQLite) GetElectionQuestionsWithValidVotes(electionPath string) (map[string]types.Question, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	tx, err := s.database.Begin()
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	electionSetup, err := s.getElectionSetup(electionPath, tx)
	if err != nil {
		return nil, err
	}
	questions, err := getQuestionsFromMessage(electionSetup)
	if err != nil {
		return nil, err
	}

	rows, err := tx.Query(selectCastVotes, electionPath, messagedata.ElectionObject, messagedata.VoteActionCastVote)
	if err != nil {
		return nil, err
	}

	for rows.Next() {
		var voteBytes []byte
		var msgID string
		var sender string
		if err = rows.Scan(&voteBytes, &msgID, &sender); err != nil {
			return nil, err
		}
		var vote messagedata.VoteCastVote
		err = json.Unmarshal(voteBytes, &vote)
		if err != nil {
			return nil, err
		}
		err = updateVote(msgID, sender, vote, questions)
		if err != nil {
			return nil, err
		}
	}
	if err = rows.Err(); err != nil {
		return nil, err
	}
	err = tx.Commit()
	if err != nil {
		return nil, err
	}
	return questions, nil
}

func getQuestionsFromMessage(electionSetup messagedata.ElectionSetup) (map[string]types.Question, error) {
	questions := make(map[string]types.Question)
	for _, question := range electionSetup.Questions {
		ballotOptions := make([]string, len(question.BallotOptions))
		copy(ballotOptions, question.BallotOptions)
		_, ok := questions[question.ID]
		if ok {
			return nil, xerrors.Errorf("duplicate question ID")
		}
		questions[question.ID] = types.Question{
			ID:            []byte(question.ID),
			BallotOptions: ballotOptions,
			ValidVotes:    make(map[string]types.ValidVote),
			Method:        question.VotingMethod,
		}
	}
	return questions, nil
}

func updateVote(msgID, sender string, castVote messagedata.VoteCastVote, questions map[string]types.Question) error {
	for idx, vote := range castVote.Votes {
		question, ok := questions[vote.Question]
		if !ok {
			return xerrors.Errorf("question not found for vote number %d sent by %s", idx, sender)
		}
		earlierVote, ok := question.ValidVotes[sender]
		if !ok || earlierVote.VoteTime < castVote.CreatedAt {
			question.ValidVotes[sender] = types.ValidVote{
				MsgID:    msgID,
				ID:       vote.ID,
				VoteTime: castVote.CreatedAt,
				Index:    vote.Vote,
			}
		}
	}
	return nil
}

func (s *SQLite) StoreElectionEndWithResult(channelPath string, msg, electionResultMsg message.Message) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
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
	electionResult, err := base64.URLEncoding.DecodeString(electionResultMsg.Data)
	if err != nil {
		return err
	}
	electionResultMsgBytes, err := json.Marshal(electionResultMsg)
	if err != nil {
		return err
	}
	storedTime := time.Now().UnixNano()

	_, err = tx.Exec(insertMessage, msg.MessageID, msgBytes, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, channelPath, msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertMessage, electionResultMsg.MessageID, electionResultMsgBytes, electionResult, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec(insertChannelMessage, channelPath, electionResultMsg.MessageID, false)
	if err != nil {
		return err
	}
	err = tx.Commit()
	return err
}

//======================================================================================================================
// ChirpRepository interface implementation
//======================================================================================================================

func (s *SQLite) StoreChirpMessages(channel, generalChannel string, msg, generalMsg message.Message) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
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

	_, err = tx.Exec(insertMessage, msg.MessageID, msgBytes, messageData, storedTime)
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

//======================================================================================================================
// ReactionRepository interface implementation
//======================================================================================================================

func (s *SQLite) IsAttendee(laoPath, poptoken string) (bool, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var rollCallCloseBytes []byte
	err := s.database.QueryRow(selectLastRollCallClose, laoPath, messagedata.RollCallObject, messagedata.RollCallActionClose).Scan(&rollCallCloseBytes)
	if err != nil {
		return false, err
	}

	var rollCallClose messagedata.RollCallClose
	err = json.Unmarshal(rollCallCloseBytes, &rollCallClose)
	if err != nil {
		return false, err
	}

	for _, attendee := range rollCallClose.Attendees {
		if attendee == poptoken {
			return true, nil
		}
	}

	return false, nil
}

func (s *SQLite) GetReactionSender(messageID string) (string, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var sender string
	var object string
	var action string
	err := s.database.QueryRow(selectSender, messageID).Scan(&sender, &object, &action)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return "", nil
	} else if err != nil {
		return "", err

	}

	if object != messagedata.ReactionObject || action != messagedata.ReactionActionAdd {
		return "", xerrors.New("unexpected object or action")
	}
	return sender, nil
}

func (s *SQLite) HasRumor(senderID string, rumorID int) (bool, error) {
	dbLock.RLock()
	defer dbLock.RUnlock()

	var id int
	err := s.database.QueryRow(selectRumor, rumorID, senderID).Scan(&id)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil {
		return false, err
	}
	return true, nil
}

func (s *SQLite) StoreRumor(rumorID, sender string, unprocessed []message.Message, processed []string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}

	for _, msg := range unprocessed {
		_, err = tx.Exec(insertUnprocessedMessage, msg.MessageID, msg)
		if err != nil {
			return err
		}
		_, err = tx.Exec(insertUnprocessedMessageRumor, msg.MessageID, rumorID, sender)
		if err != nil {
			return err
		}
	}

	var processedIDs []interface{}
	for _, msgID := range processed {
		_, err = tx.Exec(insertMessageRumor, msgID, rumorID, sender)
		processedIDs = append(processedIDs, msgID)
		if err != nil {
			return err
		}
	}

	_, err = tx.Exec("DELETE FROM unprocessedMessage  WHERE messageID IN ("+
		strings.Repeat("?,", len(processed)-1)+"?)", processedIDs...)

	if err != nil {
		return err
	}

	return tx.Commit()
}

func (s *SQLite) GetUnprocessedMessagesByChannel() (map[string][]message.Message, error) {
	//dbLock.RLock()
	//defer dbLock.RUnlock()
	//
	//rows, err := s.database.Query(selectAllUnprocessedMessages)
	//if err != nil {
	//	return nil, err
	//}
	//
	//result := make(map[string]map[string]message.Message)
	//
	//for rows.Next() {
	//	var channelPath string
	//	var messageID string
	//	var messageByte []byte
	//	if err = rows.Scan(&channelPath, &messageID, &messageByte); err != nil {
	//		return nil, err
	//	}
	//	var msg message.Message
	//	if err = json.Unmarshal(messageByte, &msg); err != nil {
	//		return nil, err
	//	}
	//	if _, ok := result[channelPath]; !ok {
	//		result[channelPath] = make(map[string]message.Message)
	//	}
	//	result[channelPath][messageID] = msg
	//}
	//return result, nil

	return nil, nil
}

func (s *SQLite) StoreMessageRumor(messageID string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	_, err := s.database.Exec(`
	WITH myKey AS (
    SELECT publicKey FROM key WHERE channelPath = ?
	)
	INSERT INTO messageRumor (messageID, rumorID, sender)
			SELECT ?, (SELECT max(id) FROM rumor where sender = (SELECT publicKey FROM myKey)),
			(SELECT publicKey FROM myKey)`, messageID, serverKeysPath)

	if err != nil {
		return err
	}

	return err
}
