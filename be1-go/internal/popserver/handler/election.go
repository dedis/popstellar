package handler

import (
	"bytes"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/types"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"sort"
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

	storeMessage := true

	switch object + "#" + action {
	case messagedata.ElectionObject + "#" + messagedata.VoteActionCastVote:
		errAnswer = handleVoteCastVote(msg, channel)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionOpen:
		errAnswer = handleElectionOpen(msg, channel)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionEnd:
		errAnswer = handleElectionEnd(msg, channel)
		storeMessage = false
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleChannelElection")
		return errAnswer
	}

	if storeMessage {
		db, errAnswer := database.GetElectionRepositoryInstance()
		if errAnswer != nil {
			return errAnswer.Wrap("handleChannelElection")
		}

		err := db.StoreMessageAndData(channel, msg)
		if err != nil {
			errAnswer = answer.NewInternalServerError("failed to store message: %v", err)
			errAnswer = errAnswer.Wrap("handleChannelElection")
			return errAnswer
		}
	}

	return nil
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

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionOpen")
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

	_, ok := attendees[msg.Sender]
	if !senderPubKey.Equal(organizerPubKey) && !ok {
		errAnswer = answer.NewInvalidMessageFieldError("sender is not an attendee or the organizer of the election")
		return errAnswer
	}

	//verify message data
	errAnswer = voteCastVote.Verify(channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}

	// verify that the election is open
	started, err := db.IsElectionStarted(channel)
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
	if voteCastVote.CreatedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("cast vote created at is negative")
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}

	// verify VoteCastVote created after election createdAt
	createdAt, err := db.GetElectionCreationTime(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election creation time: %v", err)
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
		err := verifyVote(vote, channel, voteCastVote.Election)
		if err != nil {
			errAnswer = answer.NewInvalidMessageFieldError("failed to validate vote %d: %v", i, err)
			errAnswer = errAnswer.Wrap("handleVoteCastVote")
			return errAnswer
		}
	}

	errAnswer = broadcastToAllClients(msg, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleVoteCastVote")
		return errAnswer
	}

	return nil
}

