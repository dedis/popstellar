/*
This class implements the functions an organizer provides. It stores messages in the database using the db package
and create and sends appropriate response depending on what message was received.
*/
package actors

import (
	"fmt"
	"student20_pop/db"
	"student20_pop/define"
)

type Organizer struct {
	PublicKey string
	database  string
}

func NewOrganizer(pkey string, db string) *Organizer {
	return &Organizer{
		PublicKey: pkey,
		database:  db,
	}
}

/** processes what is received from the websocket */
func (o *Organizer) HandleWholeMessage(receivedMsg []byte, userId int) (message, channel, responseToSender []byte) {
	//this cannot handle an error message and will send an error message bak, resulting in an infinite loop TODO
	generic, err := define.AnalyseGeneric(receivedMsg)
	if err != nil {
		return nil, nil, define.CreateResponse(define.ErrIdNotDecoded, nil, generic)
	}

	var history []byte = nil
	var msg []byte = nil
	var chann []byte = nil

	switch generic.Method {
	case "subscribe":
		msg, chann, err = nil, nil, handleSubscribe(generic, userId)
	case "unsubscribe":
		msg, chann, err = nil, nil, handleUnsubscribe(generic, userId)
	case "publish":
		msg, chann, err = o.handlePublish(generic)
	case "message":
		msg, chann, err = o.handleMessage(generic)
	//Or they are only notification, and we just want to check that it was a success
	case "catchup":
		history, err = o.handleCatchup(generic)
	default:
		fmt.Printf("method not recognized, generating default response")
		msg, chann, err = nil, nil, define.ErrRequestDataInvalid
	}

	return msg, chann, define.CreateResponse(err, history, generic)
}

