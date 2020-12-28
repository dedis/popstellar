package actors

import (
	"encoding/json"
	"log"
	"math/rand" //just to generate jsonRPC message's ID, no need for crypto rand
	"strconv"
	"student20_pop/db"
	"student20_pop/event"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
	"student20_pop/security"
)

// Organizer implements how the organizer's back end server must behave.
// It stores received messages in a database using the db package, composes and sends the appropriate response to the received message.
type Organizer struct {
	PublicKey string
	database  string
	channels  map[string][]int
}

// NewOrganizer is the constructor for the Organizer struct. db should be a a file path (existing or not) and pkey is
// the Organizer's public key.
func NewOrganizer(pkey string, db string) *Organizer {
	return &Organizer{
		PublicKey: pkey,
		database:  db,
		channels:  make(map[string][]int),
	}
}

// HandleReceivedMessage processes the received message. It parses it and calls sub-handler functions depending on
// 	the message's method field.
func (o *Organizer) HandleReceivedMessage(receivedMsg []byte, userId int) (msgAndChannel []lib.MessageAndChannel, responseToSender []byte) {
	// if the message is an answer message (positive ack or error), ignore it
	isAnswer, err := parser.FilterAnswers(receivedMsg)
	if err != nil {
		return nil, parser.ComposeResponse(lib.ErrIdNotDecoded, nil, message.Query{})
	}
	if isAnswer {
		return nil, nil
	}

	query, err := parser.ParseQuery(receivedMsg)
	if err != nil {
		return nil, parser.ComposeResponse(lib.ErrIdNotDecoded, nil, query)
	}

	var history []byte = nil
	var msg []lib.MessageAndChannel = nil

	switch query.Method {
	case "subscribe":
		msg, err = nil, o.handleSubscribe(query, userId)
	case "unsubscribe":
		msg, err = nil, o.handleUnsubscribe(query, userId)
	case "publish":
		msg, err = o.handlePublish(query)
	case "broadcast":
		msg, err = o.handleBroadcast(query)
	// Or they are only notification, and we just want to check that it was a success
	case "catchup":
		history, err = o.handleCatchup(query)
	default:
		log.Printf("method not recognized, generating default response")
		msg, err = nil, lib.ErrRequestDataInvalid
	}

	return msg, parser.ComposeResponse(err, history, query)
}

// handleBroadcast is the function to handle a received message which method was "broadcast"
// It is called by the function HandleReceivedMessage.
func (o *Organizer) handleBroadcast(query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	params, errs := parser.ParseParamsIncludingMessage(query.Params)
	if errs != nil {
		log.Printf("unable to analyse paramsLight in handleBroadcast()")
		return nil, lib.ErrRequestDataInvalid
	}

	msg, errs := parser.ParseMessage(params.Message)
	if errs != nil {
		log.Printf("unable to analyse Message in handleBroadcast()")
		return nil, lib.ErrRequestDataInvalid
	}

	data, errs := parser.ParseData(string(msg.Data))
	if errs != nil {
		log.Printf("unable to analyse data in handleBroadcast()")
		return nil, lib.ErrRequestDataInvalid
	}

	errs = db.CreateMessage(msg, params.Channel, o.database)
	if errs != nil {
		return nil, errs
	}

	switch data["object"] {
	case "message":
		switch data["action"] {
		case "witness":
			// TODO I would remove this case and force witnesses to send their "WitnessMessage" as a publish, and not a broadcasted, do you agree?
			// As per line 63, and as only org BEs should emit broadcasts, I'm actually leaning towards treating all broadcasted message to an org BE as either errors, or just checking for correct formatting then ignoring.
			return o.handleWitnessMessage(msg, params.Channel, query)
		default:
			return nil, lib.ErrRequestDataInvalid
		}
	case "state":
		{
			return o.handleLAOState(msg, params.Channel, query)
		}
	default:
		return nil, lib.ErrRequestDataInvalid
	}

}

