package hub

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"path/filepath"
	"student20_pop"
	"sync"

	"student20_pop/message"

	"github.com/xeipuuv/gojsonschema"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

type organizerHub struct {
	messageChan chan IncomingMessage

	sync.RWMutex
	channelByID map[string]Channel

	public kyber.Point

	schemas map[string]*gojsonschema.Schema
}

const (
	GenericMsgSchema string = "genericMsgSchema"
	DataSchema       string = "dataSchema"
)

// NewOrganizerHub returns a Organizer Hub.
func NewOrganizerHub(public kyber.Point) (Hub, error) {
	// Import the Json schemas defined in the protocol section
	protocolPath, err := filepath.Abs("../protocol")
	if err != nil {
		return nil, xerrors.Errorf("failed to load the path for the json schemas: %v", err)
	}
	protocolPath = "file://" + protocolPath

	schemas := make(map[string]*gojsonschema.Schema)

	// Import the schema for generic messages
	genericMsgLoader := gojsonschema.NewReferenceLoader(protocolPath + "/genericMessage.json")
	genericMsgSchema, err := gojsonschema.NewSchema(genericMsgLoader)
	if err != nil {
		return nil, xerrors.Errorf("failed to load the json schema for generic messages: %v", err)
	}
	schemas[GenericMsgSchema] = genericMsgSchema

	// Impot the schema for data
	dataSchemaLoader := gojsonschema.NewReferenceLoader(protocolPath + "/query/method/message/data/data.json")
	dataSchema, err := gojsonschema.NewSchema(dataSchemaLoader)
	if err != nil {
		return nil, xerrors.Errorf("failed to load the json schema for data: %v", err)
	}
	schemas[DataSchema] = dataSchema

	// Create the organizer hub
	return &organizerHub{
		messageChan: make(chan IncomingMessage),
		channelByID: make(map[string]Channel),
		public:      public,
		schemas:     schemas,
	}, nil
}

// RemoveClient removes the client from this hub.
func (o *organizerHub) RemoveClientSocket(client *ClientSocket) {
	o.RLock()
	defer o.RUnlock()

	for _, channel := range o.channelByID {
		channel.Unsubscribe(client, message.Unsubscribe{})
	}
}

// Recv accepts a message and enques it for processing in the hub.
func (o *organizerHub) Recv(msg IncomingMessage) {
	log.Printf("organizerHub::Recv")
	o.messageChan <- msg
}

func (o *organizerHub) handleMessageFromClient(incomingMessage *IncomingMessage) {
	client := ClientSocket{
		incomingMessage.Socket,
	}
	byteMessage := incomingMessage.Message

	// Check if the GenericMessage has a field "id"
	genericMsg := &message.GenericMessage{}
	id, ok := genericMsg.UnmarshalID(byteMessage)
	if !ok {
		log.Printf("The message does not have a valid `id` field")
		client.SendError(nil, &message.Error{
			Code:        -1,
			Description: "The message does not have a valid `id` field",
		})
		return
	}

	// Verify the message
	err := o.verifyJson(byteMessage, GenericMsgSchema)
	if err != nil {
		log.Printf("failed to verify incoming message: %v", err)
		client.SendError(&id, err)
		return
	}

	// Unmarshal the message
	err = json.Unmarshal(byteMessage, genericMsg)
	if err != nil {
		log.Printf("failed to unmarshal incoming message: %v", err)
		client.SendError(&id, err)
		return
	}

	query := genericMsg.Query

	if query == nil {
		return
	}

	channelID := query.GetChannel()
	log.Printf("channel: %s", channelID)

	if channelID == "/root" {
		if query.Publish == nil {
			log.Printf("only publish is allowed on /root")
			client.SendError(&id, err)
			return
		}

		// Check if the structure of the message is correct
		msg := query.Publish.Params.Message

		// Verify the data
		err := o.verifyJson(msg.RawData, DataSchema)
		if err != nil {
			log.Printf("failed to validate the data: %v", err)
			client.SendError(&id, err)
			return
		}

		// Unmarshal the data
		err = query.Publish.Params.Message.VerifyAndUnmarshalData()
		if err != nil {
			log.Printf("failed to verify and unmarshal data: %v", err)
			client.SendError(&id, err)
			return
		}

		if query.Publish.Params.Message.Data.GetAction() == message.DataAction(message.CreateLaoAction) &&
			query.Publish.Params.Message.Data.GetObject() == message.DataObject(message.LaoObject) {
			err := o.createLao(*query.Publish)
			if err != nil {
				log.Printf("failed to create lao: %v", err)
				client.SendError(&id, err)
				return
			}
		} else {
			log.Printf("invalid method: %s", query.GetMethod())
			client.SendError(&id, &message.Error{
				Code:        -1,
				Description: "you may only invoke lao/create on /root",
			})
			return
		}

		status := 0
		result := message.Result{General: &status}
		log.Printf("sending result: %+v", result)
		client.SendResult(id, result)
		return
	}

	if channelID[:6] != "/root/" {
		log.Printf("channel id must begin with /root/")
		client.SendError(&id, &message.Error{
			Code:        -2,
			Description: "channel id must begin with /root/",
		})
		return
	}

	channelID = channelID[6:]
	o.RLock()
	channel, ok := o.channelByID[channelID]
	if !ok {
		log.Printf("invalid channel id: %s", channelID)
		client.SendError(&id, &message.Error{
			Code:        -2,
			Description: fmt.Sprintf("channel with id %s does not exist", channelID),
		})
		return
	}
	o.RUnlock()

	method := query.GetMethod()
	log.Printf("method: %s", method)

	msg := []message.Message{}

	// TODO: use constants
	switch method {
	case "subscribe":
		err = channel.Subscribe(&client, *query.Subscribe)
	case "unsubscribe":
		err = channel.Unsubscribe(&client, *query.Unsubscribe)
	case "publish":
		err = channel.Publish(*query.Publish)
	case "message":
		log.Printf("cannot handle broadcasts right now")
	case "catchup":
		msg = channel.Catchup(*query.Catchup)
		// TODO send catchup response to client
	}

	if err != nil {
		log.Printf("failed to process query: %v", err)
		client.SendError(&id, err)
		return
	}

	result := message.Result{}

	if method == "catchup" {
		result.Catchup = msg
	} else {
		general := 0
		result.General = &general
	}

	client.SendResult(id, result)
}

