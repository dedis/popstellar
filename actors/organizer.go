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

func (o *Organizer) handleMessage(generic define.Generic) ([]byte, []byte, error) {
	params, err := define.AnalyseParamsFull(generic.Params)
	if err != nil {
		fmt.Printf("unable to analyse paramsLight in handleMessage()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	message, err := define.AnalyseMessage(params.Message)
	if err != nil {
		fmt.Printf("unable to analyse Message in handleMessage()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	data, err := define.AnalyseData(string(message.Data))
	if err != nil {
		fmt.Printf("unable to analyse data in handleMessage()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	err = db.CreateMessage(message, params.Channel, o.database)
	if err != nil {
		return nil, nil, err
	}

	switch data["object"] {
	case "message":
		switch data["action"] {
		case "witness":
			return o.handleWitnessMessage(message, params.Channel, generic)
		default:
			return nil, nil, define.ErrRequestDataInvalid
		}
	case "state":
		{
			return o.handleLAOState(message, params.Channel, generic)
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
	if err != nil {
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

/** @returns, in order
 * message
 * channel
 * error
 */
func (o *Organizer) handleCreateRollCall(message define.Message, canal string, generic define.Generic) ([]byte, []byte, error) {
	if canal == "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateRollCall(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}

	if !define.RollCallCreatedIsValid(data, message) {
		return nil, nil, err
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
	err = db.CreateChannel(event, o.database)
	if err != nil {
		return nil, nil, err
	}

	err = db.CreateMessage(message, canal, o.database)
	if err != nil {
		return nil, nil, err
	}
	msg, channel := finalizeHandling(canal, generic)
	return msg, channel, nil
}

/** @returns, in order
 * message
 * channel
 * error
 */
func (o *Organizer) handleCreateMeeting(message define.Message, canal string, generic define.Generic) ([]byte, []byte, error) {

	if canal == "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateMeeting(message.Data)
	if err != nil {
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
	err = db.CreateChannel(event, o.database)
	if err != nil {
		return nil, nil, err
	}
	err = db.CreateMessage(message, canal, o.database)
	if err != nil {
		return nil, nil, err
	}
	channel, msg := finalizeHandling(canal, generic)
	return channel, msg, nil
}

/** @returns, in order
 * message
 * channel
 * error
 */
func (o *Organizer) handleCreatePoll(message define.Message, canal string, generic define.Generic) ([]byte, []byte, error) {

	if canal == "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreatePoll(message.Data)
	if err != nil {
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

	err = db.CreateChannel(event, o.database)
	if err != nil {
		return nil, nil, err
	}
	channel, msg := finalizeHandling(canal, generic)
	return channel, msg, nil
}

/**
@returns, in order
* message
* channel
*/
func (o *Organizer) handleUpdateProperties(message define.Message, canal string, generic define.Generic) ([]byte, []byte, error) {
	channel, msg := finalizeHandling(canal, generic)
	return channel, msg, db.CreateMessage(message, canal, o.database)
}

/** @returns, in order
 * message
 * channel
 * error
 */
func (o *Organizer) handleWitnessMessage(message define.Message, canal string, generic define.Generic) ([]byte, []byte, error) {
	//TODO verify signature correctness
	// decrypt msg and compare with hash of "local" data

	//retrieve message to sign from database
	toSign := db.GetMessage([]byte(canal), []byte(message.MessageId), o.database)
	if toSign == nil {
		return nil, nil, define.ErrInvalidResource
	}

	toSignStruct, err := define.AnalyseMessage(toSign)
	if err != nil {
		fmt.Printf("unable to analyse Message in handleWitnessMessage()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	//if message was already signed by this witness, returns an error
	_, found := define.FindStr(toSignStruct.WitnessSignatures, message.Signature)
	if found {
		return nil, nil, define.ErrResourceAlreadyExists
	}

	toSignStruct.WitnessSignatures = append(toSignStruct.WitnessSignatures, message.Signature)

	// update "LAOUpdateProperties" message in DB
	err = db.UpdateMessage(toSignStruct, canal, o.database)
	if err != nil {
		return nil, nil, define.ErrDBFault
	}
	//store received message in DB
	err = db.CreateMessage(message, canal, o.database)
	if err != nil {
		return nil, nil, define.ErrDBFault
	}

	//broadcast received message
	channel, msg := finalizeHandling(canal, generic)
	return channel, msg, nil
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
func (o *Organizer) handleLAOState(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {
	return nil, nil, define.ErrInvalidAction
}
