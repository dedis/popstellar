package actors

import (
	"encoding/json"
	"log"
	"strings"
	"student20_pop/db"
	"student20_pop/event"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
	"student20_pop/security"
)

// organizer implements how the organizer's back end server must behave.
// It stores received messages in a database using the db package, composes and sends the appropriate response to the sender
// as well as broadcast messages if available. The database field is only the name of the file the database is stored to.
type organizer struct {
	PublicKey string
	database  string
	channels  map[string][]int
}

// NewOrganizer is the constructor for the organizer struct. db should be a a file path (existing or not) and pkey is
// the organizer's public key.
func NewOrganizer(pkey string, db string) *organizer {
	return &organizer{
		PublicKey: pkey,
		database:  db,
		channels:  make(map[string][]int),
	}
}

// HandleReceivedMessage processes the received message. It parses it and calls sub-handler functions depending on
// 	the message's method field.
func (o *organizer) HandleReceivedMessage(receivedMsg []byte, userId int) (msgAndChannel []lib.MessageAndChannel, responseToSender []byte) {
	// if the message is an answer message (positive ack or error), ignore it
	isAnswer, err := parser.FilterAnswers(receivedMsg)
	if err != nil {
		log.Printf("could not parse the received message into a message.GenericMessage structure")
		return nil, parser.ComposeResponse(lib.ErrIdNotDecoded, nil, message.Query{})
	}
	if isAnswer {
		return nil, nil
	}

	query, err := parser.ParseQuery(receivedMsg)
	if err != nil {
		log.Printf("could not parse the received message into a message.Query structure")
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
		// Should currently return an error as organizer should not be receiving broadcast messages in the current implementation
		msg, err = o.handleBroadcast(query)
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
// It returns an error as in the current implementation there is only one organizer. So there is only one actor sending
// broadcast message. Hence he should not be receiving this message.
func (o *organizer) handleBroadcast(query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	// In the current specification, only organizer BEs should emit broadcasts, so currently,
	// incoming broadcast messages are considered erroneous with requestDataInvalid
	log.Printf("received a broadcast message on an organizer back-end")
	return nil, lib.ErrRequestDataInvalid

	/*
		Legacy code which might be helpful for future protocol modifications?
		params, errs := parser.ParseParams(query.Params)
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
		}*/

}

// handlePublish is the function to handle a received message which method was "publish"
// It is called by the function HandleReceivedMessage. It Analyses the message's object and action fields, and delegate
// the execution to sub-handlers depending on these fields.
func (o *organizer) handlePublish(query message.Query) (msgAndChannel []lib.MessageAndChannel, err_ error) {
	params, errs := parser.ParseParams(query.Params)
	if errs != nil {
		log.Printf("unable to analyse params in handlePublish()")
		return nil, lib.ErrRequestDataInvalid
	}

	msg, errs := parser.ParseMessage(params.Message)
	if errs != nil {
		log.Printf("unable to analyse Message in handlePublish()")
		return nil, lib.ErrRequestDataInvalid
	}

	errs = security.MessageIsValid(msg)
	if errs != nil {
		log.Printf("message is not valid")
		return nil, lib.ErrRequestDataInvalid
	}

	data, errs := parser.ParseData(string(msg.Data))
	if errs != nil {
		log.Printf("unable to analyse data in handlePublish()")
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
			// should never happen
			return o.handleLAOState(msg)
		default:
			return nil, lib.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			return o.handleWitnessMessage(msg, params.Channel, query)
		default:
			return nil, lib.ErrInvalidAction
		}
	case "roll_call":
		switch data["action"] {
		case "create":
			return o.handleCreateRollCall(msg, params.Channel, query)
		case "open", "reopen":
			return o.handleOpenRollCall(msg, params.Channel, query)
		case "close":
			return o.handleCloseRollCall(msg, params.Channel, query)
		default:
			return nil, lib.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			return o.handleCreateMeeting(msg, params.Channel, query)
		case "state":
			// should never happen as we are the only organizer right now
			return o.handleMeetingState(msg)
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
		log.Printf("data[object] (%v) not recognized in handlepublish, generating default response ", data["object"])
		return nil, lib.ErrRequestDataInvalid
	}
}

// handleCreateLAO is the function to handle a received message requesting a LAO Creation.
// It is called by the function handlePublish.
// The received message had the object field set to "lao" and action field to "create"
// It will check for the validity of the received message, store the received message in the database, and store the new
// LAO in the database.
func (o *organizer) handleCreateLAO(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err_ error) {

	if canal != "/root" {
		log.Printf("Invalid channel. LAO create requests are valid only on the /root channel")
		return nil, lib.ErrInvalidResource
	}

	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		log.Printf("Could not parse received data into a create LAO structure")
		return nil, lib.ErrInvalidResource
	}

	if !security.LAOIsValid(data, true) {
		log.Printf("Received data for LAO Creation not valid")
		return nil, lib.ErrInvalidResource
	}

	lao := event.LAO{
		ID:            string(data.ID),
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: string(data.Organizer),
		Witnesses:     lib.NestedByteArrayToStringArray(data.Witnesses),
	}

	errs = db.CreateChannel(lao, o.database)
	if errs != nil {
		log.Printf("An error occured, could not create new channel in the database")
		return nil, errs
	}

	errs = db.CreateMessage(msg, canal, o.database)
	if errs != nil {
		log.Printf("An error occured, could not store message to the database")
		return nil, errs
	}

	log.Printf("Sucessfully created lao %s", lao.Name)

	return nil, nil
}

// handleCreateRollCall is the function to handle a received message requesting a Roll Call Creation.
// It is called by the function handlePublish.
// The received message had the object field set to "roll_call" and action field to "create"
// It will check for the validity of the received message, store the received message in the database, and store the new
// Roll Call in the database.
func (o *organizer) handleCreateRollCall(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	if !strings.HasPrefix(canal, "/root/") {
		log.Printf("invalid channel name for roll call creation: %s", canal)
		return nil, lib.ErrInvalidResource
	}

	data, err := parser.ParseDataCreateRollCall(msg.Data)
	if err != nil {
		log.Printf("could not parse received message data into a message.DataCreateRollCall strcuture")
		return nil, lib.ErrInvalidResource
	}
	laoID := strings.TrimPrefix(canal, "/root/")
	if !security.RollCallCreatedIsValid(data, laoID) {
		log.Printf("data for roll call creation invalid")
		return nil, err
	}

	rollCall := event.RollCall{
		ID:          string(data.ID),
		Name:        data.Name,
		Creation:    data.Creation,
		Location:    data.Location,
		Start:       data.Start,
		Scheduled:   data.Scheduled,
		Description: data.Description,
	}

	err = db.CreateChannel(rollCall, o.database)
	if err != nil {
		log.Printf("An error occured, unable to create new channel in the database")
		return nil, err
	}

	err = db.CreateMessage(msg, canal, o.database)
	if err != nil {
		log.Printf("An error occured, unable to store received message in the database")
		return nil, err
	}

	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}

	log.Printf("Sucessfully created roll call %s", rollCall.Name)

	return msgAndChan, nil
}