func (o *organizerHub) handleMessageFromWitness(incomingMessage *IncomingMessage) {
	//TODO

}

func (o *organizerHub) handleIncomingMessage(incomingMessage *IncomingMessage) {
	log.Printf("organizerHub::handleMessageFromClient: %s", incomingMessage.Message)

	switch incomingMessage.Socket.socketType {
	case ClientSocketType:
		o.handleMessageFromClient(incomingMessage)
		return
	case WitnessSocketType:
		o.handleMessageFromWitness(incomingMessage)
		return
	default:
		log.Printf("error: invalid socket type")
		return
	}

}

func (o *organizerHub) Start(done chan struct{}) {
	log.Printf("started organizer hub...")

	for {
		select {
		case incomingMessage := <-o.messageChan:
			o.handleIncomingMessage(&incomingMessage)
		case <-done:
			return
		}
	}
}

func (o *organizerHub) verifyJson(byteMessage []byte, schemaName string) error {
	// Validate the Json "byteMessage" with a schema
	messageLoader := gojsonschema.NewBytesLoader(byteMessage)
	resultErrors, err := o.schemas[schemaName].Validate(messageLoader)
	if err != nil {
		return &message.Error{
			Code:        -1,
			Description: err.Error(),
		}
	}
	errorsList := resultErrors.Errors()
	descriptionErrors := ""
	// Concatenate all error descriptions
	for index, e := range errorsList {
		descriptionErrors += fmt.Sprintf(" (%d) %s", index+1, e.Description())
	}

	if len(errorsList) > 0 {
		return &message.Error{
			Code:        -1,
			Description: descriptionErrors,
		}
	}

	return nil
}

func (o *organizerHub) createLao(publish message.Publish) error {
	o.Lock()
	defer o.Unlock()

	data, ok := publish.Params.Message.Data.(*message.CreateLAOData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: "failed to cast data to CreateLAOData",
		}
	}

	encodedID := base64.StdEncoding.EncodeToString(data.ID)
	if _, ok := o.channelByID[encodedID]; ok {
		return &message.Error{
			Code:        -3,
			Description: "failed to create lao: another one with the same ID exists",
		}
	}

	if _, ok := o.channelByID[encodedID]; ok {
		return &message.Error{
			Code:        -3,
			Description: "failed to create lao: another one with the same ID exists",
		}
	}
	laoChannelID := "/root/" + encodedID

	laoCh := laoChannel{
		rollCall:    rollCall{},
		attendees:   make(map[string]struct{}),
		baseChannel: createBaseChannel(o, laoChannelID),
	}
	messageID := base64.StdEncoding.EncodeToString(publish.Params.Message.MessageID)
	laoCh.inbox[messageID] = *publish.Params.Message

	o.channelByID[encodedID] = &laoCh

	return nil
}

