package sqlite

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"errors"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/types"
	"time"
)

func (s *SQLite) GetLAOOrganizerPubKey(electionPath string) (kyber.Point, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return nil, poperrors.NewDatabaseTransactionBeginErrorMsg("%v", err)
	}
	defer tx.Rollback()

	var organizerPubBuf []byte
	err = tx.QueryRow(selectLaoOrganizerKey, electionPath).
		Scan(&organizerPubBuf)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("lao organizer public key: %v", err)
	}

	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerPubBuf)
	if err != nil {
		return nil, poperrors.NewInternalServerError("failed to unmarshal lao organizer public key: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return nil, poperrors.NewDatabaseTransactionCommitErrorMsg("%v", err)
	}

	return organizerPubKey, nil
}

func (s *SQLite) GetElectionSecretKey(electionPath string) (kyber.Scalar, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var electionSecretBuf []byte
	err := s.database.QueryRow(selectSecretKey, electionPath).
		Scan(&electionSecretBuf)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("election secret key: %v", err)
	}

	electionSecretKey := crypto.Suite.Scalar()
	err = electionSecretKey.UnmarshalBinary(electionSecretBuf)
	if err != nil {
		return nil, poperrors.NewInternalServerError("failed to unmarshal election secret key: %v", err)
	}
	return electionSecretKey, nil
}

func (s *SQLite) getElectionState(electionPath string) (string, error) {

	var state string
	err := s.database.QueryRow(selectLastElectionMessage,
		electionPath,
		messagedata.ElectionObject,
		messagedata.VoteActionCastVote).
		Scan(&state)

	if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return "", poperrors.NewDatabaseSelectErrorMsg("election state: %v", err)
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
	err := s.database.QueryRow(selectElectionCreationTime, electionPath, messagedata.ElectionObject, messagedata.ElectionActionSetup).
		Scan(&creationTime)
	if err != nil {
		return 0, poperrors.NewDatabaseSelectErrorMsg("election creation time: %v", err)
	}
	return creationTime, nil
}

func (s *SQLite) GetElectionType(electionPath string) (string, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var electionType string
	err := s.database.QueryRow(selectElectionType,
		electionPath,
		messagedata.ElectionObject,
		messagedata.ElectionActionSetup).
		Scan(&electionType)

	if err != nil {
		return "", poperrors.NewDatabaseSelectErrorMsg("election type: %v", err)
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
		return nil, poperrors.NewDatabaseSelectErrorMsg("roll call close message data: %v", err)
	}

	var rollCallClose messagedata.RollCallClose
	err = json.Unmarshal(rollCallCloseBytes, &rollCallClose)
	if err != nil {
		return nil, poperrors.NewJsonUnmarshalError("roll call close message data: %v", err)
	}

	attendeesMap := make(map[string]struct{})
	for _, attendee := range rollCallClose.Attendees {
		attendeesMap[attendee] = struct{}{}
	}
	return attendeesMap, nil
}

func (s *SQLite) getElectionSetup(electionPath string, tx *sql.Tx) (messagedata.ElectionSetup, error) {

	var electionSetupBytes []byte
	err := tx.QueryRow(selectElectionSetup, electionPath, messagedata.ElectionObject, messagedata.ElectionActionSetup).
		Scan(&electionSetupBytes)
	if err != nil {
		return messagedata.ElectionSetup{}, poperrors.NewDatabaseSelectErrorMsg("election setup message data: %v", err)
	}

	var electionSetup messagedata.ElectionSetup
	err = json.Unmarshal(electionSetupBytes, &electionSetup)
	if err != nil {
		return messagedata.ElectionSetup{}, poperrors.NewJsonUnmarshalError("election setup message data: %v", err)
	}
	return electionSetup, nil
}

func (s *SQLite) GetElectionQuestions(electionPath string) (map[string]types.Question, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return nil, poperrors.NewDatabaseTransactionBeginErrorMsg("%v", err)

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
		return nil, poperrors.NewDatabaseTransactionCommitErrorMsg("%v", err)

	}
	return questions, nil
}

func (s *SQLite) GetElectionQuestionsWithValidVotes(electionPath string) (map[string]types.Question, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return nil, poperrors.NewDatabaseTransactionBeginErrorMsg("%v", err)
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
		return nil, poperrors.NewDatabaseSelectErrorMsg("cast vote messages: %v", err)
	}
	defer rows.Close()

	for rows.Next() {
		var voteBytes []byte
		var msgID string
		var sender string
		if err = rows.Scan(&voteBytes, &msgID, &sender); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg("cast vote message: %v", err)
		}
		var vote messagedata.VoteCastVote
		err = json.Unmarshal(voteBytes, &vote)
		if err != nil {
			return nil, poperrors.NewJsonUnmarshalError("cast vote message data: %v", err)
		}
		err = updateVote(msgID, sender, vote, questions)
		if err != nil {
			return nil, err
		}
	}
	if err = rows.Err(); err != nil {
		return nil, poperrors.NewDatabaseIteratorErrorMsg("cast vote messages: %v", err)
	}
	err = tx.Commit()
	if err != nil {
		return nil, poperrors.NewDatabaseTransactionCommitErrorMsg("%v", err)
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
			return nil, poperrors.NewInvalidMessageFieldError("duplicate question ID in election setup message data: %s", question.ID)
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
			return poperrors.NewInvalidMessageFieldError("question not found in election setup for vote number %d sent by %s", idx, sender)
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
		return poperrors.NewDatabaseTransactionBeginErrorMsg("%v", err)
	}
	defer tx.Rollback()

	msgBytes, err := json.Marshal(msg)
	if err != nil {
		return poperrors.NewJsonMarshalError("election end message: %v", err)
	}
	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return poperrors.NewDecodeStringError("election end message data: %v", err)
	}
	electionResult, err := base64.URLEncoding.DecodeString(electionResultMsg.Data)
	if err != nil {
		return poperrors.NewInternalServerError("failed to decode election result message data: %v", err)
	}
	electionResultMsgBytes, err := json.Marshal(electionResultMsg)
	if err != nil {
		return poperrors.NewJsonMarshalError("failed to marshal election result message: %v", err)
	}
	storedTime := time.Now().UnixNano()

	err = s.insertMessageHelper(tx, msg.MessageID, msgBytes, messageData, storedTime)
	if err != nil {
		return err

	}
	_, err = tx.Exec(insertChannelMessage, channelPath, msg.MessageID, true)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation election end message and election channel: %v", err)
	}
	_, err = tx.Exec(insertMessage, electionResultMsg.MessageID, electionResultMsgBytes, electionResult, storedTime)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("election result message: %v", err)
	}
	_, err = tx.Exec(insertChannelMessage, channelPath, electionResultMsg.MessageID, false)
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("relation election result message and election channel: %v", err)
	}
	err = tx.Commit()

	if err != nil {
		return poperrors.NewDatabaseTransactionCommitErrorMsg("%v", err)
	}

	return err
}
