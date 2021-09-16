package election

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"sort"
	"student20_pop/channel"
	"student20_pop/channel/inbox"
	"student20_pop/crypto"
	jsonrpc "student20_pop/message"
	"student20_pop/message/answer"
	"student20_pop/message/messagedata"
	"student20_pop/message/query"
	"student20_pop/message/query/method"
	"student20_pop/message/query/method/message"
	"student20_pop/network/socket"
	"student20_pop/validation"
	"sync"

	"golang.org/x/xerrors"
)

// Attendees represents the attendees in an election.
type Attendees struct {
	sync.Mutex
	store map[string]struct{}
}

// NewAttendees returns a new instance of Attendees.
func NewAttendees() *Attendees {
	return &Attendees{
		store: make(map[string]struct{}),
	}
}

// IsPresent checks if a key representing a user is present in
// the list of attendees.
func (a *Attendees) IsPresent(key string) bool {
	a.Lock()
	defer a.Unlock()

	_, ok := a.store[key]
	return ok
}

// Add adds an attendee to the election.
func (a *Attendees) Add(key string) {
	a.Lock()
	defer a.Unlock()

	a.store[key] = struct{}{}
}

// Copy deep copies the Attendees struct.
func (a *Attendees) Copy() *Attendees {
	a.Lock()
	defer a.Unlock()

	clone := NewAttendees()

	for key := range a.store {
		clone.store[key] = struct{}{}
	}

	return clone
}

// NewChannel ...
func NewChannel(channelPath string, start, end int64, terminated bool,
	questions []messagedata.ElectionSetupQuestion, attendees map[string]struct{},
	msg message.Message, hub channel.HubThingTheChannelNeeds) Channel {

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

		attendees: &Attendees{
			store: attendees,
		},

		hub: hub,
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
	attendees *Attendees

	hub channel.HubThingTheChannelNeeds
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
		return xerrors.Errorf("failed to verify publish message on an election channel: %w", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	jsonData, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)

	if object == "election" {

		switch action {
		case "cast_vote":
			var castVote messagedata.VoteCastVote

			err := msg.UnmarshalData(&castVote)
			if err != nil {
				return xerrors.Errorf("failed to unmarshal cast vote: %v", err)
			}

			err = c.castVoteHelper(msg, castVote)
			if err != nil {
				return xerrors.Errorf("failed to cast vote: %v", err)
			}
		case "end":
			log.Fatal("Not implemented election#end")
		case "result":
			log.Fatal("Not implemented election#result")
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
	log.Printf("received a subscribe with id: %d", msg.ID)
	c.sockets.Upsert(socket)

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *Channel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	log.Printf("received an unsubscribe with id: %d", msg.ID)

	ok := c.sockets.Delete(socketID)

	if !ok {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	log.Printf("received a catchup with id: %d", catchup.ID)

	messages := c.inbox.GetMessages()

	// sort.Slice on messages based on the timestamp
	sort.Slice(messages, func(i, j int) bool {
		return messages[i].StoredTime < messages[j].StoredTime
	})

	result := make([]message.Message, 0, len(messages))

	// iterate and extract the messages[i].message field and
	// append it to the result slice
	for _, msgInfo := range messages {
		result = append(result, msgInfo.Message)
	}

	return result
}

// broadcastToAllClients is a helper message to broadcast a message to all
// subscribers.
func (c *Channel) broadcastToAllClients(msg message.Message) {
	rpcMessage := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "broadcast",
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
		log.Printf("failed to marshal broadcast query: %v", err)
	}

	c.sockets.SendToAll(buf)
}

// VerifyPublishMessage checks if a Publish message is valid
func (c *Channel) VerifyPublishMessage(publish method.Publish) error {
	log.Printf("received a publish with id: %d", publish.ID)

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

func (c *Channel) castVoteHelper(msg message.Message, voteMsg messagedata.VoteCastVote) error {

	if voteMsg.CreatedAt > c.end {
		return answer.NewErrorf(-4, "Vote cast too late, vote casted at %v and election ended at %v", voteMsg.CreatedAt, c.end)
	}

	senderBuf, err := base64.URLEncoding.DecodeString(msg.Sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewError(-4, "Invalid sender public key")
	}

	log.Printf("All the valid pks are %v and %v", c.attendees, senderPoint)

	ok := c.attendees.IsPresent(msg.Sender) || c.hub.GetPubkey().Equal(senderPoint)
	if !ok {
		return answer.NewError(-4, "Only attendees can cast a vote in an election")
	}

	//This should update any previously set vote if the message ids are the same
	c.inbox.StoreMessage(msg)
	for _, q := range voteMsg.Votes {

		qs, ok := c.questions[q.Question]

		if !ok {
			for k, quest := range c.questions {
				fmt.Printf("k=%s. v=%v, id=%s\n", k, quest, quest.id)
			}

			return answer.NewErrorf(-4, "No Question with question ID %s exists", q.Question)
		}

		// this is to handle the case when the organizer must handle multiple votes being cast at the same time
		qs.validVotesMu.Lock()
		earlierVote, ok := qs.validVotes[msg.Sender]

		// if the sender didn't previously cast a vote or if the vote is no longer valid update it
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

		//other votes can now change the list of valid votes
		qs.validVotesMu.Unlock()
	}

	log.Printf("Vote casted with success")
	return nil
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
		qs.validVotes[sender] =
			validVote{
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
