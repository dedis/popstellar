package channel

import (
	"bytes"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"sort"

	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"popstellar/internal/types"
)

const (
	voteFlag = "Vote"
)

func handleChannelElection(channelPath string, msg message.Message) error {
	object, action, err := verifyDataAndGetObjectAction(msg)
	if err != nil {
		return err
	}

	storeMessage := true

	switch object + "#" + action {
	case messagedata.ElectionObject + "#" + messagedata.VoteActionCastVote:
		err = handleVoteCastVote(msg, channelPath)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionOpen:
		err = handleElectionOpen(msg, channelPath)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionEnd:
		err = handleElectionEnd(msg, channelPath)
		storeMessage = false
	default:
		err = errors.NewInvalidMessageFieldError("failed to handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	if storeMessage {
		db, err := database.GetElectionRepositoryInstance()
		if err != nil {
			return err
		}

		err = db.StoreMessageAndData(channelPath, msg)
		if err != nil {
			return err
		}
	}

	return nil
}

func handleVoteCastVote(msg message.Message, channelPath string) error {
	var voteCastVote messagedata.VoteCastVote
	err := msg.UnmarshalData(&voteCastVote)
	if err != nil {
		return err
	}

	err = verifySenderElection(msg, channelPath, false)
	if err != nil {
		return err
	}

	//verify message data
	err = voteCastVote.Verify(channelPath)
	if err != nil {
		return err
	}

	db, err := database.GetElectionRepositoryInstance()
	if err != nil {
		return err
	}

	if voteCastVote.CreatedAt < 0 {
		return errors.NewInvalidMessageFieldError("cast vote created at is negative")
	}

	// verify VoteCastVote created after election createdAt
	createdAt, err := db.GetElectionCreationTime(channelPath)
	if err != nil {
		return err
	}

	if createdAt > voteCastVote.CreatedAt {
		return errors.NewInvalidMessageFieldError("cast vote cannot have a creation time prior to election setup")
	}

	// verify votes
	for _, vote := range voteCastVote.Votes {
		err = verifyVote(vote, channelPath, voteCastVote.Election)
		if err != nil {
			return err
		}
	}

	// Just store the vote cast if the election has ended because will not have any influence on the result
	ended, err := db.IsElectionEnded(channelPath)
	if err != nil {
		return err
	}
	if ended {
		return nil
	}

	// verify that the election is open
	started, err := db.IsElectionStarted(channelPath)
	if err != nil {
		return err
	}
	if !started {
		return errors.NewInvalidMessageFieldError("election is not started")
	}

	return broadcastToAllClients(msg, channelPath)
}

func handleElectionOpen(msg message.Message, channelPath string) error {
	var electionOpen messagedata.ElectionOpen
	err := msg.UnmarshalData(&electionOpen)
	if err != nil {
		return err
	}

	err = verifySenderElection(msg, channelPath, true)
	if err != nil {
		return err
	}

	// verify message data
	err = electionOpen.Verify(channelPath)
	if err != nil {
		return err
	}

	db, err := database.GetElectionRepositoryInstance()
	if err != nil {
		return err
	}

	// verify if the election was already started or terminated
	ok, err := db.IsElectionStartedOrEnded(channelPath)
	if err != nil {
		return err
	}
	if ok {
		return errors.NewInvalidMessageFieldError("election is already started or ended")
	}

	createdAt, err := db.GetElectionCreationTime(channelPath)
	if err != nil {
		return err
	}
	if electionOpen.OpenedAt < createdAt {
		return errors.NewInvalidMessageFieldError("election open cannot have a creation time prior to election setup")
	}

	return broadcastToAllClients(msg, channelPath)
}

func handleElectionEnd(msg message.Message, channelPath string) error {
	var electionEnd messagedata.ElectionEnd
	err := msg.UnmarshalData(&electionEnd)
	if err != nil {
		return err
	}

	err = verifySenderElection(msg, channelPath, true)
	if err != nil {
		return err
	}

	err = verifyElectionEnd(electionEnd, channelPath)
	if err != nil {
		return err
	}

	db, err := database.GetElectionRepositoryInstance()
	if err != nil {
		return err
	}

	questions, err := db.GetElectionQuestionsWithValidVotes(channelPath)
	if err != nil {
		return err
	}

	if len(electionEnd.RegisteredVotes) != 0 {
		err = verifyRegisteredVotes(electionEnd, questions)
		if err != nil {
			return err
		}
	}

	electionResultMsg, err := createElectionResult(questions, channelPath)
	if err != nil {
		return err
	}

	err = db.StoreElectionEndWithResult(channelPath, msg, electionResultMsg)
	if err != nil {
		return err
	}

	err = broadcastToAllClients(msg, channelPath)
	if err != nil {
		return err
	}

	return broadcastToAllClients(electionResultMsg, channelPath)
}

func verifyElectionEnd(electionEnd messagedata.ElectionEnd, channelPath string) error {
	// verify message data
	err := electionEnd.Verify(channelPath)
	if err != nil {
		return err

	}

	db, err := database.GetElectionRepositoryInstance()
	if err != nil {
		return err
	}

	// verify if the election is started
	started, err := db.IsElectionStarted(channelPath)
	if err != nil {
		return err
	}
	if !started {
		return errors.NewInvalidMessageFieldError("election was not started")
	}

	// verify if the timestamp is stale
	createdAt, err := db.GetElectionCreationTime(channelPath)
	if err != nil {
		return err
	}

	if electionEnd.CreatedAt < createdAt {
		return errors.NewInvalidMessageFieldError("election end cannot have a creation time prior to election setup")
	}

	return nil
}

func verifySenderElection(msg message.Message, channelPath string, onlyOrganizer bool) error {
	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode sender: %v", err)
	}

	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to unmarshal sender: %v", err)
	}

	db, err := database.GetElectionRepositoryInstance()
	if err != nil {
		return err
	}

	organizerPubKey, err := db.GetLAOOrganizerPubKey(channelPath)
	if err != nil {
		return err
	}

	if onlyOrganizer && !senderPubKey.Equal(organizerPubKey) {
		return errors.NewAccessDeniedError("sender is not the organizer of the channel")
	}

	if onlyOrganizer {
		return nil
	}

	attendees, err := db.GetElectionAttendees(channelPath)
	if err != nil {
		return err
	}

	_, ok := attendees[msg.Sender]
	if !ok && !senderPubKey.Equal(organizerPubKey) {
		return errors.NewAccessDeniedError("sender is not an attendee or the organizer of the election")
	}

	return nil
}

