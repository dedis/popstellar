package election

import (
	"bytes"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"popstellar/channel"
	"popstellar/channel/registry"
	"popstellar/crypto"
	"popstellar/inbox"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"strconv"
	"strings"
	"sync"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
)

const (
	msgID              = "msg id"
	failedToDecodeData = "failed to decode message data: %v"
	failedToBroadcast  = "failed to broadcast message: %v"
)

var suite = crypto.Suite

// Channel is used to handle election messages.
type Channel struct {
	sockets   channel.Sockets
	inbox     *inbox.Inbox
	channelID string

	// *baseChannel

	// Type of election channel ("OPEN_BALLOT", "SECRET_BALLOT", ...)
	electionType string

	// Keys of the election if secret ballot, nil otherwise
	pubElectionKey kyber.Point
	secElectionKey kyber.Scalar

	// Starting time of the election
	start int64

	// Ending time of the election
	end int64

	// True if the election has started and false otherwise
	started bool

	// True if the election is over and false otherwise
	terminated bool

	// Questions asked to the participants
	// the key will be the string representation of the id of type byte[]
	questions map[string]*question

	// attendees that took part in the roll call string of their PK
	attendees *attendees

	hub channel.HubFunctionalities

	log zerolog.Logger

	organiserPubKey kyber.Point

	registry registry.MessageRegistry
}

// question represents a question in an election.
type question struct {
	// ID represents the ID of the question.
	ID []byte

	// ballotOptions represents different ballot options.
	ballotOptions []string

	// valid vote mutex.
	validVotesMu sync.RWMutex

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

// attendees represents the attendees in an election.
type attendees struct {
	sync.Mutex
	store map[string]struct{}
}

// NewChannel returns a new initialized election channel
func NewChannel(channelPath string, msg message.Message, msgData messagedata.ElectionSetup,
	attendeesMap map[string]struct{}, hub channel.HubFunctionalities,
	log zerolog.Logger, organizerPubKey kyber.Point) (channel.Channel, error) {

	log = log.With().Str("channel", "election").Logger()

	pubKey, secKey := generateKeys()

	newChannel := &Channel{
		sockets:   channel.NewSockets(),
		inbox:     inbox.NewInbox(channelPath),
		channelID: channelPath,

		electionType:   msgData.Version,
		pubElectionKey: pubKey,
		secElectionKey: secKey,
		start:          msgData.StartTime,
		end:            msgData.EndTime,
		started:        false,
		terminated:     false,
		questions:      getAllQuestionsForElectionChannel(msgData.Questions),

		attendees: &attendees{
			store: attendeesMap,
		},

		hub: hub,

		log: log,

		organiserPubKey: organizerPubKey,
	}

	newChannel.registry = newChannel.newElectionRegistry()

	newChannel.inbox.StoreMessage(msg)

	if newChannel.electionType != messagedata.SecretBallot {
		return newChannel, nil
	}

	err := newChannel.createAndSendElectionKey()
	if err != nil {
		err = xerrors.Errorf("failed to send the election key: %v", err)
	}

	return newChannel, err
}

// ---
// Publish-subscribe / channel.Channel implementation
// ---

// Subscribe is used to handle a subscribe message from the client.
func (c *Channel) Subscribe(socket socket.Socket, msg method.Subscribe) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(msg.ID)).
		Msg("received a subscribe")
	c.sockets.Upsert(socket)

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *Channel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(msg.ID)).
		Msg("received an unsubscribe")

	ok := c.sockets.Delete(socketID)

	if !ok {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Publish is used to handle publish messages in the election channel.
func (c *Channel) Publish(publish method.Publish, socket socket.Socket) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	err := c.verifyMessage(publish.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on an "+
			"election channel: %w", err)
	}

	err = c.handleMessage(publish.Params.Message, socket)
	if err != nil {
		return xerrors.Errorf("failed to handle a publish message: %v", err)
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	c.log.Info().
		Str(msgID, strconv.Itoa(catchup.ID)).
		Msg("received a catchup")

	return c.inbox.GetSortedMessages()
}

