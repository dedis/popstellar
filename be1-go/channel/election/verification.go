package election

import (
	"encoding/base64"
	"fmt"
	"popstellar/message/messagedata"
	"sort"
	"strings"

	"github.com/rs/zerolog/log"
	"golang.org/x/xerrors"
)

const (
	laoIDBase64   = "lao id is %s, should be base64URL encoded"
	elecIDBase64  = "election id is %s, should be base64URL encoded"
	elecIDFormat  = "election channel id is %s, should be formatted as /root/laoID/electionID"
	laoIDCompare  = "lao id is %s, should be %s"
	elecIDCompare = "election id is %s, should be %s"
)

func (c *Channel) verifyMessageElectionOpen(electionOpen messagedata.ElectionOpen) error {
	c.log.Info().Msgf("verifying election#open message of election with id %s",
		electionOpen.Election)

	// verify lao id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(electionOpen.Lao)
	if err != nil {
		return xerrors.Errorf(laoIDBase64, electionOpen.Lao)
	}

	// verify election id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(electionOpen.Election)
	if err != nil {
		return xerrors.Errorf(elecIDBase64, electionOpen.Election)
	}

	// split channel to [lao id, election id]
	noRoot := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")

	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		return xerrors.Errorf(elecIDFormat, c.channelID)
	}

	laoID := IDs[0]
	electionID := IDs[1]

	// verify if lao id is the same as the channel
	if electionOpen.Lao != laoID {
		return xerrors.Errorf(laoIDCompare, laoID, electionOpen.Lao)
	}

	// verify if election id is the same as the channel
	if electionOpen.Election != electionID {
		return xerrors.Errorf(elecIDCompare, electionID, electionOpen.Election)
	}

	// verify opened at is positive
	if electionOpen.OpenedAt < 0 {
		return xerrors.Errorf("election open created at is %d, should be minimum 0",
			electionOpen.OpenedAt)
	}

	// verify if the election was already started or terminated
	if c.started || c.terminated {
		return xerrors.Errorf("election was already started or terminated")
	}

	if electionOpen.OpenedAt < c.createdAt {
		return xerrors.Errorf("election open cannot have a creation time prior to election setup")
	}

	return nil
}

// verifyMessageCastVote checks the election#cast_vote message data is valid.
func (c *Channel) verifyMessageCastVote(castVote messagedata.VoteCastVote) error {
	c.log.Info().Msgf("verifying election#cast_vote message of election with id %s",
		castVote.Election)

	// verify lao id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(castVote.Lao)
	if err != nil {
		return xerrors.Errorf(laoIDBase64, castVote.Lao)
	}

	// verify election id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(castVote.Election)
	if err != nil {
		return xerrors.Errorf(elecIDBase64, castVote.Election)
	}

	// split channel to [lao id, election id]
	noRoot := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		return xerrors.Errorf(elecIDFormat, c.channelID)
	}
	laoID := IDs[0]
	electionID := IDs[1]

	// verify if lao id is the same as the channel
	if castVote.Lao != laoID {
		return xerrors.Errorf(laoIDCompare, laoID, castVote.Lao)
	}

	// verify if election id is the same as the channel
	if castVote.Election != electionID {
		return xerrors.Errorf(elecIDCompare, electionID, castVote.Election)
	}

	//verify if election is terminated
	if c.terminated {
		return xerrors.Errorf("cast vote created at is %d, but the election is terminated",
			castVote.CreatedAt)
	}

	// verify if election is not open
	if !c.started {
		return xerrors.Errorf("cast vote created at is %d, but the election is not started",
			castVote.CreatedAt)
	}

	// verify created at is positive
	if castVote.CreatedAt < 0 {
		return xerrors.Errorf("cast vote created at is %d, should be minimum 0", castVote.CreatedAt)
	}

	for i, vote := range castVote.Votes {
		qs, ok := c.questions[vote.Question]
		if !ok {
			return xerrors.Errorf("no Question with question ID %s exists", vote.Question)
		}

		sort.Ints(vote.Vote)
		sortedIndex := arrayToString(vote.Vote, ",")

		hash := messagedata.Hash("Vote", electionID, string(qs.ID), sortedIndex)

		if vote.ID != hash {
			return xerrors.Errorf("vote ID of vote %d is incorrect", i)
		}
	}

	return nil
}

// verifyMessageElectionEnd checks the election#end message data is valid.
func (c *Channel) verifyMessageElectionEnd(electionEnd messagedata.ElectionEnd) error {
	c.log.Info().Msgf("verifying election#end message of election with id %s",
		electionEnd.Election)

	// verify lao id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(electionEnd.Lao)
	if err != nil {
		return xerrors.Errorf(laoIDBase64, electionEnd.Lao)
	}

	// verify election id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(electionEnd.Election)
	if err != nil {
		return xerrors.Errorf(elecIDBase64, electionEnd.Election)
	}

	// split channel to [lao id, election id]
	noRoot := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		return xerrors.Errorf(elecIDFormat, c.channelID)
	}
	laoID := IDs[0]
	electionID := IDs[1]

	// verify if lao id is the same as the channel
	if electionEnd.Lao != laoID {
		return xerrors.Errorf(laoIDCompare, laoID, electionEnd.Lao)
	}

	// verify if election id is the same as the channel
	if electionEnd.Election != electionID {
		return xerrors.Errorf(elecIDCompare, electionID, electionEnd.Election)
	}

	// verify created at is positive
	if electionEnd.CreatedAt < 0 {
		return xerrors.Errorf("election end created at is %d, should be minimum 0",
			electionEnd.CreatedAt)
	}

	// verify if the election is not terminated
	if c.terminated {
		return xerrors.Errorf("election is already terminated")
	}

	// verify if election is started
	if !c.started {
		return xerrors.Errorf("election is not started")
	}

	// verify registered votes are base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(electionEnd.RegisteredVotes); err != nil {
		return xerrors.Errorf("election registered votes is %s, should be base64URL encoded",
			electionEnd.RegisteredVotes)
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
func verifyRegisteredVotes(electionEnd messagedata.ElectionEnd,
	questions *map[string]*question) error {

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

func arrayToString(a []int, delim string) string {
	return strings.Trim(strings.Replace(fmt.Sprint(a), " ", delim, -1), "[]")
}
