package election

import (
	"encoding/base64"
	"github.com/rs/zerolog/log"
	"golang.org/x/xerrors"
	"popstellar/message/messagedata"
	"sort"
	"strings"
)

// verifyMessageCastVote checks the election#cast_vote message data is valid.
func (c *Channel) verifyMessageCastVote(castVote messagedata.VoteCastVote) error {
	c.log.Info().Msgf("verifying election#cast_vote message of election with id %s", castVote.Election)

	// verify lao id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(castVote.Lao)
	if err != nil {
		return xerrors.Errorf("lao id is %s, should be base64URL encoded", castVote.Lao)
	}

	// verify election id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(castVote.Election)
	if err != nil {
		return xerrors.Errorf("election id is %s, should be base64URL encoded", castVote.Election)
	}

	// split channel to [lao id, election id]
	noRoot := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		return xerrors.Errorf("election channel id is %s, should be formatted as /root/laoID/electionID", c.channelID)
	}
	laoId := IDs[0]
	electionId := IDs[1]

	// verify if lao id is the same as the channel
	if castVote.Lao != laoId {
		return xerrors.Errorf("lao id is %s, should be %s", laoId, castVote.Lao)
	}

	// verify if election id is the same as the channel
	if castVote.Election != electionId {
		return xerrors.Errorf("election id is %s, should be %s", electionId, castVote.Election)
	}

	// verify if election is not open
	if !c.started {
		return xerrors.Errorf("cast vote created at is %d, but the election is not started", castVote.CreatedAt)
	}

	//verify if election is terminated
	if c.terminated {
		return xerrors.Errorf("cast vote created at is %d, but the election is terminated", castVote.CreatedAt)
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

// verifyMessageElectionOpen checks the election#open message data is valid.
func (c *Channel) verifyMessageElectionOpen(electionOpen messagedata.ElectionOpen) error {
	c.log.Info().Msgf("verifying election#open message of election with id %s", electionOpen.Election)

	// verify lao id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(electionOpen.LAO)
	if err != nil {
		return xerrors.Errorf("lao id is %s, should be base64URL encoded", electionOpen.LAO)
	}

	// verify election id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(electionOpen.Election)
	if err != nil {
		return xerrors.Errorf("election id is %s, should be base64URL encoded", electionOpen.Election)
	}

	// split channel to [lao id, election id]
	noRoot := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		return xerrors.Errorf("election channel id is %s, should be formatted as /root/laoID/electionID", c.channelID)
	}
	laoId := IDs[0]
	electionId := IDs[1]

	// verify if lao id is the same as the channel
	if electionOpen.LAO != laoId {
		return xerrors.Errorf("lao id is %s, should be %s", laoId, electionOpen.LAO)
	}

	// verify if election id is the same as the channel
	if electionOpen.Election != electionId {
		return xerrors.Errorf("election id is %s, should be %s", electionId, electionOpen.Election)
	}

	// verify created at is positive
	if electionOpen.OpenedAt < 0 {
		return xerrors.Errorf("election openedAt is %d, should be minimum 0", electionOpen.OpenedAt)
	}

	// verify open time of election
	if electionOpen.OpenedAt < c.start {
		return xerrors.Errorf("election openedAt is %d, should be greater or equal to defined start time %d",
			electionOpen.OpenedAt, c.start)
	}

	// verify if the election was already started
	if c.started || c.terminated {
		return xerrors.Errorf("election was already started")
	}

	return nil
}

// verifyMessageElectionEnd checks the election#end message data is valid.
func (c *Channel) verifyMessageElectionEnd(electionEnd messagedata.ElectionEnd) error {
	c.log.Info().Msgf("verifying election#end message of election with id %s", electionEnd.Election)

	// verify lao id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(electionEnd.LAO)
	if err != nil {
		return xerrors.Errorf("lao id is %s, should be base64URL encoded", electionEnd.LAO)
	}

	// verify election id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(electionEnd.Election)
	if err != nil {
		return xerrors.Errorf("election id is %s, should be base64URL encoded", electionEnd.Election)
	}

	// split channel to [lao id, election id]
	noRoot := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		return xerrors.Errorf("election channel id is %s, should be formatted as /root/laoID/electionID", c.channelID)
	}
	laoId := IDs[0]
	electionId := IDs[1]

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

	// verify if the election is started
	if !c.started {
		return xerrors.Errorf("election is not started")
	}
	// verify if the election is not terminated
	if c.terminated {
		return xerrors.Errorf("election is already terminated")
	}

	// verify registered votes are base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(electionEnd.RegisteredVotes); err != nil {
		return xerrors.Errorf("election registered votes is %s, should be base64URL encoded", electionEnd.RegisteredVotes)
	}

	// verify order of registered votes
	if len(electionEnd.RegisteredVotes) != 0 {
		err := verifyRegisteredVotes(electionEnd, &c.questions)
		if err != nil {
			c.log.Err(err).Msgf("problem with registered votes: %v", err)
			return nil
		}
	}

	return nil
}

// verifyRegisteredVotes checks the registered votes of an election end message are valid.
func verifyRegisteredVotes(electionEnd messagedata.ElectionEnd, questions *map[string]*question) error {
	// get hashed ID of valid votes sorted by msg ID
	validVotes := make(map[string]string)
	validVotesSorted := make([]string, 0)

	for _, question := range *questions {
		msgIDs := make([]string, 0)

		question.validVotesMu.Lock()
		for _, validVote := range question.validVotes {
			// since we eliminated (in cast vote) the duplicate votes we are sure
			// that the validVotes contain one vote for one question by every voter
			validVotes[validVote.msgID] = validVote.ID
			msgIDs = append(msgIDs, validVote.msgID)
		}
		question.validVotesMu.Unlock()

		// sort the valid votes alphabetically by msg ID
		sort.Strings(msgIDs)
		for _, msgID := range msgIDs {
			log.Info().Msgf("valid votes is %s for msg id %s", validVotes[msgID], msgID)
			validVotesSorted = append(validVotesSorted, validVotes[msgID])
		}

	}

	// hash all valid vote ids
	validVotesHash := messagedata.Hash(validVotesSorted...)

	// compare registered votes with local saved votes
	if electionEnd.RegisteredVotes != validVotesHash {
		return xerrors.Errorf("registered votes is %s, should be sorted and equal to %s",
			electionEnd.RegisteredVotes,
			validVotesHash)
	}

	return nil
}