// Broadcast is used to handle a broadcast message.
func (c *Channel) Broadcast(broadcast method.Broadcast, socket socket.Socket) error {
	c.log.Info().Msg("received a broadcast")

	err := c.verifyMessage(broadcast.Params.Message)
	if err != nil {
		return xerrors.Errorf("failed to verify broadcast message on an "+
			"election channel: %w", err)
	}

	err = c.handleMessage(broadcast.Params.Message, socket)
	if err != nil {
		return xerrors.Errorf("failed to handle broadcast message: %v", err)
	}

	return nil
}

// ---
// Message handling
// ---

// handleMessage handles a message received in a broadcast or publish method
func (c *Channel) handleMessage(msg message.Message, socket socket.Socket) error {

	err := c.registry.Process(msg, socket)
	if err != nil {
		return xerrors.Errorf("failed to process message: %v", err)
	}

	c.inbox.StoreMessage(msg)

	return nil
}

// newElectionRegistry creates a new registry for the election channel
func (c *Channel) newElectionRegistry() registry.MessageRegistry {
	registry := registry.NewMessageRegistry()

	registry.Register(messagedata.ElectionOpen{}, c.processElectionOpen)
	registry.Register(messagedata.VoteCastVote{}, c.processCastVote)
	registry.Register(messagedata.ElectionEnd{}, c.processElectionEnd)
	registry.Register(messagedata.ElectionResult{}, c.processElectionResult)

	return registry
}

func (c *Channel) processElectionOpen(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.ElectionOpen)
	if !ok {
		return xerrors.Errorf("message '%T' isn't a election#open message", msgData)
	}

	c.log.Info().Str("Election", data.Election).Msg("received a election#open message")

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	senderPoint := crypto.Suite.Point()

	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewErrorf(-4, "invalid sender public key: %s", senderBuf)
	}

	if !c.organiserPubKey.Equal(senderPoint) {
		return answer.NewErrorf(-5, "sender is %s, should be the organizer", msg.Sender)
	}

	var electionOpen messagedata.ElectionOpen

	err = msg.UnmarshalData(&electionOpen)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal publish election end: %v", err)
	}

	// check that data is correct
	err = c.verifyMessageElectionOpen(electionOpen)
	if err != nil {
		return xerrors.Errorf("invalid election#open message: %v", err)
	}

	c.started = true

	err = c.broadcastToAllClients(msg)
	if err != nil {
		return xerrors.Errorf(failedToBroadcast, err)
	}

	return nil
}

// processCastVote is the callback that processes election#cast_vote messages
func (c *Channel) processCastVote(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	_, ok := msgData.(*messagedata.VoteCastVote)
	if !ok {
		return xerrors.Errorf("message '%T' isn't a election#cast_vote message", msgData)
	}

	c.log.Info().Msg("received a election#cast_vote message")

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewError(-4, "invalid sender public key")
	}

	// verify sender is an attendee or the organizer
	ok = c.attendees.isPresent(msg.Sender) || c.organiserPubKey.Equal(senderPoint)
	if !ok {
		return answer.NewError(-4, "only attendees can cast a vote in an election")
	}

	var castVote messagedata.VoteCastVote

	err = msg.UnmarshalData(&castVote)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal cast vote: %v", err)
	}

	err = c.verifyMessageCastVote(castVote)
	if err != nil {
		return xerrors.Errorf("invalid election#cast_vote message: %v", err)
	}

	err = updateVote(msg.MessageID, msg.Sender, castVote, c.questions)
	if err != nil {
		return xerrors.Errorf("failed to update vote: %v", err)
	}

	err = c.broadcastToAllClients(msg)
	if err != nil {
		return xerrors.Errorf(failedToBroadcast, err)
	}

	return nil
}