func verifyVote(vote messagedata.Vote, channel, electionID string) *answer.Error {
	var errAnswer *answer.Error

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionOpen")
	}

	questions, err := db.GetElectionQuestions(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election questions: %v", err)
		errAnswer = errAnswer.Wrap("verifyVote")
		return errAnswer
	}
	question, ok := questions[vote.Question]
	if !ok {
		errAnswer = answer.NewInvalidMessageFieldError("Question does not exist")
		errAnswer = errAnswer.Wrap("verifyVote")
		return errAnswer
	}
	electionType, err := db.GetElectionType(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election type: %v", err)
		errAnswer = errAnswer.Wrap("verifyVote")
		return errAnswer
	}
	var voteString string
	switch electionType {
	case messagedata.OpenBallot:
		voteInt, ok := vote.Vote.(int)
		if !ok {
			errAnswer = answer.NewInvalidMessageFieldError("vote in open ballot should be an integer")
			errAnswer = errAnswer.Wrap("verifyVote")
			return errAnswer
		}
		voteString = fmt.Sprintf("%d", voteInt)
	case messagedata.SecretBallot:
		voteString, ok = vote.Vote.(string)
		if !ok {
			errAnswer = answer.NewInvalidMessageFieldError("vote in secret ballot should be a string")
			errAnswer = errAnswer.Wrap("verifyVote")
			return errAnswer
		}
		voteBytes, err := base64.URLEncoding.DecodeString(voteString)
		if err != nil {
			errAnswer = answer.NewInvalidMessageFieldError("vote should be base64 encoded: %v", err)
			errAnswer = errAnswer.Wrap("verifyVote")
			return errAnswer
		}
		if len(voteBytes) != 64 {
			errAnswer = answer.NewInvalidMessageFieldError("vote should be 64 bytes long")
			errAnswer = errAnswer.Wrap("verifyVote")
			return errAnswer
		}
	}
	hash := messagedata.Hash(voteFlag, electionID, string(question.ID), voteString)
	if vote.ID != hash {
		errAnswer = answer.NewInvalidMessageFieldError("vote ID is incorrect")
		errAnswer = errAnswer.Wrap("verifyVote")
		return errAnswer
	}
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

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionOpen")
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

	// verify message data
	errAnswer = electionOpen.Verify(channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	// verify if the election was already started or terminated
	ok, err := db.IsElectionStartedOrEnded(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election start or termination status: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	if ok {
		errAnswer = answer.NewInvalidMessageFieldError("election is already started or ended")
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}

	createdAt, err := db.GetElectionCreationTime(channel)
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

	errAnswer = broadcastToAllClients(msg, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleElectionOpen")
		return errAnswer
	}
	return nil
}

func handleElectionEnd(msg message.Message, channel string) *answer.Error {
	var errAnswer *answer.Error
	var electionEnd messagedata.ElectionEnd
	err := msg.UnmarshalData(&electionEnd)
	if err != nil {
		errAnswer = answer.NewInvalidActionError("failed to unmarshal message data: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}

	// verify sender
	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode sender: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}
	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to unmarshal sender: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")
	}

	organizerPubKey, err := db.GetLAOOrganizerPubKey(channel)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("handleElectionEnd")
		return errAnswer
	}

	// check if the sender is the LAO organizer
	if !senderPubKey.Equal(organizerPubKey) {
		errAnswer = answer.NewInvalidMessageFieldError("sender is not the organizer of the channel")
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}

	// verify message data
	errAnswer = electionEnd.Verify(channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer

	}

	// verify if the election is started
	started, err := db.IsElectionStarted(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election start status: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}
	if !started {
		errAnswer = answer.NewInvalidMessageFieldError("election was not started")
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}

	// verify if the timestamp is stale
	createdAt, err := db.GetElectionCreationTime(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election creation time: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}
	if electionEnd.CreatedAt < createdAt {
		errAnswer = answer.NewInvalidMessageFieldError("election end cannot have a creation time prior to election setup")
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}

	questions, err := db.GetElectionQuestionsWithValidVotes(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election questions: %v", err)
		errAnswer = errAnswer.Wrap("verifyRegisteredVotes")
		return errAnswer
	}

	if len(electionEnd.RegisteredVotes) != 0 {
		errAnswer = verifyRegisteredVotes(electionEnd, questions)
		if errAnswer != nil {
			errAnswer = errAnswer.Wrap("handleElectionEnd")
			return errAnswer
		}
	}

	electionResultMsg, errAnswer := createElectionResult(questions, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}

	err = db.StoreMessageAndElectionResult(channel, msg, electionResultMsg)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to store message and election result: %v", err)
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}

	errAnswer = broadcastToAllClients(msg, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}
	errAnswer = broadcastToAllClients(electionResultMsg, channel)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleElectionEnd")
		return errAnswer
	}

	return nil
}

func verifyRegisteredVotes(electionEnd messagedata.ElectionEnd, questions map[string]types.Question) *answer.Error {
	var errAnswer *answer.Error

	var voteIDs []string
	for _, question := range questions {
		for _, validVote := range question.ValidVotes {
			voteIDs = append(voteIDs, validVote.ID)
			fmt.Println("valid vote: ", validVote.ID)
		}
	}
	// sort vote IDs
	sort.Strings(voteIDs)

	// hash all valid vote ids
	validVotesHash := messagedata.Hash(voteIDs...)

	// compare registered votes with local saved votes
	if electionEnd.RegisteredVotes != validVotesHash {
		errAnswer = answer.NewInvalidMessageFieldError("registered votes is %s, should be sorted and equal to %s", electionEnd.RegisteredVotes, validVotesHash)
		errAnswer = errAnswer.Wrap("verifyRegisteredVotes")
		return errAnswer
	}
	return nil
}

