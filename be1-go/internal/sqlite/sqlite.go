package sqlite

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"errors"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
	_ "modernc.org/sqlite"
	"popstellar/internal/crypto"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
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
		return nil, nil, err
	}

	serverPubBuf, err := base64.URLEncoding.DecodeString(serverPubBuf64)
	if err != nil {
		return nil, nil, err
	}

	serverSecBuf, err := base64.URLEncoding.DecodeString(serverSecBuf64)
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

func (s *SQLite) insertMessageHelper(tx *sql.Tx, messageID string, msg, messageData []byte, storedTime int64) error {
	_, err := tx.Exec(insertMessage, messageID, msg, messageData, storedTime)
	if err != nil {
		return err

	}
	_, err = tx.Exec(tranferUnprocessedMessageRumor, messageID)
	if err != nil {
		return err
	}
	_, err = tx.Exec(deleteUnprocessedMessageRumor, messageID)
	if err != nil {
		return err
	}
	_, err = tx.Exec(deleteUnprocessedMessage, messageID)
	return err
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
	err = s.insertMessageHelper(tx, msg.MessageID, msgByte, messageData, time.Now().UnixNano())
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
	defer rows.Close()
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
	dbLock.Lock()
	defer dbLock.Unlock()

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
	defer rows.Close()

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
	dbLock.Lock()
	defer dbLock.Unlock()

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

// StoreChannel mainly used for testing purposes.
func (s *SQLite) StoreChannel(channelPath, channelType, laoPath string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	_, err := s.database.Exec(insertChannel, channelPath, channelTypeToID[channelType], laoPath)
	return err
}

func (s *SQLite) GetAllChannels() ([]string, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectAllChannels)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

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
	dbLock.Lock()
	defer dbLock.Unlock()

	var channelType string
	err := s.database.QueryRow(selectChannelType, channelPath).Scan(&channelType)
	return channelType, err
}

//======================================================================================================================
// ChannelRepository interface implementation
//======================================================================================================================

func (s *SQLite) HasChannel(channelPath string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

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
	dbLock.Lock()
	defer dbLock.Unlock()

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

//======================================================================================================================
// LaoRepository interface implementation
//======================================================================================================================

func (s *SQLite) GetOrganizerPubKey(laoPath string) (kyber.Point, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

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

//======================================================================================================================
// ElectionRepository interface implementation
//======================================================================================================================

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

//======================================================================================================================
// ReactionRepository interface implementation
//======================================================================================================================

//======================================================================================================================
// FederationRepository interface implementation
//======================================================================================================================

func (s *SQLite) IsChallengeValid(senderPk string, challenge messagedata.FederationChallenge, channelPath string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	var federationChallengeBytes []byte
	err := s.database.QueryRow(selectValidFederationChallenges, channelPath,
		senderPk, messagedata.FederationObject,
		messagedata.FederationActionChallenge, challenge.Value,
		challenge.ValidUntil).Scan(&federationChallengeBytes)
	if err != nil {
		return err
	}

	var federationChallenge messagedata.FederationChallenge
	err = json.Unmarshal(federationChallengeBytes, &federationChallenge)
	if err != nil {
		return err
	}

	if federationChallenge != challenge {
		return xerrors.New("the federation challenge doesn't match")
	}

	return nil
}

func (s *SQLite) RemoveChallenge(challenge messagedata.FederationChallenge) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	result, err := s.database.Exec(deleteFederationChallenge,
		messagedata.FederationObject,
		messagedata.FederationActionChallenge, challenge.Value,
		challenge.ValidUntil)
	if err != nil {
		return err
	}

	nb, err := result.RowsAffected()
	if err != nil {
		return err
	}

	if nb != 1 {
		return xerrors.New("unexpected number of rows affected")
	}

	return nil
}

func (s *SQLite) GetFederationExpect(senderPk string, remotePk string, challenge messagedata.FederationChallenge, channelPath string) (messagedata.FederationExpect, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectFederationExpects, channelPath,
		senderPk, messagedata.FederationObject,
		messagedata.FederationActionExpect, remotePk)
	if err != nil {
		return messagedata.FederationExpect{}, err
	}
	defer rows.Close()

	// iterate over all FederationExpect sent from the given sender pk,
	// and search the one matching the given FederationChallenge
	for rows.Next() {
		var federationExpectBytes []byte
		err = rows.Scan(&federationExpectBytes)
		if err != nil {
			continue
		}

		var federationExpect messagedata.FederationExpect
		err = json.Unmarshal(federationExpectBytes, &federationExpect)
		if err != nil {
			continue
		}

		var federationChallenge messagedata.FederationChallenge
		errAnswer := federationExpect.ChallengeMsg.UnmarshalMsgData(&federationChallenge)
		if errAnswer != nil {
			return messagedata.FederationExpect{}, errAnswer
		}

		if federationChallenge == challenge {
			return federationExpect, nil
		}
	}

	return messagedata.FederationExpect{}, sql.ErrNoRows
}

func (s *SQLite) GetFederationInit(senderPk string, remotePk string, challenge messagedata.FederationChallenge, channelPath string) (messagedata.FederationInit, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectFederationExpects, channelPath,
		senderPk, messagedata.FederationObject,
		messagedata.FederationActionInit, remotePk)
	if err != nil {
		return messagedata.FederationInit{}, err
	}
	defer rows.Close()

	// iterate over all FederationInit sent from the given sender pk,
	// and search the one matching the given FederationChallenge
	for rows.Next() {
		var federationInitBytes []byte
		err = rows.Scan(&federationInitBytes)
		if err != nil {
			continue
		}

		var federationInit messagedata.FederationInit
		err = json.Unmarshal(federationInitBytes, &federationInit)
		if err != nil {
			continue
		}

		var federationChallenge messagedata.FederationChallenge
		errAnswer := federationInit.ChallengeMsg.UnmarshalMsgData(&federationChallenge)
		if errAnswer != nil {
			return messagedata.FederationInit{}, errAnswer
		}

		if federationChallenge == challenge {
			return federationInit, nil
		}
	}

	return messagedata.FederationInit{}, sql.ErrNoRows
}
