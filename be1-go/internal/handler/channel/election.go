package channel

import (
	"bytes"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"popstellar/internal/crypto"
	"popstellar/internal/message/answer"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"popstellar/internal/types"
	"sort"
)

const (
	voteFlag = "Vote"
)

func handleChannelElection(channelPath string, msg message.Message) *answer.Error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return answer.NewInternalServerError(err.Error())
	}

	var errAnswer *answer.Error

	storeMessage := true

	switch object + "#" + action {
	case messagedata.ElectionObject + "#" + messagedata.VoteActionCastVote:
		errAnswer = handleVoteCastVote(msg, channelPath)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionOpen:
		errAnswer = handleElectionOpen(msg, channelPath)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionEnd:
		errAnswer = handleElectionEnd(msg, channelPath)
		storeMessage = false
	default:
		errAnswer = answer.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}
	if errAnswer != nil {
		return errAnswer.Wrap("handleChannelElection")
	}

	if storeMessage {
		db, errAnswer := database.GetElectionRepositoryInstance()
		if errAnswer != nil {
			return errAnswer.Wrap("handleChannelElection")
		}

		err := db.StoreMessageAndData(channelPath, msg)
		if err != nil {
			errAnswer = answer.NewStoreDatabaseError(err.Error())
			return errAnswer.Wrap("handleChannelElection")
		}
	}

	return nil
}

func handleVoteCastVote(msg message.Message, channelPath string) *answer.Error {
	var voteCastVote messagedata.VoteCastVote
	errAnswer := msg.UnmarshalMsgData(&voteCastVote)
	if errAnswer != nil {
		return errAnswer.Wrap("handleVoteCastVote")
	}

	errAnswer = verifySenderElection(msg, channelPath, false)
	if errAnswer != nil {
		return errAnswer.Wrap("handleVoteCastVote")
	}

	//verify message data
	errAnswer = voteCastVote.Verify(channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleVoteCastVote")
	}

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleVoteCastVote")
	}

	if voteCastVote.CreatedAt < 0 {
		errAnswer := answer.NewInvalidMessageFieldError("cast vote created at is negative")
		return errAnswer.Wrap("handleVoteCastVote")
	}

	// verify VoteCastVote created after election createdAt
	createdAt, err := db.GetElectionCreationTime(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election creation time: %v", err)
		return errAnswer.Wrap("handleVoteCastVote")
	}

	if createdAt > voteCastVote.CreatedAt {
		errAnswer := answer.NewInvalidMessageFieldError("cast vote cannot have a creation time prior to election setup")
		return errAnswer.Wrap("handleVoteCastVote")
	}

	// verify votes
	for i, vote := range voteCastVote.Votes {
		err := verifyVote(vote, channelPath, voteCastVote.Election)
		if err != nil {
			errAnswer := answer.NewInvalidMessageFieldError("failed to validate vote %d: %v", i, err)
			return errAnswer.Wrap("handleVoteCastVote")
		}
	}

	// Just store the vote cast if the election has ended because will not have any influence on the result
	ended, err := db.IsElectionEnded(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election end status: %v", err)
		return errAnswer.Wrap("handleVoteCastVote")
	}
	if ended {
		return nil
	}

	// verify that the election is open
	started, err := db.IsElectionStarted(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election start status: %v", err)
		return errAnswer.Wrap("handleVoteCastVote")
	}

	if !started {
		errAnswer := answer.NewInvalidMessageFieldError("election is not started")
		return errAnswer.Wrap("handleVoteCastVote")
	}

	errAnswer = broadcastToAllClients(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleVoteCastVote")
	}

	return nil
}

func handleElectionOpen(msg message.Message, channelPath string) *answer.Error {
	var electionOpen messagedata.ElectionOpen
	errAnswer := msg.UnmarshalMsgData(&electionOpen)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionOpen")
	}

	errAnswer = verifySenderElection(msg, channelPath, true)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionOpen")
	}

	// verify message data
	errAnswer = electionOpen.Verify(channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionOpen")
	}

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionOpen")
	}

	// verify if the election was already started or terminated
	ok, err := db.IsElectionStartedOrEnded(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election start or termination status: %v", err)
		return errAnswer.Wrap("handleElectionOpen")
	}
	if ok {
		errAnswer := answer.NewInvalidMessageFieldError("election is already started or ended")
		return errAnswer.Wrap("handleElectionOpen")
	}

	createdAt, err := db.GetElectionCreationTime(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election creation time: %v", err)
		return errAnswer.Wrap("handleElectionOpen")
	}
	if electionOpen.OpenedAt < createdAt {
		errAnswer := answer.NewInvalidMessageFieldError("election open cannot have a creation time prior to election setup")
		return errAnswer.Wrap("handleElectionOpen")
	}

	errAnswer = broadcastToAllClients(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionOpen")
	}

	return nil
}

