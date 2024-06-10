package helection

import (
	"bytes"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata"
	melection2 "popstellar/internal/handler/messagedata/election/melection"
	"popstellar/internal/handler/messagedata/election/telection"
	"popstellar/internal/handler/messagedata/lao/mlao"
	"popstellar/internal/validation"
	"sort"
)

const (
	voteFlag = "Vote"
)

type Config interface {
	GetServerPublicKey() kyber.Point
	Sign(data []byte) ([]byte, error)
}

type Subscribers interface {
	BroadcastToAllClients(msg mmessage.Message, channel string) error
}

type Repository interface {

	// GetLAOOrganizerPubKey returns the organizer public key of an election.
	GetLAOOrganizerPubKey(electionID string) (kyber.Point, error)

	// GetElectionSecretKey returns the secret key of an election.
	GetElectionSecretKey(electionID string) (kyber.Scalar, error)

	// IsElectionStartedOrEnded returns true if the election is started or ended.
	IsElectionStartedOrEnded(electionID string) (bool, error)

	// IsElectionEnded returns true if the election is ended.
	IsElectionEnded(electionID string) (bool, error)

	//IsElectionStarted returns true if the election is started.
	IsElectionStarted(electionID string) (bool, error)

	// GetElectionType returns the type of an election.
	GetElectionType(electionID string) (string, error)

	// GetElectionCreationTime returns the creation time of an election.
	GetElectionCreationTime(electionID string) (int64, error)

	// GetElectionAttendees returns the attendees of an election.
	GetElectionAttendees(electionID string) (map[string]struct{}, error)

	// GetElectionQuestions returns the questions of an election.
	GetElectionQuestions(electionID string) (map[string]telection.Question, error)

	// GetElectionQuestionsWithValidVotes returns the questions of an election with valid votes.
	GetElectionQuestionsWithValidVotes(electionID string) (map[string]telection.Question, error)

	// StoreElectionEndWithResult stores a message and an election result message inside the database.
	StoreElectionEndWithResult(channelID string, msg, electionResultMsg mmessage.Message) error

	// StoreMessageAndData stores a message with an object and an action inside the database.
	StoreMessageAndData(channelID string, msg mmessage.Message) error
}

type Handler struct {
	conf   Config
	subs   Subscribers
	db     Repository
	schema *validation.SchemaValidator
}

func New(conf Config, subs Subscribers,
	db Repository, schema *validation.SchemaValidator) *Handler {
	return &Handler{
		conf:   conf,
		subs:   subs,
		db:     db,
		schema: schema,
	}
}

func (h *Handler) Handle(channelPath string, msg mmessage.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode message data: %v", err)
	}

	err = h.schema.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return err
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return err
	}

	storeMessage := true

	switch object + "#" + action {
	case messagedata.ElectionObject + "#" + messagedata.VoteActionCastVote:
		err = h.handleVoteCastVote(msg, channelPath)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionOpen:
		err = h.handleElectionOpen(msg, channelPath)
	case messagedata.ElectionObject + "#" + messagedata.ElectionActionEnd:
		err = h.handleElectionEnd(msg, channelPath)
		storeMessage = false
	default:
		err = errors.NewInvalidMessageFieldError("failed to Handle %s#%s, invalid object#action", object, action)
	}

	if err != nil {
		return err
	}

	if storeMessage {
		err = h.db.StoreMessageAndData(channelPath, msg)
		if err != nil {
			return err
		}
	}

	return nil
}

func (h *Handler) handleVoteCastVote(msg mmessage.Message, channelPath string) error {
	var voteCastVote melection2.VoteCastVote
	err := msg.UnmarshalData(&voteCastVote)
	if err != nil {
		return err
	}

	err = h.verifySenderElection(msg, channelPath, false)
	if err != nil {
		return err
	}

	//verify message data
	err = voteCastVote.Verify(channelPath)
	if err != nil {
		return err
	}

	if voteCastVote.CreatedAt < 0 {
		return errors.NewInvalidMessageFieldError("cast vote created at is negative")
	}

	// verify VoteCastVote created after election createdAt
	createdAt, err := h.db.GetElectionCreationTime(channelPath)
	if err != nil {
		return err
	}

	if createdAt > voteCastVote.CreatedAt {
		return errors.NewInvalidMessageFieldError("cast vote cannot have a creation time prior to election setup")
	}

	// verify votes
	for _, vote := range voteCastVote.Votes {
		err = h.verifyVote(vote, channelPath, voteCastVote.Election)
		if err != nil {
			return err
		}
	}

	// Just store the vote cast if the election has ended because will not have any influence on the result
	ended, err := h.db.IsElectionEnded(channelPath)
	if err != nil {
		return err
	}
	if ended {
		return nil
	}

	// verify that the election is open
	started, err := h.db.IsElectionStarted(channelPath)
	if err != nil {
		return err
	}
	if !started {
		return errors.NewInvalidMessageFieldError("election is not started")
	}

	return h.subs.BroadcastToAllClients(msg, channelPath)
}