// processElectionEnd is the callback that processes election#end messages
func (c *Channel) processElectionEnd(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	_, ok := msgData.(*messagedata.ElectionEnd)
	if !ok {
		return xerrors.Errorf("message '%T' isn't a election#end message", msgData)
	}

	c.log.Info().Msg("received a election#end message")

	sender := msg.Sender
	senderBuf, err := base64.URLEncoding.DecodeString(sender)
	if err != nil {
		c.log.Error().Msgf("problem decoding sender public key: %v", err)
		return xerrors.Errorf("sender is %s, should be base64", sender)
	}

	// check sender is a valid public key
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		c.log.Error().Msgf("public key unmarshal problem: %v", err)
		return answer.NewErrorf(-4, "sender is %s, should be a valid public key: %v", sender, err)
	}

	// check sender of the election end message is the organizer
	if !c.organiserPubKey.Equal(senderPoint) {
		return answer.NewErrorf(-5, "sender is %s, should be the organizer", msg.Sender)
	}

	var electionEnd messagedata.ElectionEnd

	err = msg.UnmarshalData(&electionEnd)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal publish election end: %v", err)
	}

	// check that data is correct
	err = c.verifyMessageElectionEnd(electionEnd)
	if err != nil {
		return xerrors.Errorf("invalid election#end message: %v", err)
	}

	c.started = false
	c.terminated = true

	err = c.broadcastToAllClients(msg)
	if err != nil {
		return xerrors.Errorf(failedToBroadcast, err)
	}

	// broadcast election result message
	err = c.broadcastElectionResult()
	if err != nil {
		return xerrors.Errorf("problem sending election#result message: %v", err)
	}

	return nil
}

// processElectionResult is the callback that processes election#result messages
func (c *Channel) processElectionResult(msg message.Message, msgData interface{},
	_ socket.Socket) error {

	data, ok := msgData.(*messagedata.ElectionResult)
	if !ok {
		return xerrors.Errorf("message '%T' isn't a election#result message", msgData)
	}

	c.log.Info().Msg("received a election#result message")

	// verify that the question ids are base64URL encoded
	for i, q := range data.Questions {
		_, err := base64.URLEncoding.DecodeString(q.ID)
		if err != nil {
			return xerrors.Errorf("invalid election#result message: question "+
				"id %d %s, should be base64URL encoded", i, q.ID)
		}
	}

	return nil
}

// verifyMessage checks if a message in a Publish or Broadcast method is valid
func (c *Channel) verifyMessage(msg message.Message) error {
	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf(failedToDecodeData, err)
	}

	// Verify the data
	err = c.hub.GetSchemaValidator().VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return xerrors.Errorf("failed to verify json schema: %w", err)
	}

	// Check if the message already exists
	_, ok := c.inbox.GetMessage(msg.MessageID)
	if ok {
		return answer.NewError(-3, "message already exists")
	}

	return nil
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *Channel) broadcastToAllClients(msg message.Message) error {
	c.log.Info().
		Str(msgID, msg.MessageID).
		Msg("broadcasting message to all clients")

	rpcMessage := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodBroadcast,
		},
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			c.channelID,
			msg,
		},
	}

	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		return xerrors.Errorf("failed to marshal broadcast query: %v", err)
	}

	c.sockets.SendToAll(buf)

	return nil
}