// handlePublish is the function to handle a received message which method was "publish"
// It is called by the function HandleReceivedMessage. It Analyses the message's object and action fields, and delegate the
// work to other functions.
func (o *Organizer) handlePublish(query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	params, errs := parser.ParseParamsIncludingMessage(query.Params)
	if errs != nil {
		log.Printf("1. unable to analyse paramsLight in handlePublish()")
		return nil, lib.ErrRequestDataInvalid
	}

	msg, errs := parser.ParseMessage(params.Message)
	if errs != nil {
		log.Printf("2. unable to analyse Message in handlePublish()")
		return nil, lib.ErrRequestDataInvalid
	}

	errs = security.MessageIsValid(msg)
	if errs != nil {
		log.Printf("7")
		return nil, lib.ErrRequestDataInvalid
	}

	data, errs := parser.ParseData(string(msg.Data))
	if errs != nil {
		log.Printf("3. unable to analyse data in handlePublish()")
		return nil, lib.ErrRequestDataInvalid
	}

	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "create":
			return o.handleCreateLAO(msg, params.Channel, query)
		case "update_properties":
			return o.handleUpdateProperties(msg, params.Channel, query)
		case "state":
			return o.handleLAOState(msg, params.Channel, query) // should never happen
		default:
			return nil, lib.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			// TODO : send state broadcast if more signatures than threshold
			return o.handleWitnessMessage(msg, params.Channel, query)
			// TODO : state broadcast done on root
		default:
			return nil, lib.ErrInvalidAction
		}
	case "roll call":
		switch data["action"] {
		case "create":
			return o.handleCreateRollCall(msg, params.Channel, query)
		//case "state":  TODO : waiting on protocol definition
		default:
			return nil, lib.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			return o.handleCreateMeeting(msg, params.Channel, query)
		case "state": //
			// TODO: waiting on protocol definition
			return nil, lib.ErrNotYetImplemented
		default:
			return nil, lib.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create":
			return o.handleCreatePoll(msg, params.Channel, query)
		case "state":
			// TODO: waiting on protocol definition
			return nil, lib.ErrNotYetImplemented
		default:
			return nil, lib.ErrInvalidAction
		}
	default:
		log.Printf("data[action] (%v) not recognized in handlepublish, generating default response ", data["action"])
		return nil, lib.ErrRequestDataInvalid
	}
}

// handleCreateLAO is the function to handle a received message requesting a LAO Creation.
// It is called by the function handlePublish.
// The received message had the object field set to "lao" and action field to "create"
// It will check for the validity of the received message, store the received message in the database, and store the new
// LAO in the database.
func (o *Organizer) handleCreateLAO(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {

	if canal != "/root" {
		return nil, lib.ErrInvalidResource
	}

	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}

	if !security.LAOIsValid(data, true) {
		return nil, lib.ErrInvalidResource
	}

	errs = db.CreateMessage(msg, canal, o.database)
	if errs != nil {
		return nil, err
	}

	lao := event.LAO{
		ID:            string(data.ID),
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: string(data.Organizer),
		Witnesses:     lib.ArrayArrayByteToArrayString(data.Witnesses),
	}

	errs = db.CreateChannel(lao, o.database)
	if errs != nil {
		return nil, err
	}

	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}

	return msgAndChan, nil
}

// handleCreateRollCall is the function to handle a received message requesting a Roll Call Creation.
// It is called by the function handlePublish.
// The received message had the object field set to "roll call" and action field to "create"
// It will check for the validity of the received message, store the received message in the database, and store the new
// Roll Call in the database.
func (o *Organizer) handleCreateRollCall(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	if canal == "/root" {
		return nil, lib.ErrInvalidResource
	}

	data, errs := parser.ParseDataCreateRollCall(msg.Data)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}

	if !security.RollCallCreatedIsValid(data, msg) {
		return nil, errs
	}

	rollCall := event.RollCall{
		ID:       string(data.ID),
		Name:     data.Name,
		Creation: data.Creation,
		Location: data.Location,
		Start:    data.Start,
		End:      data.End,
		Extra:    data.Extra,
	}
	errs = db.CreateChannel(rollCall, o.database)
	if errs != nil {
		return nil, errs
	}

	errs = db.CreateMessage(msg, canal, o.database)
	if errs != nil {
		return nil, errs
	}

	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}

	return msgAndChan, nil
}