// handleCreateMeeting is the function to handle a received message requesting a meeting Creation.
// It is called by the function handlePublish.
// The received message had the object field set to "meeting" and action field to "create"
// It will check for the validity of the received message, store the received message in the database, and store the new
// meeting in the database.
func (o *organizer) handleCreateMeeting(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {

	if !strings.HasPrefix(canal, "/root/") {
		log.Printf("invalid channel name for meeting creation: %s", canal)
		return nil, lib.ErrInvalidResource
	}

	data, err := parser.ParseDataCreateMeeting(msg.Data)
	if err != nil {
		log.Printf("unable to parse received message data into a message.DataCreateMeeting structure")
		return nil, lib.ErrInvalidResource
	}

	//we provide the id of the channel
	laoId := strings.TrimPrefix(canal, "/root/")
	if !security.MeetingCreatedIsValid(data, laoId) {
		log.Printf("Meeting data invalid. Meeting not created")
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

	err = db.CreateChannel(meeting, o.database)
	if err != nil {
		log.Printf("An error occured, unable to create channel in the database")
		return nil, err
	}
	err = db.CreateMessage(msg, canal, o.database)
	if err != nil {
		log.Printf("An error occured, unable to store received message in the database")
		return nil, err
	}

	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}

	log.Printf("Sucessfully created meeting %s", meeting.Name)

	return msgAndChan, nil
}

// handleCreatePoll is the function to handle a received message requesting a poll Creation.
// It is called by the function handlePublish.
// The received message had the object field set to "poll" and action field to "create"
// It will check for the validity of the received message, store the received message in the database, and store the new
// poll in the database.
func (o *organizer) handleCreatePoll(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {

	if !strings.HasPrefix(canal, "/root/") {
		log.Printf("invalid channel name for meeting creation: %s", canal)
		return nil, lib.ErrInvalidResource
	}

	data, err := parser.ParseDataCreatePoll(msg.Data)
	if err != nil {
		log.Printf("unable to parse received message data into a message.DataCreatePoll structure")
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

	err = db.CreateChannel(poll, o.database)
	if err != nil {
		log.Printf("An error occured, unable to create channel in the database")
		return nil, err
	}

	err = db.CreateMessage(msg, canal, o.database)
	if err != nil {
		log.Printf("An error occured, unable to store received message in the database")
		return nil, err
	}

	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}

	log.Printf("Sucessfully created poll %s", poll.Name)

	return msgAndChan, nil
}

// handleUpdateProperties is the function to handle a received message requesting a change of some properties of a LAO (currently).
// It should already be relatively modular if action "update_properties" will exists for events others than a LAO.
// It is called by the function handlePublish.
// The received message had the object field set to "lao" and action field to "update_properties"
// It will store the received message in the database, and send the change request to every subscriber of the channel,
// waiting for Witness's validation to make the update.
func (o *organizer) handleUpdateProperties(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	// if statement to check that if SigThreshold == 0 then directly make the stateUpdate
	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}
	err = db.CreateMessage(msg, canal, o.database)
	if err != nil {
		return msgAndChan, err
	}
	if SigThreshold == 0 {
		data, err := parser.ParseDataWitnessMessage(msg.Data)
		if err != nil {
			log.Printf("unable to parse received Message in handleUpdateProperties()")
			return nil, err
		}
		//retrieve message to sign from database
		toApply := db.GetMessage([]byte(canal), data.MessageId, o.database)
		if toApply == nil {
			return nil, lib.ErrInvalidResource
		}

		toApplyStruct, err := parser.ParseMessage(toApply)
		if err != nil {
			log.Printf("unable to parse (just) stored Message in handleUpdateProperties()")
			return nil, lib.ErrRequestDataInvalid
		}
		queryStr, err := o.applyUpdates(toApplyStruct, data)
		if err != nil {
			return nil, err
		}
		msgAndChan = append(msgAndChan, lib.MessageAndChannel{Channel: []byte(canal), Message: queryStr})
	}
	return msgAndChan, nil
}

// handleWitnessMessage is the function to handle a received message validating a previously received message.
// It retrieves the message that had to be signed, verifies the received signature and checks every received signature already received
// (as they are stored on the disk we have no guarantee they were not tempered with). If they are enough signatures,
// it should append to the list of returned message a reacting message (like a state broadcast for example). This is still to be
// implemented.
func (o *organizer) handleWitnessMessage(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {

	data, err := parser.ParseDataWitnessMessage(msg.Data)
	if err != nil {
		log.Printf("unable to parse received Message in handleWitnessMessage()")
		return nil, err
	}

	//retrieve message to sign from database
	toSign := db.GetMessage([]byte(canal), data.MessageId, o.database)
	if toSign == nil {
		return nil, lib.ErrInvalidResource
	}

	toSignStruct, err := parser.ParseMessage(toSign)
	if err != nil {
		log.Printf("unable to parse stored Message in handleWitnessMessage()")
		return nil, lib.ErrRequestDataInvalid
	}

	// verify signature correctness
	err = security.VerifySignature(msg.Sender, toSignStruct.Data, data.Signature)
	if err != nil {
		log.Printf("invalid message signature")
		return nil, err
	}

	//retrieves all the existing signatures for this message
	var signaturesOnly []string
	count := 0
	for i, item := range toSignStruct.WitnessSignatures {
		witnessSignature, err := parser.ParseWitnessSignature(item)
		if err != nil {
			log.Println("couldn't unMarshal the ItemWitnessSignatures from the DB")
			continue
		}
		//dataWitnessMessage' signature is Sign(message_id)
		err = security.VerifySignature(witnessSignature.WitnessKey, toSignStruct.MessageId, witnessSignature.Signature)
		if err != nil {
			log.Printf("Invalid signature found in signature lists: index %d. Should check if the database has not been tempered with", i)
		} else {
			count++
		}
		signaturesOnly = append(signaturesOnly, string(witnessSignature.Signature))
	}

	// if new signature already exists, returns error
	// does not return an error if the signature's threshold is achieved. This mechanism is to let a witness
	// trigger the process of sending the "update properties" once more if it failed during the first execution, but
	// after its message was stored on the database
	_, found := lib.FindStr(signaturesOnly, string(data.Signature))
	if found && count < SigThreshold {
		log.Printf("Signature already exists in message list")
		return nil, lib.ErrResourceAlreadyExists
	} else {

		iws, err := json.Marshal(message.ItemWitnessSignatures{WitnessKey: msg.Sender, Signature: data.Signature})
		if err != nil {
			log.Println("couldn't Marshal the ItemWitnessSignatures")
		}

		toSignStruct.WitnessSignatures = append(toSignStruct.WitnessSignatures, iws)
	}

	// update "LAOUpdateProperties" (or other message) in DB
	err = db.UpdateMessage(toSignStruct, canal, o.database)
	if err != nil {
		log.Printf("An error occured, unable to update message in the database")
		return nil, lib.ErrDBFault
	}

	//store received message in DB
	err = db.CreateMessage(msg, canal, o.database)
	if err != nil {
		log.Printf("An error occured, unable to update message in the database")
		return nil, lib.ErrDBFault
	}

	//broadcast received message
	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(canal),
	}}

	if count == SigThreshold-1 {
		queryStr, err := o.applyUpdates(toSignStruct, data)
		if err != nil {
			return nil, err
		}

		msgAndChan = append(msgAndChan, lib.MessageAndChannel{Channel: []byte(canal), Message: queryStr})
	}

	return msgAndChan, nil
}

