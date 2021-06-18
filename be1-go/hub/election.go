package hub

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"

	//"encoding/json"
	"fmt"
	"golang.org/x/xerrors"
	"log"
	"sort"
	"student20_pop"
	"student20_pop/message"
	"sync"
)

type Attendees struct {
	sync.Mutex
	store map[string]struct{}
}

func NewAttendees() *Attendees {
	return &Attendees{
		store: make(map[string]struct{}),
	}
}

func (a *Attendees) IsPresent(key string) bool {
	a.Lock()
	defer a.Unlock()

	_, ok := a.store[key]
	return ok
}

func (a *Attendees) Add(key string) {
	a.Lock()
	defer a.Unlock()

	a.store[key] = struct{}{}
}

func (a *Attendees) Copy() *Attendees {
	a.Lock()
	defer a.Unlock()

	clone := NewAttendees()

	for key := range a.store {
		clone.store[key] = struct{}{}
	}

	return clone
}

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

	// attendees that took part in the roll call string of their PK
	attendees *Attendees
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

func (c *laoChannel) createElection(msg message.Message) error {
	organizerHub := c.hub

	organizerHub.Lock()
	defer organizerHub.Unlock()

	// Check the data
	data, ok := msg.Data.(*message.ElectionSetupData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: "failed to cast data to SetupElectionData",
		}
	}

	// Check if the Lao ID of the message corresponds to the channel ID
	encodedLaoID := base64.URLEncoding.EncodeToString(data.LaoID)
	channelID := c.channelID[6:]
	if channelID != encodedLaoID {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("Lao ID of the message (Lao: %s) is different from the channelID (channel: %s)", encodedLaoID, channelID),
		}
	}

	// Compute the new election channel id
	encodedElectionID := base64.URLEncoding.EncodeToString(data.ID)
	encodedID := encodedLaoID + "/" + encodedElectionID

	// Create the new election channel
	electionCh := electionChannel{
		createBaseChannel(organizerHub, rootPrefix+encodedID),
		data.StartTime,
		data.EndTime,
		false,
		getAllQuestionsForElectionChannel(data.Questions),
		c.attendees,
	}

	// Saving the election channel creation message on the lao channel
	c.inbox.storeMessage(msg)

	// Saving on election channel too so it self-contains the entire election history
	electionCh.inbox.storeMessage(msg)

	// Add the new election channel to the organizerHub
	organizerHub.channelByID[encodedID] = &electionCh

	return nil
}