func handleElectionEnd(msg message.Message, channelPath string) *answer.Error {
	var electionEnd messagedata.ElectionEnd
	errAnswer := msg.UnmarshalMsgData(&electionEnd)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")
	}

	errAnswer = verifySenderElection(msg, channelPath, true)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")
	}

	errAnswer = verifyElectionEnd(electionEnd, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")
	}

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")
	}

	questions, err := db.GetElectionQuestionsWithValidVotes(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election questions: %v", err)
		return errAnswer.Wrap("handleElectionEnd")
	}

	if len(electionEnd.RegisteredVotes) != 0 {
		errAnswer = verifyRegisteredVotes(electionEnd, questions)
		if errAnswer != nil {
			return errAnswer.Wrap("handleElectionEnd")
		}
	}

	electionResultMsg, errAnswer := createElectionResult(questions, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")
	}

	err = db.StoreElectionEndWithResult(channelPath, msg, electionResultMsg)
	if err != nil {
		errAnswer := answer.NewStoreDatabaseError("election end and election result: %v", err)
		return errAnswer.Wrap("handleElectionEnd")
	}

	errAnswer = broadcastToAllClients(msg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")
	}
	errAnswer = broadcastToAllClients(electionResultMsg, channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")
	}

	return nil
}

func verifyElectionEnd(electionEnd messagedata.ElectionEnd, channelPath string) *answer.Error {
	// verify message data
	errAnswer := electionEnd.Verify(channelPath)
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")

	}

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionEnd")
	}

	// verify if the election is started
	started, err := db.IsElectionStarted(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election start status: %v", err)
		return errAnswer.Wrap("handleElectionEnd")
	}
	if !started {
		errAnswer := answer.NewInvalidMessageFieldError("election was not started")
		return errAnswer.Wrap("handleElectionEnd")
	}

	// verify if the timestamp is stale
	createdAt, err := db.GetElectionCreationTime(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election creation time: %v", err)
		return errAnswer.Wrap("handleElectionEnd")
	}
	if electionEnd.CreatedAt < createdAt {
		errAnswer := answer.NewInvalidMessageFieldError("election end cannot have a creation time prior to election setup")
		return errAnswer.Wrap("handleElectionEnd")
	}

	return nil
}

func verifySenderElection(msg message.Message, channelPath string, onlyOrganizer bool) *answer.Error {
	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode sender: %v", err)
		return errAnswer.Wrap("verifySender")
	}
	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal sender: %v", err)
		return errAnswer.Wrap("verifySender")
	}

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("verifySender")
	}

	organizerPubKey, err := db.GetLAOOrganizerPubKey(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("lao organizer pk: %v", err)
		return errAnswer.Wrap("verifySender")
	}

	if onlyOrganizer && !senderPubKey.Equal(organizerPubKey) {
		errAnswer := answer.NewInvalidMessageFieldError("sender is not the organizer of the channel")
		return errAnswer.Wrap("verifySender")
	}

	if onlyOrganizer {
		return nil
	}

	attendees, err := db.GetElectionAttendees(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election attendees: %v", err)
		return errAnswer.Wrap("verifySender")
	}

	_, ok := attendees[msg.Sender]
	if !ok && !senderPubKey.Equal(organizerPubKey) {
		errAnswer := answer.NewInvalidMessageFieldError("sender is not an attendee or the organizer of the election")
		return errAnswer.Wrap("verifySender")
	}

	return nil
}

func verifyVote(vote messagedata.Vote, channelPath, electionID string) *answer.Error {
	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return errAnswer.Wrap("handleElectionOpen")
	}

	questions, err := db.GetElectionQuestions(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election questions: %v", err)
		return errAnswer.Wrap("verifyVote")
	}
	question, ok := questions[vote.Question]
	if !ok {
		errAnswer := answer.NewInvalidMessageFieldError("Question does not exist")
		return errAnswer.Wrap("verifyVote")
	}
	electionType, err := db.GetElectionType(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election type: %v", err)
		return errAnswer.Wrap("verifyVote")
	}
	var voteString string
	switch electionType {
	case messagedata.OpenBallot:
		voteInt, ok := vote.Vote.(int)
		if !ok {
			errAnswer := answer.NewInvalidMessageFieldError("vote in open ballot should be an integer")
			return errAnswer.Wrap("verifyVote")
		}
		voteString = fmt.Sprintf("%d", voteInt)
	case messagedata.SecretBallot:
		voteString, ok = vote.Vote.(string)
		if !ok {
			errAnswer := answer.NewInvalidMessageFieldError("vote in secret ballot should be a string")
			return errAnswer.Wrap("verifyVote")
		}
		voteBytes, err := base64.URLEncoding.DecodeString(voteString)
		if err != nil {
			errAnswer := answer.NewInvalidMessageFieldError("vote should be base64 encoded: %v", err)
			return errAnswer.Wrap("verifyVote")
		}
		if len(voteBytes) != 64 {
			errAnswer := answer.NewInvalidMessageFieldError("vote should be 64 bytes long")
			return errAnswer.Wrap("verifyVote")
		}
	default:
		errAnswer := answer.NewInvalidMessageFieldError("invalid election type: %s", electionType)
		return errAnswer.Wrap("verifyVote")
	}
	hash := messagedata.Hash(voteFlag, electionID, string(question.ID), voteString)
	if vote.ID != hash {
		errAnswer := answer.NewInvalidMessageFieldError("vote ID is not the expected hash")
		return errAnswer.Wrap("verifyVote")
	}
	return nil
}