// applyUpdates returns a state update message for the data field of the Message. For now state update messages
// exists only for LAOs. We should find a trick to avoid giving the dataToSign field, as it's just the message.data field.
// DataToSign is used only to get the object field.
func (o *organizer) applyUpdates(toSignStruct message.Message, dataWitnessMess message.DataWitnessMessage) (queryStr []byte, err error) {
	var eventStruct interface{}

	dataToSign, err := parser.ParseData(string(toSignStruct.Data))
	if err != nil {
		log.Printf("could not parse the data to sign into a message.Data structure")
		return nil, lib.ErrDBFault
	}

	switch dataToSign["object"] {
	case "lao":
		laoData, err := parser.ParseDataCreateLAO(toSignStruct.Data)
		if err != nil {
			log.Printf("unable to parse stored LAO infos in handleWitnessMessage()")
			return nil, err
		}

		eventStruct = event.LAO{
			ID:            string(laoData.ID),
			Name:          laoData.Name,
			Creation:      laoData.Creation,
			OrganizerPKey: string(laoData.Organizer),
			Witnesses:     lib.NestedByteArrayToStringArray(laoData.Witnesses),
		}
		queryStr, err = parser.ComposeBroadcastStateLAO(eventStruct.(event.LAO), o.PublicKey, toSignStruct)
		if err != nil {
			log.Printf("could not compose a state update broadcast message")
			return nil, err
		}

	default:
		log.Printf("witnesses not able to witness something else than LAO state update for now")
		return nil, lib.ErrNotYetImplemented
	}

	err = db.UpdateChannel(eventStruct, o.database)
	if err != nil {
		log.Printf("error updating the message in the database")
		return nil, err
	}

	return queryStr, nil
}

