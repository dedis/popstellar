package election

import (
	"encoding/base64"
	"golang.org/x/xerrors"
	"popstellar/message/messagedata"
	"sort"
	"strings"
)

// verifyMessageCastVote checks the election#cast_vote message data is valid.
func (c *Channel) verifyMessageCastVote(castVote messagedata.VoteCastVote) error {
	c.log.Info().Msgf("verifying election#cast_vote message of election with id %s", castVote.Election)

	// verify lao id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(castVote.Lao); err != nil {
		return xerrors.Errorf("lao id is %s, should be base64URL encoded", castVote.Lao)
	}

	// verify election id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(castVote.Election); err != nil {
		return xerrors.Errorf("election id is %s, should be base64URL encoded", castVote.Election)
	}

	// split channel to [lao id, election id]
	IDs := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	laoId := strings.Split(IDs, "/")[0]
	electionId := strings.Split(IDs, "/")[1]

	// verify if lao id is the same as the channel
	if castVote.Lao != laoId {
		return xerrors.Errorf("lao id is %s, should be %s", laoId, castVote.Lao)
	}

	// verify if election id is the same as the channel
	if castVote.Election != electionId {
		return xerrors.Errorf("election id is %s, should be %s", electionId, castVote.Election)
	}

	// verify created at is positive
	if castVote.CreatedAt < 0 {
		return xerrors.Errorf("cast vote created at is %d, should be minimum 0", castVote.CreatedAt)
	}

	// verify created at is after start of election
	if castVote.CreatedAt < c.start {
		return xerrors.Errorf("cast vote created at is %d, should be greater or equal to defined start time %d",
			castVote.CreatedAt, c.start)
	}

	// verify created at is before end of election
	if castVote.CreatedAt > c.end {
		// note that CreatedAt is provided by the client and can't be fully trusted.
		// We leave this check as is until we have a better solution.
		return xerrors.Errorf("cast vote created at is %d, should be smaller or equal to defined end time %d",
			castVote.CreatedAt, c.end)
	}

	return nil
}

// verifyMessageElectionEnd checks the election#end message data is valid.
func (c *Channel) verifyMessageElectionEnd(electionEnd messagedata.ElectionEnd) error {
	c.log.Info().Msgf("verifying election#end message of election with id %s", electionEnd.Election)

	// verify lao id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(electionEnd.LAO); err != nil {
		return xerrors.Errorf("lao id is %s, should be base64URL encoded", electionEnd.LAO)
	}

	// verify election id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(electionEnd.Election); err != nil {
		return xerrors.Errorf("election id is %s, should be base64URL encoded", electionEnd.Election)
	}

	// split channel to [lao id, election id]
	IDs := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	laoId := strings.Split(IDs, "/")[0]
	electionId := strings.Split(IDs, "/")[1]

	// verify if lao id is the same as the channel
	if electionEnd.LAO != laoId {
		return xerrors.Errorf("lao id is %s, should be %s", laoId, electionEnd.LAO)
	}

	// verify if election id is the same as the channel
	if electionEnd.Election != electionId {
		return xerrors.Errorf("election id is %s, should be %s", electionId, electionEnd.Election)
	}

	// verify created at is positive
	if electionEnd.CreatedAt < 0 {
		return xerrors.Errorf("election end created at is %d, should be minimum 0", electionEnd.CreatedAt)
	}

	// verify end time of election
	if electionEnd.CreatedAt < c.end {
		return xerrors.Errorf("election end created at is %d, should be greater or equal to defined end time %d",
			electionEnd.CreatedAt, c.end)
	}

	// verify registered votes are base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(electionEnd.RegisteredVotes); err != nil {
		return xerrors.Errorf("election registered votes is %s, should be base64URL encoded", electionEnd.RegisteredVotes)
	}

	// verify order of registered votes
	if len(electionEnd.RegisteredVotes) != 0 {
		// get hashed ID of valid votes sorted by msg ID
		validVotes := make(map[string]string)
		for _, question := range c.questions {
			question.validVotesMu.Lock()

			// sort the valid votes alphabetically by vote ID
			for _, validVote := range question.validVotes {
				// since we eliminated (in cast vote) the duplicate votes we are sure
				// that the validVotes contain one vote for one question by every voter
				validVotes[validVote.msgID] = validVote.ID
			}

			question.validVotesMu.Unlock()
		}

		// sort all valid votes by message id
		messageIDs := make([]string, 0)
		for _, messageID := range validVotes {
			messageIDs = append(messageIDs, messageID)
		}
		sort.Strings(messageIDs)

		// hash all valid vote ids
		votes := make([]string, 0)
		for _, messageID := range messageIDs {
			votes = append(votes, validVotes[messageID])
		}
		validVotesHash := messagedata.Hash(votes...)

		// compare registered votes with local saved votes
		if electionEnd.RegisteredVotes != validVotesHash {
			c.log.Error().Msgf("not same registered votes, had %s wanted %s",
				electionEnd.RegisteredVotes,
				validVotesHash)
		}
	}

	return nil
}
