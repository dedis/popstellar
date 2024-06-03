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
	jsonrpc "popstellar/internal/message"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/types"
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

// StoreChannel mainly used for testing and storing the root channel
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

// GetAllMessagesFromChannel returns all the messages received + sent on a channel sorted by stored time.
func (s *SQLite) GetAllMessagesFromChannel(channelPath string) ([]message.Message, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectAllMessagesFromChannel, channelPath)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

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
		return make(map[string][]message.Message), nil
	}

	rows, err := s.database.Query("SELECT message, channelPath "+
		"FROM message JOIN channelMessage on message.messageID = channelMessage.messageID "+
		"WHERE isBaseChannel = ? "+
		"AND message.messageID IN ("+strings.Repeat("?,", len(interfaces)-2)+"?"+") ", interfaces...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

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
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectBaseChannelMessages, true)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

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
		return nil, err
	}
	defer rows.Close()

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

func (s *SQLite) GetLAOOrganizerPubKey(electionPath string) (kyber.Point, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

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
	dbLock.Lock()
	defer dbLock.Unlock()

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

	var state string
	err := s.database.QueryRow(selectLastElectionMessage, electionPath, messagedata.ElectionObject, messagedata.VoteActionCastVote).Scan(&state)
	if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return "", err
	}
	return state, nil
}

func (s *SQLite) IsElectionStartedOrEnded(electionPath string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	state, err := s.getElectionState(electionPath)
	if err != nil {
		return false, err
	}

	return state == messagedata.ElectionActionOpen || state == messagedata.ElectionActionEnd, nil
}

func (s *SQLite) IsElectionStarted(electionPath string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	state, err := s.getElectionState(electionPath)
	if err != nil {
		return false, err
	}
	return state == messagedata.ElectionActionOpen, nil
}

func (s *SQLite) IsElectionEnded(electionPath string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	state, err := s.getElectionState(electionPath)
	if err != nil {
		return false, err
	}
	return state == messagedata.ElectionActionEnd, nil
}

func (s *SQLite) GetElectionCreationTime(electionPath string) (int64, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var creationTime int64
	err := s.database.QueryRow(selectElectionCreationTime, electionPath, messagedata.ElectionObject, messagedata.ElectionActionSetup).Scan(&creationTime)
	if err != nil {
		return 0, err
	}
	return creationTime, nil
}

func (s *SQLite) GetElectionType(electionPath string) (string, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var electionType string
	err := s.database.QueryRow(selectElectionType, electionPath, messagedata.ElectionObject, messagedata.ElectionActionSetup).Scan(&electionType)
	if err != nil {
		return "", err
	}
	return electionType, nil
}

func (s *SQLite) GetElectionAttendees(electionPath string) (map[string]struct{}, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

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
	dbLock.Lock()
	defer dbLock.Unlock()

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
	dbLock.Lock()
	defer dbLock.Unlock()

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
	defer rows.Close()

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

	err = s.insertMessageHelper(tx, msg.MessageID, msgBytes, messageData, storedTime)
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

func (s *SQLite) IsAttendee(laoPath, poptoken string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

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
	dbLock.Lock()
	defer dbLock.Unlock()

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

func (s *SQLite) StoreRumor(rumorID int, sender string, unprocessed map[string][]message.Message, processed []string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}

	_, err = tx.Exec(insertRumor, rumorID, sender)
	if err != nil {
		return err
	}

	for channelPath, messages := range unprocessed {
		for _, msg := range messages {
			_, err = tx.Exec(insertUnprocessedMessage, msg.MessageID, channelPath, msg)
			if err != nil {
				return err
			}
			_, err = tx.Exec(insertUnprocessedMessageRumor, msg.MessageID, rumorID, sender)
			if err != nil {
				return err
			}
		}
	}

	for _, msgID := range processed {
		_, err = tx.Exec(insertMessageRumor, msgID, rumorID, sender)
		if err != nil {
			return err
		}
	}

	return tx.Commit()
}

func (s *SQLite) GetUnprocessedMessagesByChannel() (map[string][]message.Message, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectAllUnprocessedMessages)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	result := make(map[string][]message.Message)

	for rows.Next() {
		var channelPath string
		var messageByte []byte
		if err = rows.Scan(&channelPath, &messageByte); err != nil {
			return nil, err
		}
		var msg message.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, err
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
		return -1, err
	}
	defer tx.Rollback()

	_, err = s.database.Exec(insertMessageToMyRumor, messageID, serverKeysPath)
	if err != nil {
		return -1, err
	}
	var count int
	err = s.database.QueryRow(selectCountMyRumor, serverKeysPath).Scan(&count)
	if err != nil {
		return -1, err
	}

	err = tx.Commit()
	if err != nil {
		return -1, err
	}
	return count, nil
}

func (s *SQLite) GetAndIncrementMyRumor() (bool, method.Rumor, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return false, method.Rumor{}, err
	}
	defer tx.Rollback()

	rows, err := s.database.Query(selectMyRumorMessages, true, serverKeysPath, serverKeysPath)
	if err != nil {
		return false, method.Rumor{}, err
	}
	defer rows.Close()

	messages := make(map[string][]message.Message)
	for rows.Next() {
		var msgBytes []byte
		var channelPath string
		if err = rows.Scan(&msgBytes, &channelPath); err != nil {
			return false, method.Rumor{}, err
		}

		var msg message.Message
		if err = json.Unmarshal(msgBytes, &msg); err != nil {
			return false, method.Rumor{}, err
		}

		messages[channelPath] = append(messages[channelPath], msg)
	}

	if len(messages) == 0 {
		return false, method.Rumor{}, nil
	}

	var rumorID int
	var sender string
	err = tx.QueryRow(selectMyRumorInfos, serverKeysPath).Scan(&rumorID, &sender)
	if err != nil {
		return false, method.Rumor{}, err
	}

	rumor := newRumor(rumorID, sender, messages)

	_, err = tx.Exec(insertRumor, rumorID+1, sender)
	if err != nil {
		return false, method.Rumor{}, err
	}

	err = tx.Commit()
	if err != nil {
		return false, method.Rumor{}, err
	}

	return true, rumor, nil
}

func newRumor(rumorID int, sender string, messages map[string][]message.Message) method.Rumor {
	params := method.ParamsRumor{
		RumorID:  rumorID,
		SenderID: sender,
		Messages: messages,
	}

	return method.Rumor{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "rumor",
		},
		Params: params,
	}
}

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