// handleCatchup is the function to handle a received message requesting a catchup on a channel.
// It is called by HandleReceivedMessage, and returns the current state of a channel.
// DEPRECATED, this is not the intended goal of this method, it should return the history of messages but it is not implemented yet.
func (o *organizer) handleCatchup(query message.Query) ([]byte, error) {
	// TODO maybe pass userId as an arg in order to check access rights later on?
	params, err := parser.ParseParams(query.Params)
	if err != nil {
		log.Printf("unable to analyse params in handleCatchup()")
		return nil, lib.ErrRequestDataInvalid
	}
	history := db.GetChannel([]byte(params.Channel), o.database)

	return history, nil
}

//handleLAOState is just here to implement the Actor interface. It returns an error as, in the current implementation there
// is only one organizer, and he's the one sending this message. Hence he should not be receiving it.
func (o *organizer) handleLAOState(msg message.Message) (msgAndChannel []lib.MessageAndChannel, err error) {
	return nil, lib.ErrInvalidAction
}

//handleMeetingState is just here to implement the Actor interface. It returns an error as, in the current implementation there
// is only one organizer, and he's the one sending this message. Hence he should not be receiving it.
func (o *organizer) handleMeetingState(msg message.Message) (msgAndChannel []lib.MessageAndChannel, err error) {
	return nil, lib.ErrInvalidAction
}

