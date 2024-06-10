package sqlite

import (
	"database/sql"
	"encoding/json"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	mfederation2 "popstellar/internal/handler/messagedata/federation/mfederation"
)

func (s *SQLite) IsChallengeValid(senderPk string, challenge mfederation2.FederationChallenge, channelPath string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	var federationChallengeBytes []byte
	err := s.database.QueryRow(selectValidFederationChallenges, channelPath,
		senderPk, mmessage.FederationObject,
		mmessage.FederationActionChallenge, challenge.Value,
		challenge.ValidUntil).Scan(&federationChallengeBytes)
	if err != nil {
		return poperrors.NewDatabaseSelectErrorMsg("federation challenge: %v", err)
	}

	var federationChallenge mfederation2.FederationChallenge
	err = json.Unmarshal(federationChallengeBytes, &federationChallenge)
	if err != nil {
		return poperrors.NewInternalServerError("failed to unmarshal federation challenge: %v", err)
	}

	if federationChallenge != challenge {
		return poperrors.NewInvalidMessageFieldError("federation challenge doesn't match")
	}

	return nil
}

func (s *SQLite) RemoveChallenge(challenge mfederation2.FederationChallenge) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	result, err := s.database.Exec(deleteFederationChallenge,
		mmessage.FederationObject,
		mmessage.FederationActionChallenge, challenge.Value,
		challenge.ValidUntil)
	if err != nil {
		return poperrors.NewDatabaseDeleteErrorMsg(err.Error())
	}

	nb, err := result.RowsAffected()
	if err != nil {
		return poperrors.NewDatabaseRowsAffectedErrorMsg(err.Error())
	}

	if nb != 1 {
		return poperrors.NewDatabaseRowsAffectedErrorMsg("wrong number of rows affected: %d", nb)
	}

	return nil
}

func (s *SQLite) GetFederationExpect(senderPk string, remotePk string, challenge mfederation2.FederationChallenge, channelPath string) (mfederation2.FederationExpect, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectFederationExpects, channelPath,
		senderPk, mmessage.FederationObject,
		mmessage.FederationActionExpect, remotePk)
	if err != nil {
		return mfederation2.FederationExpect{}, poperrors.NewDatabaseSelectErrorMsg("federation expect messages: %v", err)
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

		var federationExpect mfederation2.FederationExpect
		err = json.Unmarshal(federationExpectBytes, &federationExpect)
		if err != nil {
			continue
		}

		var federationChallenge mfederation2.FederationChallenge
		err = federationExpect.ChallengeMsg.UnmarshalData(&federationChallenge)
		if err != nil {
			return mfederation2.FederationExpect{}, err
		}

		if federationChallenge == challenge {
			return federationExpect, nil
		}
	}

	return mfederation2.FederationExpect{}, sql.ErrNoRows
}

func (s *SQLite) GetFederationInit(senderPk string, remotePk string, challenge mfederation2.FederationChallenge, channelPath string) (mfederation2.FederationInit, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectFederationExpects, channelPath,
		senderPk, mmessage.FederationObject,
		mmessage.FederationActionInit, remotePk)
	if err != nil {
		return mfederation2.FederationInit{}, poperrors.NewDatabaseSelectErrorMsg("federation expect messages: %v", err)
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

		var federationInit mfederation2.FederationInit
		err = json.Unmarshal(federationInitBytes, &federationInit)
		if err != nil {
			continue
		}

		var federationChallenge mfederation2.FederationChallenge
		err = federationInit.ChallengeMsg.UnmarshalData(&federationChallenge)
		if err != nil {
			return mfederation2.FederationInit{}, err
		}

		if federationChallenge == challenge {
			return federationInit, nil
		}
	}

	return mfederation2.FederationInit{}, sql.ErrNoRows
}
