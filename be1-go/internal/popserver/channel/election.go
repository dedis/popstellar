package channel

import (
	"encoding/base64"
	"popstellar/crypto"
	"popstellar/internal/popserver/database"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strings"
)

const (
	voteFlag = "Vote"
)

func handleChannelElection(channel string, msg message.Message) *answer.Error {
	object, action, errAnswer := verifyDataAndGetObjectAction(msg)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelElection")
		return errAnswer
	}

	switch object + "#" + action {
	case messagedata.ElectionObject + "#" + messagedata.VoteActionCastVote:
		errAnswer = handleVoteCastVote(msg, channel)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionOpen:
		errAnswer = handleElectionOpen(msg, channel)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionEnd:
		errAnswer = handleElectionEnd(msg)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionResult:
		errAnswer = handleElectionResult(msg)
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelElection")
		return errAnswer
	}

	db, ok := database.GetElectionRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleChannelElection")
		return errAnswer
	}

	err := db.StoreMessage(channel, msg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
		errAnswer = errAnswer.Wrap("handleChannelElection")
		return errAnswer
	}

	errAnswer = broadcastToAllClients(msg, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelElection")
		return errAnswer
	}
	return nil
}

type Question struct {
	// ID represents the ID of the Question.
	ID []byte

	// ballotOptions represents different ballot options.
	ballotOptions []string

	// validVotes represents the list of all valid votes. The key represents
	// the public key of the person casting the vote.
	validVotes map[string]validVote

	// method represents the voting method of the election. Either "Plurality"
	// or "Approval".
	method string
}

type validVote struct {
	// msgID represents the ID of the message containing the cast vote
	msgID string

	// ID represents the ID of the valid cast vote
	ID string

	// voteTime represents the time of the creation of the vote
	voteTime int64

	// index represents the index of the ballot options
	index interface{}
}