// handleOpenRollCall is used If the roll-call was started in future mode (see create roll-call), it can be opened using
// the open roll-call query. If it was closed, but it need to be reopened later (e.g. the organizer forgot to scan
// the public key of one attendee), then it can reopen it by using the open query. In this case, the action should be
// set to reopen.
func (o *organizer) handleOpenRollCall(msg message.Message, channel string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	if !strings.HasPrefix(channel, "/root/") {
		log.Printf("invalid channel name for meeting creation: %s", channel)
		return nil, lib.ErrInvalidResource
	}

	openRollCall, err := parser.ParseDataOpenRollCall(msg.Data)
	if err != nil {
		log.Printf("unable to analyse params in handlOpenRollCall()")
		return nil, lib.ErrRequestDataInvalid
	}

	//retrieve roll Call to open from database
	storedRollCall := db.GetChannel(openRollCall.ID, o.database)
	if storedRollCall == nil {
		log.Printf("unable to access the stored roll call : ID or DB does not exist ")
		return nil, lib.ErrInvalidResource
	}

	rollCallData := event.RollCall{}
	err = json.Unmarshal(storedRollCall, &rollCallData)
	if err != nil {
		log.Printf("unable to parse stored roll call infos in handleOpenRollRall()")
		return nil, err
	}

	//we provide the id of the channel
	laoId := strings.TrimPrefix(channel, "/root/")
	if !security.RollCallOpenedIsValid(openRollCall, laoId, rollCallData) {
		log.Printf("roll call data invalid. Roll call not created")
		return nil, lib.ErrInvalidResource
	}

	updatedRollCall := event.RollCall{
		ID:           string(openRollCall.ID),
		Name:         rollCallData.Name,
		Creation:     rollCallData.Creation,
		LastModified: rollCallData.Creation,
		Location:     rollCallData.Location,
		Start:        openRollCall.Start,
		Description:  rollCallData.Description,
	}

	err = db.UpdateChannel(updatedRollCall, o.database)
	if err != nil {
		log.Printf("could not update channel in the database")
		return nil, err
	}
	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(channel),
	}}
	return msgAndChan, db.CreateMessage(msg, channel, o.database)
}

// handleCloseRollCall is used to close the roll-call. If it was closed, but it need to be reopened later (e.g. the organizer forgot to scan
// the public key of one attendee), then it can reopen it by using the open query. In this case, the action should be
// set to reopen.
func (o *organizer) handleCloseRollCall(msg message.Message, channel string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err_ error) {
	if !strings.HasPrefix(channel, "/root/") {
		log.Printf("invalid channel name for meeting creation: %s", channel)
		return nil, lib.ErrInvalidResource
	}

	closeRollCall, err := parser.ParseDataCloseRollCall(msg.Data)
	if err != nil {
		log.Printf("unable to analyse params in handleCloseRollCall()")
		return nil, lib.ErrRequestDataInvalid
	}
	//retrieve roll Call to close from database
	storedRollCall := db.GetChannel(closeRollCall.ID, o.database)

	rollCallData := event.RollCall{}
	err = json.Unmarshal(storedRollCall, &rollCallData)
	if err != nil {
		log.Printf("unable to parse stored roll call infos in handleCloseRollCall()")
		return nil, err
	}

	//we provide the id of the channel
	laoId := strings.TrimPrefix(channel, "/root/")
	if !security.RollCallClosedIsValid(closeRollCall, laoId, rollCallData) {
		log.Printf("roll call data invalid. Roll call not closed")
		return nil, lib.ErrInvalidResource
	}

	updatedRollCall := event.RollCall{
		ID:           string(closeRollCall.ID),
		Name:         rollCallData.Name,
		Creation:     rollCallData.Creation,
		LastModified: rollCallData.Creation,
		Location:     rollCallData.Location,
		Description:  rollCallData.Description,
		Start:        closeRollCall.Start,
		Attendees:    closeRollCall.Attendees,
		End:          closeRollCall.End,
	}

	err = db.UpdateChannel(updatedRollCall, o.database)
	if err != nil {
		log.Printf("unable to update channel in database")
		return nil, err
	}

	err = db.CreateMessage(msg, channel, o.database)
	if err != nil {
		log.Printf("unable to store receieved message in the database")
		return nil, err
	}

	msgAndChan := []lib.MessageAndChannel{{
		Message: parser.ComposeBroadcastMessage(query),
		Channel: []byte(channel),
	}}

	return msgAndChan, nil
}