func verifyRegisteredVotes(electionEnd messagedata.ElectionEnd, questions map[string]types.Question) *answer.Error {
	var voteIDs []string
	for _, question := range questions {
		for _, validVote := range question.ValidVotes {
			voteIDs = append(voteIDs, validVote.ID)
		}
	}
	// sort vote IDs
	sort.Strings(voteIDs)

	// hash all valid vote ids
	validVotesHash := messagedata.Hash(voteIDs...)

	// compare registered votes with local saved votes
	if electionEnd.RegisteredVotes != validVotesHash {
		errAnswer := answer.NewInvalidMessageFieldError("registered votes is %s, should be sorted and equal to %s",
			electionEnd.RegisteredVotes, validVotesHash)
		return errAnswer.Wrap("verifyRegisteredVotes")
	}
	return nil
}

func createElectionResult(questions map[string]types.Question, channelPath string) (message.Message, *answer.Error) {
	resultElection, errAnswer := computeElectionResult(questions, channelPath)
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createElectionResult")
	}

	buf, err := json.Marshal(resultElection)
	if err != nil {
		errAnswer := answer.NewInternalServerError("marshal election result: %v", err)
		return message.Message{}, errAnswer.Wrap("createElectionResult")
	}
	buf64 := base64.URLEncoding.EncodeToString(buf)

	serverPubKey, errAnswer := config.GetServerPublicKeyInstance()
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createElectionResult")
	}
	serverPubBuf, err := serverPubKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal server public key: %v", err)
		return message.Message{}, errAnswer.Wrap("createElectionResult")
	}
	signatureBuf, errAnswer := Sign(buf)
	if errAnswer != nil {
		return message.Message{}, errAnswer.Wrap("createElectionResult")
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

func computeElectionResult(questions map[string]types.Question, channelPath string) (
	messagedata.ElectionResult, *answer.Error) {
	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return messagedata.ElectionResult{}, errAnswer.Wrap("computeElectionResult")
	}

	electionType, err := db.GetElectionType(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election type: %v", err)
		return messagedata.ElectionResult{}, errAnswer.Wrap("computeElectionResult")
	}

	result := make([]messagedata.ElectionResultQuestion, 0)

	for id, question := range questions {
		if question.Method != messagedata.PluralityMethod {
			continue
		}
		votesPerBallotOption := make([]int, len(question.BallotOptions))
		for _, validVote := range question.ValidVotes {
			index, ok := getVoteIndex(validVote, electionType, channelPath)
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
		result = append(result, electionResult)
	}

	resultElection := messagedata.ElectionResult{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.ElectionActionResult,
		Questions: result,
	}

	return resultElection, nil
}

func getVoteIndex(vote types.ValidVote, electionType, channelPath string) (int, bool) {
	switch electionType {
	case messagedata.OpenBallot:
		index, _ := vote.Index.(int)
		return index, true

	case messagedata.SecretBallot:
		encryptedVote, _ := vote.Index.(string)
		index, err := decryptVote(encryptedVote, channelPath)
		if err != nil {
			return index, false
		}
		return index, true
	}
	return -1, false
}

func decryptVote(vote, channelPath string) (int, *answer.Error) {
	voteBuff, err := base64.URLEncoding.DecodeString(vote)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to decode vote: %v", err)
		return -1, errAnswer.Wrap("decryptVote")
	}
	if len(voteBuff) != 64 {
		errAnswer := answer.NewInvalidMessageFieldError("vote should be 64 bytes long")
		return -1, errAnswer.Wrap("decryptVote")
	}

	// K and C are respectively the first and last 32 bytes of the vote
	K := crypto.Suite.Point()
	C := crypto.Suite.Point()

	err = K.UnmarshalBinary(voteBuff[:32])
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal K: %v", err)
		return -1, errAnswer.Wrap("decryptVote")
	}
	err = C.UnmarshalBinary(voteBuff[32:])
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal C: %v", err)
		return -1, errAnswer.Wrap("decryptVote")
	}

	db, errAnswer := database.GetElectionRepositoryInstance()
	if errAnswer != nil {
		return -1, errAnswer.Wrap("decryptVote")
	}

	electionSecretKey, err := db.GetElectionSecretKey(channelPath)
	if err != nil {
		errAnswer := answer.NewQueryDatabaseError("election secret key: %v", err)
		return -1, errAnswer.Wrap("decryptVote")
	}

	// performs the ElGamal decryption
	S := crypto.Suite.Point().Mul(electionSecretKey, K)
	data, err := crypto.Suite.Point().Sub(C, S).Data()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to decrypt vote: %v", err)
		return -1, errAnswer.Wrap("decryptVote")
	}

	var index uint16

	// interprets the data as a big endian int
	buf := bytes.NewReader(data)
	err = binary.Read(buf, binary.BigEndian, &index)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to interpret decrypted data: %v", err)
		return -1, errAnswer.Wrap("decryptVote")
	}
	return int(index), nil
}