// handleCreateMeeting is the function to handle a received message requesting a meeting Creation.
// It is called by the function handlePublish.
// The received message had the object field set to "meeting" and action field to "create"
// It will check for the validity of the received message, store the received message in the database, and store the new
// meeting in the database.
func (o *Organizer) handleCreateMeeting(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {

	if canal == "/root" {
		return nil, lib.ErrInvalidResource
	}

	data, errs := parser.ParseDataCreateMeeting(msg.Data)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}

	meeting := event.Meeting{
		ID:       string(data.ID),
		Name:     data.Name,
		Creation: data.Creation,
		Location: data.Location,
		Start:    data.Start,
		End:      data.End,
		Extra:    data.Extra,
	}

	errs = db.CreateChannel(meeting, o.database)
	if errs != nil {
		return nil, errs
	}
	errs = db.CreateMessage(msg, canal, o.database)
	if errs != nil {
		return nil, errs
	}

	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}

	return msgAndChan, nil
}

// handleCreatePoll is the function to handle a received message requesting a poll Creation.
// It is called by the function handlePublish.
// The received message had the object field set to "poll" and action field to "create"
// It will check for the validity of the received message, store the received message in the database, and store the new
// poll in the database.
func (o *Organizer) handleCreatePoll(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {

	if canal == "/root" {
		return nil, lib.ErrInvalidResource
	}

	data, errs := parser.ParseDataCreatePoll(msg.Data)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}

	poll := event.Poll{
		ID:       string(data.ID),
		Name:     data.Name,
		Creation: data.Creation,
		Location: data.Location,
		Start:    data.Start,
		End:      data.End,
		Extra:    data.Extra,
	}

	errs = db.CreateChannel(poll, o.database)
	if errs != nil {
		return nil, err
	}

	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}

	return msgAndChan, nil
}

// handleUpdateProperties is the function to handle a received message requesting a change of some properties of a LAO.
// It is called by the function handlePublish.
// The received message had the object field set to "lao" and action field to "update_properties"
// It will store the received message in the database, and send the change request to every subscriber of this LAO,
// waiting for Witnesse's validation to make the update.
func (o *Organizer) handleUpdateProperties(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}
	return msgAndChan, db.CreateMessage(msg, canal, o.database)
}

