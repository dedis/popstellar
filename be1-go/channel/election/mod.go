package election

import (
	"encoding/base64"
	"encoding/json"
	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
	"popstellar/channel"
	"popstellar/channel/inbox"
	"popstellar/crypto"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"strconv"
	"sync"
)

const msgID = "msg id"

// attendees represents the attendees in an election.
type attendees struct {
	sync.Mutex
	store map[string]struct{}
}

// newAttendees returns a new instance of Attendees.
func newAttendees() *attendees {
	return &attendees{
		store: make(map[string]struct{}),
	}
}

// IsPresent checks if a key representing a user is present in
// the list of attendees.
func (a *attendees) IsPresent(key string) bool {
	a.Lock()
	defer a.Unlock()

	_, ok := a.store[key]
	return ok
}

// Add adds an attendee to the election.
func (a *attendees) Add(key string) {
	a.Lock()
	defer a.Unlock()

	a.store[key] = struct{}{}
}

// Copy deep copies the Attendees struct.
func (a *attendees) Copy() *attendees {
	a.Lock()
	defer a.Unlock()

	clone := newAttendees()

	for key := range a.store {
		clone.store[key] = struct{}{}
	}

	return clone
}

// NewChannel returns a new initialized election channel
func NewChannel(channelPath string, start, end int64, terminated bool, questions []messagedata.ElectionSetupQuestion,
	attendeesMap map[string]struct{}, msg message.Message, hub channel.HubFunctionalities, log zerolog.Logger) Channel {

	log = log.With().Str("channel", "election").Logger()

	// Saving on election channel too so it self-contains the entire election history
	// electionCh.inbox.storeMessage(msg)
	return Channel{
		sockets:   channel.NewSockets(),
		inbox:     inbox.NewInbox(channelPath),
		channelID: channelPath,

		start:      start,
		end:        end,
		terminated: terminated,
		questions:  getAllQuestionsForElectionChannel(questions),

		attendees: &attendees{
			store: attendeesMap,
		},

		hub: hub,

		log: log,
	}
}

// Channel is used to handle election messages.
type Channel struct {
	sockets   channel.Sockets
	inbox     *inbox.Inbox
	channelID string

	// *baseChannel

	// Starting time of the election
	start int64

	// Ending time of the election
	end int64

	// True if the election is over and false otherwise
	terminated bool

	// Questions asked to the participants
	//the key will be the string representation of the id of type byte[]
	questions map[string]*question

	// attendees that took part in the roll call string of their PK
	attendees *attendees

	hub channel.HubFunctionalities

	log zerolog.Logger
}

// question represents a question in an election.
type question struct {
	// ID represents the id of the question.
	id []byte

	// ballotOptions represents different ballot options.
	ballotOptions []string

	//valid vote mutex.
	validVotesMu sync.RWMutex

	// validVotes represents the list of all valid votes. The key represents
	// the public key of the person casting the vote.
	validVotes map[string]validVote

	// method represents the voting method of the election. Either "Plurality"
	// or "Approval".
	method string
}

type validVote struct {
	// voteTime represents the time of the creation of the vote.
	voteTime int64

	// indexes represents the indexes of the ballot options
	indexes []int
}

// Publish is used to handle publish messages in the election channel.
func (c *Channel) Publish(publish method.Publish) error {
	err := c.VerifyPublishMessage(publish)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message on an "+
			"election channel: %w", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	jsonData, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)

	if object == messagedata.ElectionObject {

		switch action {
		case messagedata.VoteActionCastVote:
			err := c.publishCastVote(msg)
			if err != nil {
				return xerrors.Errorf("failed to cast vote: %v", err)
			}
		case messagedata.ElectionActionEnd:
			err := c.publishEndElection(msg)
			if err != nil {
				return xerrors.Errorf("failed to end election: %v", err)
			}
		case messagedata.ElectionActionResult:
			err = c.publishResultElection(msg)
			if err != nil {
				return xerrors.Errorf("failed to end election: %v", err)
			}
		default:
			return answer.NewInvalidActionError(action)
		}
	}

	if err != nil {
		return xerrors.Errorf("failed to process %q action: %w", action, err)
	}

	c.broadcastToAllClients(msg)

	return nil
}

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

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	c.log.Info().
		Str(msgID, strconv.Itoa(catchup.ID)).
		Msg("received a catchup")

	return c.inbox.GetSortedMessages()
}