type laoChannel struct {
	rollCall  rollCall
	attendees map[string]struct{}
	*baseChannel
}

type rollCallState string

const (
	Open    rollCallState = "open"
	Closed  rollCallState = "closed"
	Created rollCallState = "created"
)

type rollCall struct {
	state rollCallState
	id    string
}

func (c *laoChannel) Publish(publish message.Publish) error {
	err := c.baseChannel.VerifyPublishMessage(publish)
	if err != nil {
		return message.NewError("failed to verify Publish message on a lao channel", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	object := data.GetObject()

	switch object {
	case message.LaoObject:
		err = c.processLaoObject(*msg)
	case message.MeetingObject:
		err = c.processMeetingObject(data)
	case message.MessageObject:
		err = c.processMessageObject(msg.Sender, data)
	case message.RollCallObject:
		err = c.processRollCallObject(*msg)
	case message.ElectionObject:
		err = c.processElectionObject(*msg)
	}

	if err != nil {
		log.Printf("failed to process %s object: %v", object, err)
		return xerrors.Errorf("failed to process %s object: %v", object, err)
	}

	c.broadcastToAllClients(*msg)
	return nil
}

func (c *laoChannel) processLaoObject(msg message.Message) error {
	action := message.LaoDataAction(msg.Data.GetAction())
	msgIDEncoded := base64.StdEncoding.EncodeToString(msg.MessageID)

	switch action {
	case message.UpdateLaoAction:
	case message.StateLaoAction:
		err := c.processLaoState(msg.Data.(*message.StateLAOData))
		if err != nil {
			log.Printf("failed to process lao/state: %v", err)
			return xerrors.Errorf("failed to process lao/state: %v", err)
		}
	default:
		return message.NewInvalidActionError(message.DataAction(action))
	}

	c.inboxMu.Lock()
	c.inbox[msgIDEncoded] = msg
	c.inboxMu.Unlock()

	return nil
}

func (c *laoChannel) processLaoState(data *message.StateLAOData) error {
	// Check if we have the update message
	updateMsgIDEncoded := base64.StdEncoding.EncodeToString(data.ModificationID)

	c.inboxMu.RLock()
	updateMsg, ok := c.inbox[updateMsgIDEncoded]
	c.inboxMu.RUnlock()

	if !ok {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("cannot find lao/update_properties with ID: %s", updateMsgIDEncoded),
		}
	}

	// Check if the signatures are from witnesses we need. We maintain
	// the current state of witnesses for a LAO in the channel instance
	// TODO: threshold signature verification

	c.witnessMu.Lock()
	match := 0
	expected := len(c.witnesses)
	// TODO: O(N^2), O(N) possible
	for i := 0; i < expected; i++ {
		for j := 0; j < len(data.ModificationSignatures); j++ {
			if bytes.Equal(c.witnesses[i], data.ModificationSignatures[j].Witness) {
				match++
				break
			}
		}
	}
	c.witnessMu.Unlock()

	if match != expected {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("not enough witness signatures provided. Needed %d got %d", expected, match),
		}
	}

	// Check if the signatures match
	for _, pair := range data.ModificationSignatures {
		err := schnorr.VerifyWithChecks(student20_pop.Suite, pair.Witness, data.ModificationID, pair.Signature)
		if err != nil {
			pk := base64.StdEncoding.EncodeToString(pair.Witness)
			return &message.Error{
				Code:        -4,
				Description: fmt.Sprintf("signature verification failed for witness %s", pk),
			}
		}
	}

	// Check if the updates are consistent with the update message
	updateMsgData, ok := updateMsg.Data.(*message.UpdateLAOData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("modification id %s refers to a message which is not lao/update_properties", updateMsgIDEncoded),
		}
	}

	err := compareLaoUpdateAndState(updateMsgData, data)
	if err != nil {
		return xerrors.Errorf("failure while comparing lao/update and lao/state")
	}

	return nil
}

func compareLaoUpdateAndState(update *message.UpdateLAOData, state *message.StateLAOData) error {
	if update.LastModified != state.LastModified {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between last modified: expected %d got %d", update.LastModified, state.LastModified),
		}
	}

	if update.Name != state.Name {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between name: expected %d got %d", update.LastModified, state.LastModified),
		}
	}

	if update.Name != state.Name {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between name: expected %d got %d", update.LastModified, state.LastModified),
		}
	}

	M := len(update.Witnesses)
	N := len(state.Witnesses)

	if M != N {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between witness count: expected %d got %d", M, N),
		}
	}

	match := 0

	for i := 0; i < M; i++ {
		for j := 0; j < N; j++ {
			if bytes.Equal(update.Witnesses[i], state.Witnesses[j]) {
				match++
				break
			}
		}
	}

	if match != M {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between witness keys: expected %d keys to match but %d matched", M, match),
		}
	}

	return nil
}