func (o *Organizer) handleMessage(generic define.Generic) (message, channel []byte, err error) {
	params, errs := define.AnalyseParamsFull(generic.Params)
	if errs != nil {
		fmt.Printf("unable to analyse paramsLight in handleMessage()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	msg, errs := define.AnalyseMessage(params.Message)
	if errs != nil {
		fmt.Printf("unable to analyse Message in handleMessage()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	data, errs := define.AnalyseData(string(msg.Data))
	if errs != nil {
		fmt.Printf("unable to analyse data in handleMessage()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	errs = db.CreateMessage(msg, params.Channel, o.database)
	if errs != nil {
		return nil, nil, errs
	}

	switch data["object"] {
	case "message":
		switch data["action"] {
		case "witness":
			return o.handleWitnessMessage(msg, params.Channel, generic)
		default:
			return nil, nil, define.ErrRequestDataInvalid
		}
	case "state":
		{
			return o.handleLAOState(msg, params.Channel, generic)
		}
	default:
		return nil, nil, define.ErrRequestDataInvalid
	}

}

/* handles a received publish message */
func (o *Organizer) handlePublish(generic define.Generic) (message, channel []byte, err error) {
	params, errs := define.AnalyseParamsFull(generic.Params)
	if errs != nil {
		fmt.Printf("unable to analyse paramsLight in handlePublish()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	msg, errs := define.AnalyseMessage(params.Message)
	if errs != nil {
		fmt.Printf("unable to analyse Message in handlePublish()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	err = define.MessageIsValid(msg)
	if errs != nil {
		fmt.Printf("7")
		return nil, nil, define.ErrRequestDataInvalid
	}

	data, errs := define.AnalyseData(string(msg.Data))
	if errs != nil {
		fmt.Printf("unable to analyse data in handlePublish()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "create":
			return o.handleCreateLAO(msg, params.Channel, generic)
		case "update_properties":
			return o.handleUpdateProperties(msg, params.Channel, generic)
		case "state":
			return o.handleLAOState(msg, params.Channel, generic) // should never happen
		default:
			return nil, nil, define.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			return o.handleWitnessMessage(msg, params.Channel, generic)
			//TODO: update state and send state broadcast
			// TODO : state broadcast done on root/ or on LAO channel
		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "roll call":
		switch data["action"] {
		case "create":
			return o.handleCreateRollCall(msg, params.Channel, generic)
		//case "state":  TODO : waiting on protocol definition
		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			return o.handleCreateMeeting(msg, params.Channel, generic)
		case "state": //
			// TODO: waiting on protocol definition
			return nil, nil, define.ErrInvalidAction
		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create":
			return o.handleCreatePoll(msg, params.Channel, generic)
		case "state":
			// TODO: waiting on protocol definition
			return nil, nil, define.ErrInvalidAction
		default:
			return nil, nil, define.ErrInvalidAction
		}
	default:
		fmt.Printf("data[action] not recognized in handlepublish, generating default response ")
		return nil, nil, define.ErrRequestDataInvalid
	}
}

/* handles the creation of a LAO */
func (o *Organizer) handleCreateLAO(msg define.Message, canal string, generic define.Generic) (message, channel []byte, err error) {

	if canal != "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, errs := define.AnalyseDataCreateLAO(msg.Data)
	if errs != nil {
		return nil, nil, define.ErrInvalidResource
	}

	if !define.LAOIsValid(data, msg, true) {
		return nil, nil, define.ErrInvalidResource
	}

	errs = db.CreateMessage(msg, canal, o.database)
	if errs != nil {
		return nil, nil, err
	}

	lao := define.LAO{
		ID:            data.ID,
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: data.Organizer,
		Witnesses:     data.Witnesses,
	}
	errs = db.CreateChannel(lao, o.database)
	if errs != nil {
		return nil, nil, err
	}

	msgToSend, chann := finalizeHandling(canal, generic)
	return msgToSend, chann, nil
}

func (o *Organizer) handleCreateRollCall(msg define.Message, canal string, generic define.Generic) (message, channel []byte, err error) {
	if canal == "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, errs := define.AnalyseDataCreateRollCall(msg.Data)
	if errs != nil {
		return nil, nil, define.ErrInvalidResource
	}

	if !define.RollCallCreatedIsValid(data, msg) {
		return nil, nil, errs
	}

	// don't need to check for validity if we use json schema
	event := define.RollCall{ID: data.ID,
		Name:     data.Name,
		Creation: data.Creation,
		Location: data.Location,
		Start:    data.Start,
		End:      data.End,
		Extra:    data.Extra,
	}
	errs = db.CreateChannel(event, o.database)
	if errs != nil {
		return nil, nil, errs
	}

	errs = db.CreateMessage(msg, canal, o.database)
	if errs != nil {
		return nil, nil, errs
	}
	sendMsg, chann := finalizeHandling(canal, generic)
	return sendMsg, chann, nil
}

func (o *Organizer) handleCreateMeeting(msg define.Message, canal string, generic define.Generic) (message, channel []byte, err error) {

	if canal == "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, errs := define.AnalyseDataCreateMeeting(msg.Data)
	if errs != nil {
		return nil, nil, define.ErrInvalidResource
	}

	// don't need to check for validity if we use json schema
	event := define.Meeting{ID: data.ID,
		Name:     data.Name,
		Creation: data.Creation,
		Location: data.Location,
		Start:    data.Start,
		End:      data.End,
		Extra:    data.Extra,
	}
	errs = db.CreateChannel(event, o.database)
	if errs != nil {
		return nil, nil, errs
	}
	errs = db.CreateMessage(msg, canal, o.database)
	if errs != nil {
		return nil, nil, errs
	}
	sendMsg, chann := finalizeHandling(canal, generic)
	return sendMsg, chann, nil
}

func (o *Organizer) handleCreatePoll(msg define.Message, canal string, generic define.Generic) (message, channel []byte, err error) {

	if canal == "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, errs := define.AnalyseDataCreatePoll(msg.Data)
	if errs != nil {
		return nil, nil, define.ErrInvalidResource
	}

	event := define.Poll{ID: data.ID,
		Name:     data.Name,
		Creation: data.Creation,
		Location: data.Location,
		Start:    data.Start,
		End:      data.End,
		Extra:    data.Extra,
	}

	errs = db.CreateChannel(event, o.database)
	if errs != nil {
		return nil, nil, err
	}
	sendMsg, chann := finalizeHandling(canal, generic)
	return sendMsg, chann, nil
}

func (o *Organizer) handleUpdateProperties(msg define.Message, canal string, generic define.Generic) (message, channel []byte, err error) {
	sendMsg, chann := finalizeHandling(canal, generic)
	return sendMsg, chann, db.CreateMessage(msg, canal, o.database)
}

func (o *Organizer) handleWitnessMessage(msg define.Message, canal string, generic define.Generic) (message, channel []byte, err error) {
	//TODO verify signature correctness
	// decrypt msg and compare with hash of "local" data

	//retrieve message to sign from database
	toSign := db.GetMessage([]byte(canal), []byte(msg.MessageId), o.database)
	if toSign == nil {
		return nil, nil, define.ErrInvalidResource
	}

	toSignStruct, errs := define.AnalyseMessage(toSign)
	if errs != nil {
		fmt.Printf("unable to analyse Message in handleWitnessMessage()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	//if message was already signed by this witness, returns an error
	_, found := define.FindStr(toSignStruct.WitnessSignatures, msg.Signature)
	if found {
		return nil, nil, define.ErrResourceAlreadyExists
	}

	toSignStruct.WitnessSignatures = append(toSignStruct.WitnessSignatures, msg.Signature)

	// update "LAOUpdateProperties" message in DB
	errs = db.UpdateMessage(toSignStruct, canal, o.database)
	if errs != nil {
		return nil, nil, define.ErrDBFault
	}
	//store received message in DB
	errs = db.CreateMessage(msg, canal, o.database)
	if errs != nil {
		return nil, nil, define.ErrDBFault
	}

	//broadcast received message
	sendMsg, chann := finalizeHandling(canal, generic)
	return sendMsg, chann, nil
}

func (o *Organizer) handleCatchup(generic define.Generic) ([]byte, error) {
	// TODO maybe pass userId as an arg in order to check access rights later on?
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		fmt.Printf("unable to analyse paramsLight in handleCatchup()")
		return nil, define.ErrRequestDataInvalid
	}
	history := db.GetChannel([]byte(params.Channel), o.database)

	return history, nil
}

//just to implement the interface, this function is not needed for the Organizer (as he's the one sending this message)
func (o *Organizer) handleLAOState(msg define.Message, chann string, generic define.Generic) (message, channel []byte, err error) {
	return nil, nil, define.ErrInvalidAction
}