func createElectionResult(questions map[string]types.Question, channel string) (message.Message, *answer.Error) {
	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createElectionResult")
	}

	electionType, err := db.GetElectionType(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election type: %v", err)
		errAnswer = errAnswer.Wrap("createElectionResult")
		return message.Message{}, errAnswer
	}

	resultElection := messagedata.ElectionResult{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.ElectionActionResult,
		Questions: []messagedata.ElectionResultQuestion{},
	}

	for id, question := range questions {
		if question.Method != messagedata.PluralityMethod {
			continue
		}
		votesPerBallotOption := make([]int, len(question.BallotOptions))
		for _, validVote := range question.ValidVotes {
			index, ok := getVoteIndex(validVote, electionType, channel)
			if ok && index >= 0 && index < len(question.BallotOptions) {
				votesPerBallotOption[index]++
			}
		}
		var questionResults []messagedata.ElectionResultQuestionResult
		for i, options := range question.BallotOptions {
			questionResults = append(questionResults, messagedata.ElectionResultQuestionResult{
				BallotOption: options,
				Count:        votesPerBallotOption[i],
			})
		}

		electionResult := messagedata.ElectionResultQuestion{
			ID:     id,
			Result: questionResults,
		}
		resultElection.Questions = append(resultElection.Questions, electionResult)
	}

	buf, err := json.Marshal(resultElection)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to marshal election result: %v", err)
		errAnswer = errAnswer.Wrap("createElectionResult")
		return message.Message{}, errAnswer
	}
	buf64 := base64.URLEncoding.EncodeToString(buf)

	serverPubKey, errAnswer := config.GetServerPublicKeyInstance()
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createElectionResult")
	}
	serverPubBuf, err := serverPubKey.MarshalBinary()
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to marshal server public key: %v", err)
		errAnswer = errAnswer.Wrap("createElectionResult")
		return message.Message{}, errAnswer
	}
	signatureBuf, errAnswer := Sign(buf)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("createElectionResult")
		return message.Message{}, errAnswer
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)
	electionResultMsg := message.Message{
		Data:              buf64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(buf64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	return electionResultMsg, nil
}

func getVoteIndex(vote types.ValidVote, electionType, channel string) (int, bool) {
	switch electionType {
	case messagedata.OpenBallot:
		index, _ := vote.Index.(int)
		return index, true

	case messagedata.SecretBallot:
		encryptedVote, _ := vote.Index.(string)
		index, err := decryptVote(encryptedVote, channel)
		if err != nil {
			return index, false
		}
		return index, true
	}
	return -1, false
}

func decryptVote(vote, channel string) (int, *answer.Error) {
	var errAnswer *answer.Error
	voteBuff, err := base64.URLEncoding.DecodeString(vote)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode vote: %v", err)
		errAnswer = errAnswer.Wrap("decryptVote")
		return -1, errAnswer
	}
	if len(voteBuff) != 64 {
		errAnswer = answer.NewInvalidMessageFieldError("vote should be 64 bytes long")
		errAnswer = errAnswer.Wrap("decryptVote")
		return -1, errAnswer
	}

	// K and C are respectively the first and last 32 bytes of the vote
	K := crypto.Suite.Point()
	C := crypto.Suite.Point()

	err = K.UnmarshalBinary(voteBuff[:32])
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to unmarshal K: %v", err)
		errAnswer = errAnswer.Wrap("decryptVote")
		return -1, errAnswer
	}
	err = C.UnmarshalBinary(voteBuff[32:])
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to unmarshal C: %v", err)
		errAnswer = errAnswer.Wrap("decryptVote")
		return -1, errAnswer
	}

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return -1, errAnswer.Wrap("decryptVote")
	}
	electionSecretKey, err := db.GetElectionSecretKey(channel)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to get election secret key: %v", err)
		errAnswer = errAnswer.Wrap("decryptVote")
		return -1, errAnswer
	}

	// performs the ElGamal decryption
	S := crypto.Suite.Point().Mul(electionSecretKey, K)
	data, err := crypto.Suite.Point().Sub(C, S).Data()
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to decrypt vote: %v", err)
		errAnswer = errAnswer.Wrap("decryptVote")
		return -1, errAnswer
	}

	var index uint16

	// interprets the data as a big endian int
	buf := bytes.NewReader(data)
	err = binary.Read(buf, binary.BigEndian, &index)
	if err != nil {
		errAnswer = answer.NewInternalServerError("failed to interpret decrypted data: %v", err)
		errAnswer = errAnswer.Wrap("decryptVote")
		return -1, errAnswer
	}
	return int(index), nil
}