func (c *laoChannel) processMeetingObject(data message.Data) error {
	action := message.MeetingDataAction(data.GetAction())

	switch action {
	case message.CreateMeetingAction:
	case message.UpdateMeetingAction:
	case message.StateMeetingAction:
	}

	return nil
}

func (c *laoChannel) processMessageObject(public message.PublicKey, data message.Data) error {
	action := message.MessageDataAction(data.GetAction())

	switch action {
	case message.WitnessAction:
		witnessData := data.(*message.WitnessMessageData)

		msgEncoded := base64.StdEncoding.EncodeToString(witnessData.MessageID)

		err := schnorr.VerifyWithChecks(student20_pop.Suite, public, witnessData.MessageID, witnessData.Signature)
		if err != nil {
			return &message.Error{
				Code:        -4,
				Description: "invalid witness signature",
			}
		}

		c.inboxMu.Lock()
		msg, ok := c.inbox[msgEncoded]
		if !ok {
			// TODO: We received a witness signature before the message itself.
			// We ignore it for now but it might be worth keeping it until we
			// actually receive the message
			log.Printf("failed to find message_id %s for witness message", msgEncoded)
			c.inboxMu.Unlock()
			return nil
		}
		msg.WitnessSignatures = append(msg.WitnessSignatures, message.PublicKeySignaturePair{
			Witness:   public,
			Signature: witnessData.Signature,
		})
		c.inboxMu.Unlock()
	default:
		return message.NewInvalidActionError(message.DataAction(action))
	}

	return nil
}

func (c *laoChannel) processRollCallObject(msg message.Message) error {
	sender := msg.Sender
	data := msg.Data

	// Check if the sender of the roll call message is the organizer
	senderPoint := student20_pop.Suite.Point()
	err := senderPoint.UnmarshalBinary(sender)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal public key of the sender: %v", err)
	}

	if !c.hub.public.Equal(senderPoint) {
		return &message.Error{
			Code:        -5,
			Description: "The sender of the roll call message has a different public key from the organizer",
		}
	}

	action := message.RollCallAction(data.GetAction())

	switch action {
	case message.CreateRollCallAction:
		err = c.processCreateRollCall(data)
	case message.RollCallAction(message.OpenRollCallAction), message.RollCallAction(message.ReopenRollCallAction):
		err = c.processOpenRollCall(data, action)
	case message.CloseRollCallAction:
		err = c.processCloseRollCall(data)
	default:
		return message.NewInvalidActionError(message.DataAction(action))
	}

	if err != nil {
		return xerrors.Errorf("failed to process %v roll-call action: %v", action, err)
	}

	msgIDEncoded := base64.StdEncoding.EncodeToString(msg.MessageID)
	c.inboxMu.Lock()
	c.inbox[msgIDEncoded] = msg
	c.inboxMu.Unlock()

	return nil
}

func (c *laoChannel) processCreateRollCall(data message.Data) error {
	rollCallData := data.(*message.CreateRollCallData)
	if !c.checkRollCallID(rollCallData.Creation, message.Stringer(rollCallData.Name), rollCallData.ID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||creation||name)",
		}
	}

	c.rollCall.id = string(rollCallData.ID)
	c.rollCall.state = Created
	return nil
}

func (c *laoChannel) processOpenRollCall(data message.Data, action message.RollCallAction) error {
	if action == message.RollCallAction(message.OpenRollCallAction) {
		// If the action is an OpenRollCallAction,
		// the previous roll call action should be a CreateRollCallAction
		if c.rollCall.state != Created {
			return &message.Error{
				Code:        -1,
				Description: "The roll call can not be opened since it has not been created previously",
			}
		}
	} else {
		// If the action is an RepenRollCallAction,
		// the previous roll call action should be a CloseRollCallAction
		if c.rollCall.state != Closed {
			return &message.Error{
				Code:        -1,
				Description: "The roll call can not be reopened since it has not been closed previously",
			}
		}
	}

	rollCallData := data.(*message.OpenRollCallData)

	if !c.rollCall.checkPrevID(rollCallData.Opens) {
		return &message.Error{
			Code:        -4,
			Description: "The field `opens` does not correspond to the id of the previous roll call message",
		}
	}

	opens := base64.StdEncoding.EncodeToString(rollCallData.Opens)
	if !c.checkRollCallID(message.Stringer(opens), rollCallData.OpenedAt, rollCallData.UpdateID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||opens||opened_at)",
		}
	}

	c.rollCall.id = string(rollCallData.UpdateID)
	c.rollCall.state = Open
	return nil
}

