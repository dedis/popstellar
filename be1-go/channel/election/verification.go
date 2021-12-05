package election

import (
	"encoding/base64"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"sort"
	"strings"
)

// verifyMessageCastVote checks the election#cast_vote message data is valid.
func (c *Channel) verifyMessageCastVote(castVote messagedata.VoteCastVote, sender string) error {
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

	senderBuf, err := base64.URLEncoding.DecodeString(sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewError(-4, "invalid sender public key")
	}

	// verify sender is an attendee or the organizer
	ok := c.attendees.IsPresent(sender) || c.hub.GetPubKeyOrg().Equal(senderPoint)
	if !ok {
		return answer.NewError(-4, "only attendees can cast a vote in an election")
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
		// get hashed valid votes sorted by vote ID
		validVotes := make([]string, 0)
		for _, question := range c.questions {
			question.validVotesMu.Lock()

			// sort the valid votes alphabetically by vote ID
			for _, validVote := range question.validVotes {
				// since we eliminated (in cast vote) the duplicate votes we are sure
				// that the validVotes contain one vote for one question by every voter
				validVotes = append(validVotes, validVote.ID)
			}

			question.validVotesMu.Unlock()
		}

		// sort all valid votes by vote id and hash
		sort.Strings(validVotes)
		validVotesHash := messagedata.Hash(validVotes...)

		// compare registered votes with local saved votes
		if electionEnd.RegisteredVotes != validVotesHash {
			c.log.Error().Msgf("not same registered votes, had %s wanted %s", electionEnd.RegisteredVotes, validVotesHash)
			//return xerrors.Errorf("received registered votes is not correct, should be %s", validVotesHash)
		}
	}

	return nil
}