func (h *Handler) handleElectionOpen(msg mmessage.Message, channelPath string) error {
	var electionOpen melection2.ElectionOpen
	err := msg.UnmarshalData(&electionOpen)
	if err != nil {
		return err
	}

	err = h.verifySenderElection(msg, channelPath, true)
	if err != nil {
		return err
	}

	// verify message data
	err = electionOpen.Verify(channelPath)
	if err != nil {
		return err
	}

	// verify if the election was already started or terminated
	ok, err := h.db.IsElectionStartedOrEnded(channelPath)
	if err != nil {
		return err
	}
	if ok {
		return errors.NewInvalidMessageFieldError("election is already started or ended")
	}

	createdAt, err := h.db.GetElectionCreationTime(channelPath)
	if err != nil {
		return err
	}
	if electionOpen.OpenedAt < createdAt {
		return errors.NewInvalidMessageFieldError("election open cannot have a creation time prior to election setup")
	}

	return h.subs.BroadcastToAllClients(msg, channelPath)
}

func (h *Handler) handleElectionEnd(msg mmessage.Message, channelPath string) error {
	var electionEnd melection2.ElectionEnd
	err := msg.UnmarshalData(&electionEnd)
	if err != nil {
		return err
	}

	err = h.verifySenderElection(msg, channelPath, true)
	if err != nil {
		return err
	}

	err = h.verifyElectionEnd(electionEnd, channelPath)
	if err != nil {
		return err
	}

	questions, err := h.db.GetElectionQuestionsWithValidVotes(channelPath)
	if err != nil {
		return err
	}

	if len(electionEnd.RegisteredVotes) != 0 {
		err = h.verifyRegisteredVotes(electionEnd, questions)
		if err != nil {
			return err
		}
	}

	electionResultMsg, err := h.createElectionResult(questions, channelPath)
	if err != nil {
		return err
	}

	err = h.db.StoreElectionEndWithResult(channelPath, msg, electionResultMsg)
	if err != nil {
		return err
	}

	err = h.subs.BroadcastToAllClients(msg, channelPath)
	if err != nil {
		return err
	}

	return h.subs.BroadcastToAllClients(electionResultMsg, channelPath)
}

func (h *Handler) verifyElectionEnd(electionEnd melection2.ElectionEnd, channelPath string) error {
	// verify message data
	err := electionEnd.Verify(channelPath)
	if err != nil {
		return err

	}

	// verify if the election is started
	started, err := h.db.IsElectionStarted(channelPath)
	if err != nil {
		return err
	}
	if !started {
		return errors.NewInvalidMessageFieldError("election was not started")
	}

	// verify if the timestamp is stale
	createdAt, err := h.db.GetElectionCreationTime(channelPath)
	if err != nil {
		return err
	}

	if electionEnd.CreatedAt < createdAt {
		return errors.NewInvalidMessageFieldError("election end cannot have a creation time prior to election setup")
	}

	return nil
}

func (h *Handler) verifySenderElection(msg mmessage.Message, channelPath string, onlyOrganizer bool) error {
	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode sender: %v", err)
	}

	senderPubKey := crypto.Suite.Point()
	err = senderPubKey.UnmarshalBinary(senderBuf)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to unmarshal sender: %v", err)
	}

	organizerPubKey, err := h.db.GetLAOOrganizerPubKey(channelPath)
	if err != nil {
		return err
	}

	if onlyOrganizer && !senderPubKey.Equal(organizerPubKey) {
		return errors.NewAccessDeniedError("sender is not the organizer of the oldchannel")
	}

	if onlyOrganizer {
		return nil
	}

	attendees, err := h.db.GetElectionAttendees(channelPath)
	if err != nil {
		return err
	}

	_, ok := attendees[msg.Sender]
	if !ok && !senderPubKey.Equal(organizerPubKey) {
		return errors.NewAccessDeniedError("sender is not an attendee or the organizer of the election")
	}

	return nil
}