// createAndSendElectionKey creates and sends the election#key message
func (c *Channel) createAndSendElectionKey() error {
	// Marshalls the election key
	ekBuf, err := c.pubElectionKey.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal the election key: %v", err)
	}

	msgData := messagedata.ElectionKey{
		Object:   messagedata.ElectionObject,
		Action:   messagedata.ElectionActionKey,
		Election: c.getElectionID(),
		Key:      base64.URLEncoding.EncodeToString(ekBuf),
	}

	// Marshalls the message data
	dataBuf, err := json.Marshal(&msgData)
	if err != nil {
		return xerrors.Errorf("failed to marshal the data: %v", err)
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	// Marshalls the server public key
	pk := c.hub.GetPubKeyServ()
	skBuf, err := pk.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal the server key: %v", err)
	}

	// Sign the data
	signatureBuf, err := c.hub.Sign(dataBuf)
	if err != nil {
		return xerrors.Errorf("failed to sign the data: %v", err)
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	electionKeyMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(skBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	err = c.broadcastToAllClients(electionKeyMsg)
	if err != nil {
		return xerrors.Errorf(failedToBroadcast, err)
	}

	c.inbox.StoreMessage(electionKeyMsg)

	return nil
}

// getElectionID extracts and returns the electionID from the channelID
func (c *Channel) getElectionID() string {
	// split channel to [lao id, election id]
	noRoot := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	IDs := strings.Split(noRoot, "/")

	return IDs[1]
}

// generateKeys generates and returns a key pair
func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	return point, secret
}

// isPresent checks if a key representing a user is present in
// the list of attendees.
func (a *attendees) isPresent(key string) bool {
	a.Lock()
	defer a.Unlock()

	_, ok := a.store[key]

	return ok
}