// handleWitnessMessage is the function to handle a received message validating a previously received message.
// It retrieves the message that had to be signed, verifies the received signature and checks every received signature already received
// (as they are stored on the disk we have no guarantee they were not tempered with). If they are enough signatures,
// it should append to the list of returned message a reacting message (like a state broadcast for example). This is still to be
// implemented.
func (o *Organizer) handleWitnessMessage(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err_ error) {

	data, err := parser.ParseDataWitnessMessage(msg.Data)
	if err != nil {
		log.Printf("unable to parse received Message in handleWitnessMessage()")
		return nil, err
	}

	//retrieve message to sign from database
	toSign := db.GetMessage([]byte(canal), data.Message_id, o.database)
	if toSign == nil {
		return nil, lib.ErrInvalidResource
	}

	toSignStruct, err := parser.ParseMessage(toSign)
	if err != nil {
		log.Printf("unable to parse stored Message in handleWitnessMessage()")
		return nil, lib.ErrRequestDataInvalid
	}

	laoData, err := parser.ParseDataCreateLAO(toSignStruct.Data)
	if err != nil {
		log.Printf("unable to parse stored LAO infos in handleWitnessMessage()")
		return nil, err
	}

	err = security.VerifySignature(msg.Sender, toSignStruct.Data, data.Signature)
	if err != nil {
		return nil, err
	}

	//retrieves all the existing signatures for this message
	var signaturesOnly []string
	count := 0
	for i, item := range toSignStruct.WitnessSignatures {
		witnessSignature, errs := parser.ParseWitnessSignature(item)
		if errs != nil {
			log.Println("couldn't unMarshal the ItemWitnessSignatures from the DB")
			continue
		}
		err = security.VerifySignature(witnessSignature.WitnessKey, toSignStruct.Data, witnessSignature.Signature)
		if err != nil {
			count--
			log.Printf("Invalid signature found in signature lists, with index %d", i)
		}
		count++
		signaturesOnly = append(signaturesOnly, string(witnessSignature.Signature))
	}
	//if new signature already exists, returns error
	_, found := lib.FindStr(signaturesOnly, string(data.Signature))
	if found {
		return nil, lib.ErrResourceAlreadyExists
	}

	iws, err := json.Marshal(message.ItemWitnessSignatures{WitnessKey: msg.Sender, Signature: data.Signature})
	if err != nil {
		log.Println("couldn't Marshal the ItemWitnessSignatures")
	}
	toSignStruct.WitnessSignatures = append(toSignStruct.WitnessSignatures, iws)

	// update "LAOUpdateProperties" message in DB
	err = db.UpdateMessage(toSignStruct, canal, o.database)
	if err != nil {
		return nil, lib.ErrDBFault
	}
	//store received message in DB
	err = db.CreateMessage(msg, canal, o.database)
	if err != nil {
		return nil, lib.ErrDBFault
	}

	//broadcast received message
	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}

	//TODO switch on event type. Think of clever code
	if count == SIG_THRESHOLD-1 {
		lao := event.LAO{
			ID:            string(laoData.ID),
			Name:          laoData.Name,
			Creation:      laoData.Creation,
			OrganizerPKey: string(laoData.Organizer),
			Witnesses:     lib.ArrayArrayByteToArrayString(laoData.Witnesses),
		}

		err = db.CreateChannel(lao, o.database)
		if err != nil {
			return nil, err
		}
		//compose state update message
		state := message.DataStateLAO{
			Object:        "lao",
			Action:        "state",
			ID:            []byte(lao.ID),
			Name:          lao.Name,
			Creation:      lao.Creation,
			Last_modified: lao.Creation,
			Organizer:     []byte(lao.OrganizerPKey),
			Witnesses:     laoData.Witnesses,
		}

		stateStr, errs := json.Marshal(state)
		if errs != nil {
			return nil, errs
		}

		content := message.Message{
			Data:              stateStr,
			Sender:            []byte(o.PublicKey),
			Signature:         nil, //TODO should implement a function to sign the message's content
			MessageId:         []byte(strconv.Itoa(rand.Int())),
			WitnessSignatures: nil,
		}

		contentStr, errs := json.Marshal(content)
		if errs != nil {
			return nil, errs
		}

		sendParams := message.ParamsIncludingMessage{
			Channel: "/root",
			Message: contentStr,
		}

		paramsStr, errs := json.Marshal(sendParams)
		if errs != nil {
			return nil, errs
		}

		sendQuery := message.Query{
			Jsonrpc: "2.0",
			Method:  "message",
			Params:  paramsStr,
			Id:      rand.Int(),
		}

		queryStr, errs := json.Marshal(sendQuery)
		if errs != nil {
			return nil, errs
		}
		msgAndChan = append(msgAndChan, lib.MessageAndChannel{Channel: []byte(canal), Message: queryStr})
	}

	return msgAndChan, nil
}

// handleCatchup is the function to handle a received message requesting a catchup on a channel.
// It is called by HandleReceivedMessage, and returns the current state of a channel.
func (o *Organizer) handleCatchup(query message.Query) ([]byte, error) {
	// TODO maybe pass userId as an arg in order to check access rights later on?
	params, err := parser.ParseParams(query.Params)
	if err != nil {
		log.Printf("unable to analyse paramsLight in handleCatchup()")
		return nil, lib.ErrRequestDataInvalid
	}
	history := db.GetChannel([]byte(params.Channel), o.database)

	return history, nil
}

//handleLAOState is just here to implement the Actor interface. It returns an error as, in the current implementation there
// is only one Organizer, and he's the one sending this message. Hence he should not be receiving it.
func (o *Organizer) handleLAOState(msg message.Message, chann string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	return nil, lib.ErrInvalidAction
}