func (h *Handler) verifyVote(vote melection2.Vote, channelPath, electionID string) error {
	questions, err := h.db.GetElectionQuestions(channelPath)
	if err != nil {
		return err
	}

	question, ok := questions[vote.Question]
	if !ok {
		return errors.NewInvalidMessageFieldError("Question does not exist")
	}

	electionType, err := h.db.GetElectionType(channelPath)
	if err != nil {
		return err
	}

	var voteString string
	switch electionType {
	case mlao.OpenBallot:
		voteInt, ok := vote.Vote.(int)
		if !ok {
			return errors.NewInvalidMessageFieldError("vote in open ballot should be an integer")
		}
		voteString = fmt.Sprintf("%d", voteInt)

	case mlao.SecretBallot:
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

func (h *Handler) verifyRegisteredVotes(electionEnd melection2.ElectionEnd,
	questions map[string]telection.Question) error {
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

func (h *Handler) createElectionResult(questions map[string]telection.Question, channelPath string) (mmessage.Message, error) {
	resultElection, err := h.computeElectionResult(questions, channelPath)
	if err != nil {
		return mmessage.Message{}, err
	}

	buf, err := json.Marshal(resultElection)
	if err != nil {
		return mmessage.Message{}, errors.NewJsonMarshalError(err.Error())
	}

	buf64 := base64.URLEncoding.EncodeToString(buf)

	serverPubBuf, err := h.conf.GetServerPublicKey().MarshalBinary()
	if err != nil {
		return mmessage.Message{}, errors.NewInternalServerError("failed to marshal server public key: %v", err)
	}

	signatureBuf, err := h.conf.Sign(buf)
	if err != nil {
		return mmessage.Message{}, err
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	electionResultMsg := mmessage.Message{
		Data:              buf64,
		Sender:            base64.URLEncoding.EncodeToString(serverPubBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(buf64, signature),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	return electionResultMsg, nil
}

func (h *Handler) computeElectionResult(questions map[string]telection.Question, channelPath string) (melection2.ElectionResult, error) {
	electionType, err := h.db.GetElectionType(channelPath)
	if err != nil {
		return melection2.ElectionResult{}, err
	}

	result := make([]melection2.ElectionResultQuestion, 0)

	for id, question := range questions {
		if question.Method != mlao.PluralityMethod {
			continue
		}

		votesPerBallotOption := make([]int, len(question.BallotOptions))
		for _, validVote := range question.ValidVotes {
			index, ok := h.getVoteIndex(validVote, electionType, channelPath)
			if ok && index >= 0 && index < len(question.BallotOptions) {
				votesPerBallotOption[index]++
			}
		}

		var questionResults []melection2.ElectionResultQuestionResult
		for i, options := range question.BallotOptions {
			questionResults = append(questionResults, melection2.ElectionResultQuestionResult{
				BallotOption: options,
				Count:        votesPerBallotOption[i],
			})
		}

		electionResult := melection2.ElectionResultQuestion{
			ID:     id,
			Result: questionResults,
		}

		result = append(result, electionResult)
	}

	resultElection := melection2.ElectionResult{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.ElectionActionResult,
		Questions: result,
	}

	return resultElection, nil
}

func (h *Handler) getVoteIndex(vote telection.ValidVote, electionType, channelPath string) (int, bool) {
	switch electionType {
	case mlao.OpenBallot:
		index, _ := vote.Index.(int)
		return index, true

	case mlao.SecretBallot:
		encryptedVote, _ := vote.Index.(string)
		index, err := h.decryptVote(encryptedVote, channelPath)
		if err != nil {
			return index, false
		}
		return index, true
	}
	return -1, false
}

func (h *Handler) decryptVote(vote, channelPath string) (int, error) {
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

	electionSecretKey, err := h.db.GetElectionSecretKey(channelPath)
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