func verifyVote(vote messagedata.Vote, channelPath, electionID string) error {
	db, err := database.GetElectionRepositoryInstance()
	if err != nil {
		return err
	}

	questions, err := db.GetElectionQuestions(channelPath)
	if err != nil {
		return err
	}

	question, ok := questions[vote.Question]
	if !ok {
		return errors.NewInvalidMessageFieldError("Question does not exist")
	}

	electionType, err := db.GetElectionType(channelPath)
	if err != nil {
		return err
	}

	var voteString string
	switch electionType {
	case messagedata.OpenBallot:
		voteInt, ok := vote.Vote.(int)
		if !ok {
			return errors.NewInvalidMessageFieldError("vote in open ballot should be an integer")
		}
		voteString = fmt.Sprintf("%d", voteInt)

	case messagedata.SecretBallot:
		voteString, ok = vote.Vote.(string)
		if !ok {
			return errors.NewInvalidMessageFieldError("vote in secret ballot should be a string")
		}

		voteBytes, err := base64.URLEncoding.DecodeString(voteString)
		if err != nil {
			return errors.NewInvalidMessageFieldError("vote should be base64 encoded: %v", err)
		}
		if len(voteBytes) != 64 {
			return errors.NewInvalidMessageFieldError("vote should be 64 bytes long")
		}

	default:
		return errors.NewInvalidMessageFieldError("invalid election type: %s", electionType)
	}

	hash := messagedata.Hash(voteFlag, electionID, string(question.ID), voteString)
	if vote.ID != hash {
		return errors.NewInvalidMessageFieldError("vote ID is not the expected hash")
	}

	return nil
}

func verifyRegisteredVotes(electionEnd messagedata.ElectionEnd, questions map[string]types.Question) error {
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
		return errors.NewInvalidMessageFieldError("registered votes is %s, should be sorted and equal to %s",
			electionEnd.RegisteredVotes, validVotesHash)
	}

	return nil
}

func createElectionResult(questions map[string]types.Question, channelPath string) (message.Message, error) {
	resultElection, err := computeElectionResult(questions, channelPath)
	if err != nil {
		return message.Message{}, err
	}

	buf, err := json.Marshal(resultElection)
	if err != nil {
		return message.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	buf64 := base64.URLEncoding.EncodeToString(buf)

	serverPubKey, err := config.GetServerPublicKeyInstance()
	if err != nil {
		return message.Message{}, err
	}

	serverPubBuf, err := serverPubKey.MarshalBinary()
	if err != nil {
		return message.Message{}, errors.NewInternalServerError("failed to marshal server public key: %v", err)
	}

	signatureBuf, err := sign(buf)
	if err != nil {
		return message.Message{}, err
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

func computeElectionResult(questions map[string]types.Question, channelPath string) (messagedata.ElectionResult, error) {
	db, err := database.GetElectionRepositoryInstance()
	if err != nil {
		return messagedata.ElectionResult{}, err
	}

	electionType, err := db.GetElectionType(channelPath)
	if err != nil {
		return messagedata.ElectionResult{}, err
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

func decryptVote(vote, channelPath string) (int, error) {
	voteBuff, err := base64.URLEncoding.DecodeString(vote)
	if err != nil {
		return -1, errors.NewInvalidMessageFieldError("failed to decode vote: %v", err)
	}
	if len(voteBuff) != 64 {
		return -1, errors.NewInvalidMessageFieldError("vote should be 64 bytes long")
	}

	// K and C are respectively the first and last 32 bytes of the vote
	K := crypto.Suite.Point()
	C := crypto.Suite.Point()

	err = K.UnmarshalBinary(voteBuff[:32])
	if err != nil {
		return -1, errors.NewInvalidMessageFieldError("failed to unmarshal K: %v", err)
	}

	err = C.UnmarshalBinary(voteBuff[32:])
	if err != nil {
		return -1, errors.NewInvalidMessageFieldError("failed to unmarshal C: %v", err)
	}

	db, err := database.GetElectionRepositoryInstance()
	if err != nil {
		return -1, err
	}

	electionSecretKey, err := db.GetElectionSecretKey(channelPath)
	if err != nil {
		return -1, err
	}

	// performs the ElGamal decryption
	S := crypto.Suite.Point().Mul(electionSecretKey, K)
	data, err := crypto.Suite.Point().Sub(C, S).Data()
	if err != nil {
		return -1, errors.NewInternalServerError("failed to decrypt vote: %v", err)
	}

	var index uint16

	// interprets the data as a big endian int
	buf := bytes.NewReader(data)
	err = binary.Read(buf, binary.BigEndian, &index)
	if err != nil {
		return -1, errors.NewInternalServerError("failed to interpret decrypted data: %v", err)
	}

	return int(index), nil
}
