package sqlite

import (
	"database/sql"
	"encoding/json"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
)

func (s *SQLite) IsChallengeValid(senderPk string, challenge messagedata.FederationChallenge, channelPath string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	var federationChallengeBytes []byte
	err := s.database.QueryRow(selectValidFederationChallenges, channelPath,
		senderPk, messagedata.FederationObject,
		messagedata.FederationActionChallenge, challenge.Value,
		challenge.ValidUntil).Scan(&federationChallengeBytes)
	if err != nil {
		return poperrors.NewDatabaseSelectErrorMsg("federation challenge: %v", err)
	}

	var federationChallenge messagedata.FederationChallenge
	err = json.Unmarshal(federationChallengeBytes, &federationChallenge)
	if err != nil {
		return poperrors.NewInternalServerError("failed to unmarshal federation challenge: %v", err)
	}

	if federationChallenge != challenge {
		return poperrors.NewInvalidMessageFieldError("federation challenge doesn't match")
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
		return poperrors.NewDatabaseDeleteErrorMsg("federation challenge: %v", err)
	}

	nb, err := result.RowsAffected()
	if err != nil {
		return poperrors.NewDatabaseRowsAffectedErrorMsg("federation challenge: %v", err)
	}

	if nb != 1 {
		return poperrors.NewDatabaseRowsAffectedErrorMsg("wrong number of rows affected: %d", nb)
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
		return messagedata.FederationExpect{}, poperrors.NewDatabaseSelectErrorMsg("federation expect messages: %v", err)
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
		err = federationExpect.ChallengeMsg.UnmarshalData(&federationChallenge)
		if err != nil {
			return messagedata.FederationExpect{}, err
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
		return messagedata.FederationInit{}, poperrors.NewDatabaseSelectErrorMsg("federation expect messages: %v", err)
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
		err = federationInit.ChallengeMsg.UnmarshalData(&federationChallenge)
		if err != nil {
			return messagedata.FederationInit{}, err
		}

		if federationChallenge == challenge {
			return federationInit, nil
		}
	}

	return messagedata.FederationInit{}, sql.ErrNoRows
}