// Broadcast is used to handle a broadcast message.
func (c *Channel) Broadcast(msg method.Broadcast) error {
	err := xerrors.Errorf("an election channel shouldn't need to broadcast a message")
	c.log.Err(err)
	return err
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *Channel) broadcastToAllClients(msg message.Message) {
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
		c.log.Err(err).Msg("failed to marshal broadcast query")
	}

	c.sockets.SendToAll(buf)
}

// VerifyPublishMessage checks if a Publish message is valid
func (c *Channel) VerifyPublishMessage(publish method.Publish) error {
	c.log.Info().
		Str(msgID, strconv.Itoa(publish.ID)).
		Msg("received a publish")

	// Check if the structure of the message is correct
	msg := publish.Params.Message

	jsonData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	// Verify the data
	err = c.hub.GetSchemaValidator().VerifyJSON(jsonData, validation.Data)
	if err != nil {
		return xerrors.Errorf("failed to verify json schema: %w", err)
	}

	// Check if the message already exists
	if _, ok := c.inbox.GetMessage(msg.MessageID); ok {
		return answer.NewError(-3, "message already exists")
	}

	return nil
}

func (c *Channel) publishCastVote(msg message.Message) error {
	voteMsg, err := c.getAndVerifyCastVoteMessage(msg)
	if err != nil {
		return xerrors.Errorf("failed to get and verify vote message: %v", err)
	}

	//This should update any previously set vote if the message ids are the same
	c.inbox.StoreMessage(msg)
	for _, q := range voteMsg.Votes {

		qs, ok := c.questions[q.Question]

		if !ok {
			return answer.NewErrorf(-4, "no Question with question ID %s exists", q.Question)
		}

		// this is to handle the case when the organizer must handle multiple
		// votes being cast at the same time
		qs.validVotesMu.Lock()
		earlierVote, ok := qs.validVotes[msg.Sender]

		// if the sender didn't previously cast a vote or if the vote is no
		// longer valid update it
		if err := checkMethodProperties(qs.method, len(q.Vote)); err != nil {
			return xerrors.Errorf("failed to validate voting method props: %w", err)
		}

		if !ok {
			qs.validVotes[msg.Sender] = validVote{
				voteMsg.CreatedAt,
				q.Vote,
			}
		} else {
			changeVote(qs, earlierVote, msg.Sender, voteMsg.CreatedAt, q.Vote)
		}

		// other votes can now change the list of valid votes
		qs.validVotesMu.Unlock()
	}

	return nil
}

func (c *Channel) getAndVerifyCastVoteMessage(msg message.Message) (messagedata.VoteCastVote, error) {
	var voteMsg messagedata.VoteCastVote

	err := msg.UnmarshalData(&voteMsg)
	if err != nil {
		return voteMsg, xerrors.Errorf("failed to unmarshal cast vote: %v", err)
	}

	// note that CreatedAt is provided by the client and can't be fully trusted.
	// We leave this check as is until we have a better solution.
	if voteMsg.CreatedAt > c.end {
		return voteMsg, answer.NewErrorf(-4, "vote cast too late, vote casted at %v "+
			"and election ended at %v", voteMsg.CreatedAt, c.end)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return voteMsg, xerrors.Errorf("failed to decode sender key: %v", err)
	}

	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return voteMsg, answer.NewError(-4, "invalid sender public key")
	}

	ok := c.attendees.IsPresent(msg.Sender) || c.hub.GetPubkey().Equal(senderPoint)
	if !ok {
		return voteMsg, answer.NewError(-4, "only attendees can cast a vote in an election")
	}

	return voteMsg, nil
}