// broadcastElectionResult gathers and broadcasts the results of an election
func (c *Channel) broadcastElectionResult() error {
	resultElection, err := c.gatherResults(c.questions, c.log)
	if err != nil {
		return xerrors.Errorf("failed to gather results: %v", err)
	}

	dataBuf, err := json.Marshal(&resultElection)
	if err != nil {
		return xerrors.Errorf("failed to marshal the data: %v", err)
	}

	newData64 := base64.URLEncoding.EncodeToString(dataBuf)

	pk := c.hub.GetPubKeyServ()
	pkBuf, err := pk.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal the public key: %v", err)
	}

	// Sign the data
	signatureBuf, err := c.hub.Sign(dataBuf)
	if err != nil {
		return xerrors.Errorf("failed to sign the data: %v", err)
	}

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	electionResultMsg := message.Message{
		Data:              newData64,
		Sender:            base64.URLEncoding.EncodeToString(pkBuf),
		Signature:         signature,
		MessageID:         messagedata.Hash(newData64, signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	c.broadcastToAllClients(electionResultMsg)

	return nil
}

func (c *Channel) gatherResults(questions map[string]*question,
	log zerolog.Logger) (messagedata.ElectionResult, error) {

	log.Info().Msgf("gathering results for the election")

	resultElection := messagedata.ElectionResult{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.ElectionActionResult,
		Questions: []messagedata.ElectionResultQuestion{},
	}

	for id := range questions {
		question, ok := questions[id]
		if !ok {
			return resultElection, xerrors.Errorf("no question with this "+
				"questionId '%s' was recorded", id)
		}

		votes := question.validVotes
		if question.method == "Plurality" {
			numberOfVotesPerBallotOption := make([]int, len(question.ballotOptions))
			for _, vote := range votes {
				index, err := c.getVoteIndex(vote)
				if err == nil {
					numberOfVotesPerBallotOption[index]++
				}
			}

			res := gatherOptionCounts(numberOfVotesPerBallotOption, question.ballotOptions)

			electResult := messagedata.ElectionResultQuestion{
				ID:     id,
				Result: res,
			}

			resultElection.Questions = append(resultElection.Questions, electResult)

			log.Info().
				Str("question id", id).
				Msg("appending a question with the count and result")
		}
	}

	return resultElection, nil
}

// getVoteIndex extracts the index of the vote from the validVotes
func (c *Channel) getVoteIndex(vote validVote) (int, error) {
	switch m := c.electionType; m {
	// open ballot votes have the index in plain text
	case messagedata.OpenBallot:
		index, _ := vote.index.(int)
		return index, nil
	// secret ballot votes must be decrypted to get the index
	case messagedata.SecretBallot:
		temp, _ := vote.index.(string)
		index, err := c.decryptVote(temp)
		if err != nil {
			// logs a warning because we don't stop the tallying for non-conform votes
			// we just disregards them
			c.log.Warn().Msgf("failed to decrypt a vote: %v", err)
			return index, err
		}
		return index, nil
	default:
		return -1, answer.NewErrorf(-6, "election type shouldn't be %s", m)
	}
}

// decryptVote decrypts the vote using ElGamal under Ed25519 curve
func (c *Channel) decryptVote(vote string) (int, error) {
	// vote is encoded in base64
	votebuf, err := base64.URLEncoding.DecodeString(vote)
	if err != nil {
		return -1, answer.NewErrorf(-4, "vote %s is not base64 encoded", vote)
	}

	// K and C are respectively the first and last 32 bytes of the vote
	K := crypto.Suite.Point()
	C := crypto.Suite.Point()

	err = K.UnmarshalBinary(votebuf[:32])
	if err != nil {
		return -1, answer.NewErrorf(-4, "failed to unmarshal vote %s", vote)
	}

	err = C.UnmarshalBinary(votebuf[32:])
	if err != nil {
		return -1, answer.NewErrorf(-4, "failed to unmarshal vote %s", vote)
	}

	// performs the ElGamal decryption
	S := crypto.Suite.Point().Mul(c.secElectionKey, K)
	data, err := crypto.Suite.Point().Sub(C, S).Data()
	if err != nil {
		return -1, answer.NewErrorf(-4, "vote data is invalid")
	}

	var index int

	// interprets the data as a big endian int
	buf := bytes.NewReader(data)
	err = binary.Read(buf, binary.BigEndian, &index)
	if err != nil {
		return -1, answer.NewErrorf(-4, "vote data is invalid")
	}

	return index, nil
}

func gatherOptionCounts(count []int, options []string) []messagedata.ElectionResultQuestionResult {
	questionResults := make([]messagedata.ElectionResultQuestionResult, 0)
	for i, option := range options {
		questionResults = append(questionResults, messagedata.ElectionResultQuestionResult{
			BallotOption: option,
			Count:        count[i],
		})
	}

	return questionResults
}

func checkMethodProperties(method string, length int) error {
	if method == "Plurality" && length < 1 {
		return answer.NewError(-4, "No ballot option was chosen for plurality voting method")
	}
	if method == "Approval" && length != 1 {
		return answer.NewError(-4, "Cannot choose multiple ballot options "+
			"on approval voting method")
	}

	return nil
}

func updateVote(msgID string, sender string, castVote messagedata.VoteCastVote,
	questions map[string]*question) error {

	// this should update any previously set vote if the message ids are the same
	for _, vote := range castVote.Votes {

		qs, ok := questions[vote.Question]
		if !ok {
			return answer.NewErrorf(-4, "no Question with question ID %s exists", vote.Question)
		}

		// this is to handle the case when the organizer must handle multiple
		// votes being cast at the same time
		qs.validVotesMu.Lock()
		earlierVote, ok := qs.validVotes[sender]

		// if the sender didn't previously cast a vote or if the vote is no
		// longer valid update it
		err := checkMethodProperties(qs.method, 1)
		if err != nil {
			return xerrors.Errorf("failed to validate voting method props: %w", err)
		}

		if !ok || earlierVote.voteTime > castVote.CreatedAt {
			qs.validVotes[sender] = validVote{
				msgID,
				vote.ID,
				castVote.CreatedAt,
				vote.Vote,
			}
		}

		// other votes can now change the list of valid votes
		qs.validVotesMu.Unlock()
	}

	return nil
}

func getAllQuestionsForElectionChannel(questions []messagedata.ElectionSetupQuestion) map[string]*question {
	qs := make(map[string]*question)
	for _, q := range questions {
		ballotOpts := make([]string, len(q.BallotOptions))
		copy(ballotOpts, q.BallotOptions)

		qs[q.ID] = &question{
			ID:            []byte(q.ID),
			ballotOptions: ballotOpts,
			validVotesMu:  sync.RWMutex{},
			validVotes:    make(map[string]validVote),
			method:        q.VotingMethod,
		}
	}

	return qs
}