func (c *electionChannel) Publish(publish message.Publish) error {
	err := c.baseChannel.VerifyPublishMessage(publish)
	if err != nil {
		return message.NewError("failed to verify publish message on an election channel", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	object := data.GetObject()

	log.Printf("Before handling the election object")

	if object == message.ElectionObject {

		action := message.ElectionAction(data.GetAction())
		switch action {
		case message.CastVoteAction:
			err = c.castVoteHelper(publish)
		case message.ElectionEndAction:
			err = c.endElectionHelper(publish)
			for client := range c.clients{
				result := message.Result{}
				general := 0
				result.General = &general
				log.Printf("Senfind the result for election end")
				client.SendResult(51,result)
			}
			err = c.electionResultHelper(publish)
			if err == nil{
				log.Printf("End and Result broadcasted")
				return nil
			}
		case message.ElectionResultAction:
			err = c.electionResultHelper(publish)
		default:
			return message.NewInvalidActionError(message.DataAction(action))
		}
	}

	if err != nil {
		action := message.ElectionAction(data.GetAction())
		errorDescription := fmt.Sprintf("failed to process %s action", action)
		return message.NewError(errorDescription, err)
	}

	log.Printf("Broadcasting to all clients on election channel")
	c.broadcastToAllClients(*msg)

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
	senderPK := base64.URLEncoding.EncodeToString(msg.Sender)

	log.Printf("The sender pk is %s",senderPK)

	senderPoint := student20_pop.Suite.Point()
	err := senderPoint.UnmarshalBinary(msg.Sender)
	if err != nil {
		return &message.Error{
			Code:        -4,
			Description: "Invalid sender public key",
		}
	}

	log.Printf("All the valid pks are %v and %v",c.attendees,senderPoint)

	ok = c.attendees.IsPresent(senderPK) || c.hub.public.Equal(senderPoint)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: "Only attendees can cast a vote in an election",
		}
	}

	//This should update any previously set vote if the message ids are the same
	c.inbox.storeMessage(*msg)
	for _, q := range voteData.Votes {

		QuestionID := base64.URLEncoding.EncodeToString(q.QuestionID)
		qs, ok := c.questions[QuestionID]

		if !ok {
			return &message.Error{
				Code:        -4,
				Description: "No Question with this ID exists",
			}
		}
		//this is to handle the case when the organizer must handle multiple votes being cast at the same time
		qs.validVotesMu.Lock()
		earlierVote, ok := qs.validVotes[msg.Sender.String()]
		// if the sender didn't previously cast a vote or if the vote is no longer valid update it

		if err := checkMethodProperties(qs.method, len(q.VoteIndexes)); err != nil {
			return err
		}
		if !ok {
			qs.validVotes[msg.Sender.String()] =
				validVote{voteData.CreatedAt,
					q.VoteIndexes}
			log.Printf("Sender votes for the following indexes %v",q.VoteIndexes)
		} else {
			changeVote(&qs, earlierVote, msg.Sender.String(), voteData.CreatedAt, q.VoteIndexes)
		}
		//other votes can now change the list of valid votes
		qs.validVotesMu.Unlock()
	}

	log.Printf("Vote casted with success")
	return nil
}
func checkMethodProperties(method message.VotingMethod, length int) error {

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

func changeVote(qs *question, earlierVote validVote, sender string, created message.Timestamp, indexes []int) {
	if earlierVote.voteTime > created {
		qs.validVotes[sender] =
			validVote{
				voteTime: created,
				indexes:  indexes,
			}
	}
}

func getAllQuestionsForElectionChannel(questions []message.Question) map[string]question {
	qs := make(map[string]question)
	for _, q := range questions {
		log.Printf("The ballot options setup in election setup are %v",q.BallotOptions)
		qs[base64.URLEncoding.EncodeToString(q.ID)] = question{
			id:            q.ID,
			ballotOptions: q.BallotOptions,
			validVotesMu:  sync.RWMutex{},
			validVotes:    make(map[string]validVote),
			method:        q.VotingMethod,
		}
	}
	return qs
}

func (c *electionChannel) endElectionHelper(publish message.Publish) error {
	endElectionData, ok := publish.Params.Message.Data.(*message.ElectionEndData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: "failed to cast data to ElectionEndData",
		}
	}
	if endElectionData.CreatedAt < c.end{
		return &message.Error{
			Code:        -4,
			Description: "Can't send end election message before the end of the election",
		}
	}
	if len(endElectionData.RegisteredVotes) == 0 {

	}else{
		// TODO: check if the hashing is done correctly
		// since we eliminated (in cast vote) the duplicate votes we are sure that the voter casted one vote for one question
		//for _,question := range c.questions{
		//	hashed,err := sortHashVotes(question.validVotes)
		//	if err != nil {
		//		return &message.Error{
		//			Code:        -4,
		//			Description: "Error while hashing",
		//		}
		//	}
		//	endElectionData.RegisteredVotes = hashed
		//}
	}

	log.Printf("Broadcasting election end message")
	msg := publish.Params.Message
	c.broadcastToAllClients(*msg)

	c.inbox.mutex.Lock()
	c.inbox.storeMessage(*msg)
	c.inbox.mutex.Unlock()

	return nil
}
func sortHashVotes(votes2 map[string]validVote)([]byte,error) {
	type kv struct {
		voteTime message.Timestamp
		sender   string
	}
	votes := make(map[int]kv)
	i := 0
	for k, v := range votes2 {
		votes[i] = kv{v.voteTime, k}
		i += 1
	}
	sort.Slice(votes,
		func(i int, j int) bool { return votes[i].voteTime < votes[j].voteTime })
	h := sha256.New()
	for _, v := range votes {
		if len(v.sender) == 0 {
			return nil, xerrors.Errorf("empty string to hash()")
		}
		h.Write([]byte(fmt.Sprintf("%d%s", len(v.sender), v.sender)))
	}
	return h.Sum(nil), nil
}