func (c *laoChannel) processCloseRollCall(data message.Data) error {
	if c.rollCall.state != Open {
		return &message.Error{
			Code:        -1,
			Description: "The roll call can not be closed since it is not open",
		}
	}

	rollCallData := data.(*message.CloseRollCallData)
	if !c.rollCall.checkPrevID(rollCallData.Closes) {
		return &message.Error{
			Code:        -4,
			Description: "The field `closes` does not correspond to the id of the previous roll call message",
		}
	}

	closes := base64.StdEncoding.EncodeToString(rollCallData.Closes)
	if !c.checkRollCallID(message.Stringer(closes), rollCallData.ClosedAt, rollCallData.UpdateID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||closes||closed_at)",
		}
	}

	c.rollCall.id = string(rollCallData.UpdateID)
	c.rollCall.state = Closed
	c.attendees = map[string]struct{}{}
	for i := 0; i < len(rollCallData.Attendees); i += 1 {
		c.attendees[string(rollCallData.Attendees[i])] = struct{}{}
	}

	return nil
}

func (c *laoChannel) processElectionObject(msg message.Message) error {
	action := message.ElectionAction(msg.Data.GetAction())

	if action != message.ElectionSetupAction {
		return &message.Error{
			Code:        -1,
			Description: fmt.Sprintf("invalid action: %s", action),
		}
	}

	err := c.createElection(msg)
	if err != nil {
		return xerrors.Errorf("failed to setup the election %v", err)
	}

	return nil
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
		createBaseChannel(organizerHub, "/root/"+encodedID),
		data.StartTime,
		data.EndTime,
		false,
		getAllQuestionsForElectionChannel(data.Questions, data),
	}

	// Add the SetupElection message to the new election channel
	messageID := base64.URLEncoding.EncodeToString(msg.MessageID)
	c.inboxMu.Lock()
	electionCh.inbox[messageID] = msg
	electionCh.inboxMu.Unlock()

	// Add the new election channel to the organizerHub
	organizerHub.channelByID[encodedID] = &electionCh

	return nil
}

func getAllQuestionsForElectionChannel(questions []message.Question, data *message.ElectionSetupData) map[string]question {
	qs := make(map[string]question)
	for _, q := range questions {
		qs[base64.URLEncoding.EncodeToString(q.ID)] = question{
			q.ID,
			q.BallotOptions,
			sync.RWMutex{},
			make(map[string]validVote),
			q.VotingMethod,
		}
	}
	return qs
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
			qs.validVotesMu.Lock()
			earlierVote, ok := qs.validVotes[msg.Sender.String()]
			// if the sender didn't previously cast a vote or if the vote is no longer valid update it
			if !ok {
				qs.validVotes[msg.Sender.String()] =
					validVote{voteData.CreatedAt,
						q.VoteIndexes}
				if qs.method == "Plurality" && len(q.VoteIndexes) < 1 {
					return &message.Error{
						Code:        -4,
						Description: "No ballot option was chosen for plurality voting method",
					}
				}
				if qs.method == "Approval" && len(q.VoteIndexes) != 1 {
					return &message.Error{
						Code:        -4,
						Description: "Cannot choose multiple ballot options on Approval voting method",
					}
				}
			} else {
				if earlierVote.voteTime > voteData.CreatedAt {
					qs.validVotes[msg.Sender.String()] =
						validVote{voteData.CreatedAt,
							q.VoteIndexes}
				}
			}
			//other votes can now change the list of valid votes
			qs.validVotesMu.Unlock()
		} else {
			return &message.Error{
				Code:        -4,
				Description: "No Question with this ID exists",
			}
		}
	}
	return &message.Error{
		Code:        -4,
		Description: "Error in CastVote helper function",
	}
}

func (r *rollCall) checkPrevID(prevID [] byte) bool {
	return string(prevID) == r.id
}

// Check if the id of the roll call corresponds to the hash of the correct parameters
// Return true if the hash corresponds to the id and false otherwise
func (c *laoChannel) checkRollCallID(str1, str2 fmt.Stringer, id []byte) bool {
	laoID := c.channelID[6:]
	hash, err := message.Hash(message.Stringer('R'), message.Stringer(laoID), str1, str2)
	if err != nil {
		return false
	}

	return bytes.Equal(hash, id)
}
