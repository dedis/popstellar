package hub

import (
	"encoding/base64"
	"fmt"
	"golang.org/x/xerrors"
	"log"
	"student20_pop/message"
	"sync"
)

type electionChannel struct {
	*baseChannel

	// Starting time of the election
	start message.Timestamp

	// Ending time of the election
	end message.Timestamp

	// True if the election is over and false otherwise
	terminated bool

	// Questions asked to the participants
	//the key will be the string representation of the id of type byte[]
	questions map[string]question
}

type question struct {
	// ID of th question
	id []byte

	// Different options
	ballotOptions []message.BallotOption

	//valid vote mutex
	validVotesMu sync.RWMutex

	// list of all valid votes
	// the key represents the public key of the person casting the vote
	validVotes map[string]validVote

	// Voting method of the election
	method message.VotingMethod
}

type validVote struct {
	// time of the creation of the vote
	voteTime message.Timestamp

	// indexes of the ballot options
	indexes []int
}

func (c *electionChannel) Publish(publish message.Publish) error {
	err := c.baseChannel.VerifyPublishMessage(publish)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on an election channel: %v", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	object := data.GetObject()

	if object == message.ElectionObject {

		action := message.ElectionAction(data.GetAction())
		switch action {
		case message.CastVoteAction:
			return c.castVoteHelper(publish)
		case message.ElectionEndAction:
			log.Fatal("Not implemented", message.ElectionEndAction)
		case message.ElectionResultAction:
			log.Fatal("Not implemented", message.ElectionResultAction)
		}
	}

	return nil
}

func (c *electionChannel) castVoteHelper(publish message.Publish) error {
	msg := publish.Params.Message

	voteData, ok := msg.Data.(*message.CastVoteData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: "failed to cast data to CastVoteData",
		}
	}

	if voteData.CreatedAt > c.end {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("Vote cast too late, vote casted at %v and election ended at %v", voteData.CreatedAt, c.end),
		}
	}

	//This should update any previously set vote if the message ids are the same
	messageID := base64.URLEncoding.EncodeToString(msg.MessageID)
	c.inbox[messageID] = *msg
	for _, q := range voteData.Votes {

		QuestionID := base64.URLEncoding.EncodeToString(q.QuestionID)
		qs, ok := c.questions[QuestionID]
		if ok {
			//this is to handle the case when the organizer must handle multiple votes being cast at the same time
			//qs.validVotesMu.Lock()
			earlierVote, ok := qs.validVotes[msg.Sender.String()]
			// if the sender didn't previously cast a vote or if the vote is no longer valid update it
			err := xerrors.Errorf("dummyError")
			if !ok {
				qs.validVotes[msg.Sender.String()] =
					validVote{voteData.CreatedAt,
						q.VoteIndexes}
				err =checkMethodProperties(qs.method,len(q.VoteIndexes))
					//qs.validVotesMu.Unlock()
			} else {
				changeVote(&qs,earlierVote,msg.Sender.String(),voteData.CreatedAt,q.VoteIndexes)
			}
			if err != nil{
				return err
			}
			//other votes can now change the list of valid votes
			//qs.validVotesMu.Unlock()
		} else {
			return &message.Error{
				Code:        -4,
				Description: "No Question with this ID exists",
			}
		}
	}
	return nil
}
func checkMethodProperties(method message.VotingMethod, length int) error{

	if method == "Plurality" && length < 1 {
		return &message.Error{
			Code:        -4,
			Description: "No ballot option was chosen for plurality voting method",
		}
	}
	if method == "Approval" && length != 1 {
		return &message.Error{
			Code:        -4,
			Description: "Cannot choose multiple ballot options on Approval voting method",
		}
	}
	return nil
}

func changeVote(qs *question, earlierVote validVote,sender string,created message.Timestamp,indexes [] int){
	if earlierVote.voteTime > created {
		qs.validVotes[sender] =
			validVote{created,
				indexes}
	}
}