func (c *electionChannel) electionResultHelper(publish message.Publish) error{
	//msg := publish.Params.Message

	//resultData, ok := msg.Data.(*message.ElectionResultData)
	//if !ok {
	//	return &message.Error{
	//		Code:        -4,
	//		Description: "failed to cast data to ElectionResultData",
	//	}
	//}
	log.Printf("Computing election results on channel %v",c)

	msg := publish.Params.Message
	//c.broadcastToAllClients(*msg)

	genericMsg := message.GenericData{
		Action: "result",
		Object: "election",
	}

	resultData := message.ElectionResultData{
		GenericData:       &genericMsg,
		Questions:         make([]message.QuestionResult,0),
		WitnessSignatures: msg.WitnessSignatures,
	}


	log.Printf("Getting the count per ballot opetion for election results")
	//questions := resultData.Questions
	for id := range c.questions{
		// q.iD is the public key of the question, we convert it to string
		// to retrieve the votes for that question in the election channel
		question,ok := c.questions[id]
		if !ok{
			return &message.Error{
				Code:        -4,
				Description: "No question with this questionId was recorded",
			}
		}

		if len(question.id) == 0 {
			log.Printf("ignoring a question")
			break
		}

		votes  := question.validVotes
		if question.method == message.PluralityMethod {
			questionResults := make([]message.QuestionResult,0)
			numberOfVotesPerBallotOption := make([]int, len(question.ballotOptions))
			for _, vote := range votes {
				for _,ballotIndex := range vote.indexes {
					numberOfVotesPerBallotOption[ballotIndex] += 1
				}
			}

			// check if we even need questionResults
			//questionResults := make([]message.BallotOption,len(question.ballotOptions))
			questionResults2 := make([] message.BallotOptionCount,0)
			for i, option := range question.ballotOptions {
				if len(option) == 0{
					log.Printf("ignoring a ballot option")
					break
				}
				//questionResults = append(questionResults,message.BallotOption("ballot_option:") + option +
				//	message.BallotOption("count:" + string(numberOfVotesPerBallotOption[i])))
				log.Printf("For question of id %s we get an option of %v with count %v",question.id,option,numberOfVotesPerBallotOption[i])
				questionResults2 = append(questionResults2, message.BallotOptionCount{
					option,
					numberOfVotesPerBallotOption[i],
				})
			}
			log.Printf("The list of the ballot options and counts shoudl be the following: %v",questionResults)

			resultData.Questions = append(resultData.Questions,message.QuestionResult{
				ID : id,
				//Result: questionResults,
				Result2: questionResults2,
			})
			//resultData.Questions = questionResults

			log.Printf("Appending a question id:%s with the count and result",id)
		}
	}
	log.Printf("The result data field of the election result message " +
		"is the following %+v and has len of %v, first question is %v",resultData.Questions,len(resultData.Questions),resultData.Questions[0])

	log.Printf("computing message id for election result message")
	msgId := computeMessageId(resultData,msg.Signature)

	_,ok  := base64.URLEncoding.DecodeString(msgId)
	if ok != nil {
		return &message.Error{
			Code:        -4,
			Description: "Hash of the message id is not computed correctly",
		}
	}

	_ ,ok = json.Marshal(resultData)

	if ok != nil {
		return &message.Error{
			Code:        -4,
			Description: "failed to marshal election result data",
		}
	}

	log.Printf("creating the election result message")
	//ms2 := message.Message{
	//	MessageID:         id,
	//	Data:              resultData,
	//	Sender:            msg.Sender,
	//	Signature:         msg.Signature,
	//	WitnessSignatures: msg.WitnessSignatures,
	//	RawData:           raw,
	//}

	//for i, q := range resultData.Questions{
	//	if len(q.ID) == 0{
	//		log.Printf("removing a question")
	//		resultData.Questions = append(resultData.Questions[:i], resultData.Questions[i+1:]...)
	//	}
	//	for j, ballot := range q.Result2{
	//		if len(ballot.Option) < 1 {
	//			log.Printf("removing a ballot option")
	//			q.Result2 = append(q.Result2[:j],q.Result2[j+1:]...)
	//		}
	//	}
	//}

	log.Printf("The result data field of the election result message " +
		"is the following %v and has len of %v, first question is %v",resultData.Questions,len(resultData.Questions),resultData.Questions[0])

	ms3,ok := message.NewMessage(msg.Sender,msg.Signature,msg.WitnessSignatures,resultData)

	if ok != nil {
		return &message.Error{
			Code:        -4,
			Description: "failed to create an election result",
		}
	}

	log.Printf("broadcasting election resutl message")


	c.broadcastToAllClients(*ms3)
	//c.broadcastToAllClients(ms2)
	c.inbox.mutex.Lock()
	c.inbox.storeMessage(*ms3)
	c.inbox.mutex.Unlock()

	return nil
}

func computeMessageId(data message.ElectionResultData, signature message.Signature)string  {
	type messageId struct {
		data message.ElectionResultData
		sig  message.Signature
	}

	id := messageId{data: data, sig: signature}

	h := sha256.New()
	h.Write([]byte(fmt.Sprintf("%v", id)))

	return fmt.Sprintf("%x", h.Sum(nil))

}