func (c *Channel) publishEndElection(msg message.Message) error {

	var endElection messagedata.ElectionEnd

	err := msg.UnmarshalData(&endElection)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal publish end election: %v", err)
	}

	if endElection.CreatedAt < c.end {
		return xerrors.Errorf("can't end the election before its "+
			"end: %d < %d", endElection.CreatedAt, c.end)
	}

	// TODO:
	// if len(endElection.RegisteredVotes) == 0 {
	// 	// we allow empty vote
	// } else {
	// 	// TODO: finish the hashing check

	// 	// since we eliminated (in cast vote) the duplicate votes we are sure
	// 	// that the voter casted one vote for one question

	// 	//for _, question := range c.questions {
	// 	//  _, err := sortHashVotes(question.validVotes)
	// 	//  if err != nil {
	// 	//      return &message.Error{
	// 	//          Code:        -4,
	// 	//          Description: "Error while hashing",
	// 	//      }
	// 	//  }
	// 	//  if endElectionData.RegisteredVotes != hashed {
	// 	//      return &message.Error{
	// 	//          Code:        -4,
	// 	//          Description: "Received registered votes is not correct",
	// 	//      }
	// 	//  }
	// 	//}
	// }

	c.broadcastToAllClients(msg)

	c.inbox.StoreMessage(msg)

	return nil
}

func (c *Channel) publishResultElection(msg message.Message) error {
	resultElection := messagedata.ElectionResult{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.ElectionActionResult,
		Questions: []messagedata.ElectionResultQuestion{},
	}

	for id := range c.questions {
		question, ok := c.questions[id]
		if !ok {
			return xerrors.Errorf("No question with this questionId '%s' was recorded", id)
		}

		votes := question.validVotes
		if question.method == "Plurality" {
			numberOfVotesPerBallotOption := make([]int, len(question.ballotOptions))
			for _, vote := range votes {
				for _, ballotIndex := range vote.indexes {
					numberOfVotesPerBallotOption[ballotIndex]++
				}
			}

			res := gatherOptionCounts(numberOfVotesPerBallotOption, question.ballotOptions)

			electResult := messagedata.ElectionResultQuestion{
				ID:     id,
				Result: res,
			}

			resultElection.Questions = append(resultElection.Questions, electResult)

			c.log.Info().
				Str("question id", id).
				Msg("appending a question with the count and result")
		}
	}

	jsonbuf, err := json.Marshal(&resultElection)
	if err != nil {
		return xerrors.Errorf("failed to marshal result election: %v", err)
	}

	msg.Data = base64.URLEncoding.EncodeToString(jsonbuf)

	c.broadcastToAllClients(msg)

	c.inbox.StoreMessage(msg)

	return nil
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
		return answer.NewError(-4, "Cannot choose multiple ballot options on approval voting method")
	}
	return nil
}

func changeVote(qs *question, earlierVote validVote, sender string, created int64, indexes []int) {
	if earlierVote.voteTime > created {
		qs.validVotes[sender] = validVote{
			voteTime: created,
			indexes:  indexes,
		}
	}
}

func getAllQuestionsForElectionChannel(questions []messagedata.ElectionSetupQuestion) map[string]*question {

	qs := make(map[string]*question)
	for _, q := range questions {
		ballotOpts := make([]string, len(q.BallotOptions))
		for i, b := range q.BallotOptions {
			ballotOpts[i] = b
		}

		qs[q.ID] = &question{
			id:            []byte(q.ID),
			ballotOptions: ballotOpts,
			validVotesMu:  sync.RWMutex{},
			validVotes:    make(map[string]validVote),
			method:        q.VotingMethod,
		}
	}
	return qs
}