func handleVoteCastVote(msg message.Message, channel string) *answer.Error {
	var voteCastVote messagedata.VoteCastVote
	err := msg.UnmarshalData(&voteCastVote)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}

	// verify sender

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode sender: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to unmarshal sender: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}

	db, ok := database.GetElectionRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleElectionOpen")
		return errAnswer
	}

	attendees, err := db.GetElectionAttendees(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election attendees: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}

	organizerPubKey, err := db.GetLAOOrganizerPubKey(channel)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("handleElectionOpen")
		return errAnswer
	}

	_, ok = attendees[msg.Sender]
	if !senderPubKey.Equal(organizerPubKey) || !ok {
		errAnswer = answer.NewInvalidMessageFieldError("sender is not an attendee or the organizer of the election")
	}

	//verify message data

	// verify lao id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(voteCastVote.Lao)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode lao: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	// verify election id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(voteCastVote.Election)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode election: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	// split channel to [lao id, election id]
	noRoot := strings.ReplaceAll(channel, messagedata.RootPrefix, "")
	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		errAnswer = answer.NewInvalidMessageFieldError("failed to split channel: %v", channel)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	laoID := IDs[0]
	electionID := IDs[1]
	// verify if lao id is the same as the channel
	if voteCastVote.Lao != laoID {
		errAnswer = answer.NewInvalidMessageFieldError("lao id is not the same as the channel")
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	// verify if election id is the same as the channel
	if voteCastVote.Election != electionID {
		errAnswer = answer.NewInvalidMessageFieldError("election id is not the same as the channel")
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	// verify if election is terminated
	terminated, err := db.IsElectionTerminated(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election termination status: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	if terminated {
		errAnswer = answer.NewInvalidMessageFieldError("election was already terminated")
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	// verify if election is not open
	started, err := db.IsElectionTerminated(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election start status: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	if !started {
		errAnswer = answer.NewInvalidMessageFieldError("election was already terminated")
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	// verify created at is positive
	createdAt, err := db.GetElectionCreationTime(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election creation time: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	if createdAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("cast vote created at is negative")
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	if createdAt > voteCastVote.CreatedAt {
		errAnswer = answer.NewInvalidMessageFieldError("cast vote cannot have a creation time prior to election setup")
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}
	// verify votes
	for i, vote := range voteCastVote.Votes {
		err := verifyVote(vote, channel, electionID)
		if err != nil {
			errAnswer = answer.NewInvalidMessageFieldError("failed to validate vote %d: %v", i, err)
			errAnswer = errAnswer.Wrap("handleVoteCastVote")
			return errAnswer
		}
	}
	// store message and update votes
	err = db.StoreCastVote(channel, msg, voteCastVote)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store cast vote: %v", err)
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}

	return nil
}

func verifyVote(vote messagedata.Vote, channel, electionID string) *answer.Error {
	//var errAnswer *answer.Error
	////questions, err := params.DB.GetElectionQuestions(channel)
	////if err != nil {
	////	errAnswer = answer.NewInternalServerError("failed to get election questions: %v", err)
	////	errAnswer = errAnswer.Wrap("verifyVote")
	////	return errAnswer
	////}
	//question, ok := questions[vote.Question]
	//if !ok {
	//	errAnswer = answer.NewInvalidMessageFieldError("Question does not exist")
	//	errAnswer = errAnswer.Wrap("verifyVote")
	//	return errAnswer
	//}
	//electionType, err := params.DB.GetElectionType(channel)
	//if err != nil {
	//	errAnswer = answer.NewInternalServerError("failed to get election type: %v", err)
	//	errAnswer = errAnswer.Wrap("verifyVote")
	//	return errAnswer
	//}
	//var voteString string
	//switch electionType {
	//case messagedata.OpenBallot:
	//	voteInt, ok := vote.Vote.(int)
	//	if !ok {
	//		errAnswer = answer.NewInvalidMessageFieldError("vote in open ballot should be an integer")
	//		errAnswer = errAnswer.Wrap("verifyVote")
	//		return errAnswer
	//	}
	//	voteString = fmt.Sprintf("%d", voteInt)
	//case messagedata.SecretBallot:
	//	voteString, ok = vote.Vote.(string)
	//	if !ok {
	//		errAnswer = answer.NewInvalidMessageFieldError("vote in secret ballot should be a string")
	//		errAnswer = errAnswer.Wrap("verifyVote")
	//		return errAnswer
	//	}
	//	voteBytes, err := base64.URLEncoding.DecodeString(voteString)
	//	if err != nil {
	//		errAnswer = answer.NewInvalidMessageFieldError("vote should be base64 encoded: %v", err)
	//		errAnswer = errAnswer.Wrap("verifyVote")
	//		return errAnswer
	//	}
	//	if len(voteBytes) != 64 {
	//		errAnswer = answer.NewInvalidMessageFieldError("vote should be 64 bytes long")
	//		errAnswer = errAnswer.Wrap("verifyVote")
	//		return errAnswer
	//	}
	//}
	//hash := messagedata.Hash(voteFlag, electionID, string(question.ID), voteString)
	//if vote.ID != hash {
	//	errAnswer = answer.NewInvalidMessageFieldError("vote ID is incorrect")
	//	errAnswer = errAnswer.Wrap("verifyVote")
	//	return errAnswer
	//}
	return nil
}

func handleElectionOpen(msg message.Message, channel string) *answer.Error {
	var electionOpen messagedata.ElectionOpen
	err := msg.UnmarshalData(&electionOpen)
	var errAnswer *answer.Error

	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode sender: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}
	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to unmarshal sender: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	db, ok := database.GetElectionRepositoryInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get database").Wrap("handleElectionOpen")
		return errAnswer
	}

	organizerPubKey, err := db.GetLAOOrganizerPubKey(channel)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("handleElectionOpen")
		return errAnswer
	}

	// check if the sender is the LAO organizer
	if !senderPubKey.Equal(organizerPubKey) {
		errAnswer = answer.NewInvalidMessageFieldError("sender is not the organizer of the channel")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(electionOpen.Lao)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode lao: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(electionOpen.Election)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode election: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}
	noRoot := strings.ReplaceAll(channel, messagedata.RootPrefix, "")

	IDs := strings.Split(noRoot, "/")
	if len(IDs) != 2 {
		errAnswer = answer.NewInvalidMessageFieldError("failed to split channel: %v", channel)
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}
	laoID := IDs[0]
	electionID := IDs[1]

	// verify if lao id is the same as the channel
	if electionOpen.Lao != laoID {
		errAnswer = answer.NewInvalidMessageFieldError("lao id is not the same as the channel")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	// verify if election id is the same as the channel
	if electionOpen.Election != electionID {
		errAnswer = answer.NewInvalidMessageFieldError("election id is not the same as the channel")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	// verify opened at is positive
	if electionOpen.OpenedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("opened at is negative")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	// verify if the election was already started or terminated
	ok, err = db.IsElectionStartedOrTerminated(electionID)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election start or termination status: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}
	if ok {
		errAnswer = answer.NewInvalidMessageFieldError("election was already started or terminated")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	createdAt, err := db.GetElectionCreationTime(electionID)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election creation time: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}
	if electionOpen.OpenedAt < createdAt {
		errAnswer = answer.NewInvalidMessageFieldError("election open cannot have a creation time prior to election setup")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}
	return nil
}

func handleElectionEnd(msg message.Message) *answer.Error {
	return nil
}

func handleElectionResult(msg message.Message) *answer.Error {
	return nil
}
