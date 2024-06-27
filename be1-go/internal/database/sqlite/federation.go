package sqlite

import (
	"database/sql"
	"encoding/json"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/federation/mfederation"
	"slices"
)

func (s *SQLite) IsChallengeValid(senderPk string, challenge mfederation.FederationChallenge, channelPath string) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	var federationChallengeBytes []byte
	err := s.database.QueryRow(selectValidFederationChallenges, channelPath,
		senderPk, channel.FederationObject,
		channel.FederationActionChallenge, challenge.Value,
		challenge.ValidUntil).Scan(&federationChallengeBytes)
	if err != nil {
		return poperrors.NewDatabaseSelectErrorMsg("federation challenge: %v", err)
	}

	var federationChallenge mfederation.FederationChallenge
	err = json.Unmarshal(federationChallengeBytes, &federationChallenge)
	if err != nil {
		return poperrors.NewInternalServerError("failed to unmarshal federation challenge: %v", err)
	}

	if federationChallenge != challenge {
		return poperrors.NewInvalidMessageFieldError("federation challenge doesn't match")
	}

	return nil
}

func (s *SQLite) RemoveChallenge(challenge mfederation.FederationChallenge) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	result, err := s.database.Exec(deleteFederationChallenge,
		channel.FederationObject,
		channel.FederationActionChallenge, challenge.Value,
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

func (s *SQLite) GetFederationExpect(senderPk string, remotePk string, challenge mfederation.FederationChallenge, channelPath string) (mfederation.FederationExpect, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectFederationExpects, channelPath,
		senderPk, channel.FederationObject,
		channel.FederationActionExpect, remotePk)
	if err != nil {
		return mfederation.FederationExpect{}, poperrors.NewDatabaseSelectErrorMsg("federation expect messages: %v", err)
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

		var federationExpect mfederation.FederationExpect
		err = json.Unmarshal(federationExpectBytes, &federationExpect)
		if err != nil {
			continue
		}

		var federationChallenge mfederation.FederationChallenge
		err = federationExpect.ChallengeMsg.UnmarshalData(&federationChallenge)
		if err != nil {
			return mfederation.FederationExpect{}, err
		}

		if federationChallenge == challenge {
			return federationExpect, nil
		}
	}

	return mfederation.FederationExpect{}, sql.ErrNoRows
}

func (s *SQLite) GetFederationInit(senderPk string, remotePk string, challenge mfederation.FederationChallenge, channelPath string) (mfederation.FederationInit, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectFederationExpects, channelPath,
		senderPk, channel.FederationObject,
		channel.FederationActionInit, remotePk)
	if err != nil {
		return mfederation.FederationInit{}, poperrors.NewDatabaseSelectErrorMsg("federation expect messages: %v", err)
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

		var federationInit mfederation.FederationInit
		err = json.Unmarshal(federationInitBytes, &federationInit)
		if err != nil {
			continue
		}

		var federationChallenge mfederation.FederationChallenge
		err = federationInit.ChallengeMsg.UnmarshalData(&federationChallenge)
		if err != nil {
			return mfederation.FederationInit{}, err
		}

		if federationChallenge == challenge {
			return federationInit, nil
		}
	}

	return mfederation.FederationInit{}, sql.ErrNoRows
}

func (s *SQLite) IsAttendeeFederation(channelPath, poptoken string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectLastTokensExchanges, channelPath,
		channel.FederationObject, channel.FederationActionTokensExchange)
	if err != nil {
		return false, poperrors.NewDatabaseSelectErrorMsg("tokens exchange: %v", err)
	}

	defer rows.Close()
	for rows.Next() {
		var tokensExchangeBytes []byte
		var tokensExchange mfederation.FederationTokensExchange

		err = rows.Scan(&tokensExchangeBytes)
		if err != nil {
			continue
		}

		err = json.Unmarshal(tokensExchangeBytes, &tokensExchange)
		if err != nil {
			continue
		}

		if slices.Contains(tokensExchange.Tokens, poptoken) {
			return true, nil
		}
	}

	return false, nil
}
