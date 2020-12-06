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

// Test json input to create LAO:
//  careful with base64 needed to remove
//  careful with comma after witnesses[] and witnesses_signatures[] needed to remove

/** processes what is received from the WebSocket
 * msg : receivedMessage
 * returns, in order :
 * message to send on channel, or nil
 * channel for the message, or nil
 * response to the sender, or nil
 */
func (o *Organizer) HandleWholeMessage(msg []byte, userId int) ([]byte, []byte, []byte) {

	generic, err := define.AnalyseGeneric(msg)
	if err != nil {
		return nil, nil, define.CreateResponse(define.ErrIdNotDecoded, nil, generic)
	}

	var history []byte = nil
	var message []byte = nil
	var channel []byte = nil

	switch generic.Method {
	case "subscribe":
		message, channel, err = nil, nil, handleSubscribe(generic, userId)
	case "unsubscribe":
		message, channel, err = nil, nil, handleUnsubscribe(generic, userId)
	case "publish":
		message, channel, err = o.handlePublish(generic)
	//case "message": err = h.handleMessage()
	//Potentially, we never receive a "message" and only output "message" after a "publish" in order to broadcast.
	//Or they are only notification, and we just want to check that it was a success
	case "catchup":
		history, err = o.handleCatchup(generic)
	default:
		fmt.Printf("method not recognized, generating default response")
		message, channel, err = nil, nil, define.ErrRequestDataInvalid
	}

	return message, channel, define.CreateResponse(err, history, generic)
}

/** @returns, in order
 * message
 * channel
 * error
 */
func (o *Organizer) handlePublish(generic define.Generic) ([]byte, []byte, error) {
	params, err := define.AnalyseParamsFull(generic.Params)
	if err != nil {
		fmt.Printf("unable to analyse paramsLight in handlePublish()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	message, err := define.AnalyseMessage(params.Message)
	if err != nil {
		fmt.Printf("unable to analyse Message in handlePublish()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	data, err := define.AnalyseData(string(message.Data))
	if err != nil {
		fmt.Printf("unable to analyse data in handlePublish()")
		return nil, nil, define.ErrRequestDataInvalid
	}

	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "create":
			return o.handleCreateLAO(message, params.Channel, generic)
		case "update_properties":
			return o.handleUpdateProperties(message, params.Channel, generic)
		case "state":

		default:
			return nil, nil, define.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			return o.handleWitnessMessage(message, params.Channel, generic)
			//TODO: update state and send state broadcast
		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "roll call":
		switch data["action"] {
		case "create":
			return o.handleCreateRollCall(message, params.Channel, generic)
		case "state":

		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			return o.handleCreateMeeting(message, params.Channel, generic)
		case "state":

		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create":
			return o.handleCreatePoll(message, params.Channel, generic)
		case "state":

		default:
			return nil, nil, define.ErrInvalidAction
		}
	default:
		fmt.Printf("data[action] not recognized in handlepublish, generating default response ")
		return nil, nil, define.ErrRequestDataInvalid
	}

	return nil, nil, nil
}

/** @returns, in order
 * message
 * channel
 * error
 */
func (o *Organizer) handleCreateLAO(message define.Message, canal string, generic define.Generic) ([]byte, []byte, error) {

	if canal != "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}

	if !define.LAOCreatedIsValid(data, message) {
		return nil, nil, define.ErrInvalidResource
	}

	canalLAO := canal + data.ID

	err = db.CreateMessage(message, canalLAO, o.database)
	if err != nil {
		return nil, nil, err
	}

	lao := define.LAO{
		ID:            data.ID,
		Name:          data.Name,
		Creation:      data.Creation,
		LastModified:  data.Last_modified,
		OrganizerPKey: data.Organizer,
		Witnesses:     data.Witnesses,
	}
	err = db.CreateChannel(lao, o.database)
	if err != nil {
		return nil, nil, err
	}
	msg, channel := finalizeHandling(message, canal, generic)
	return msg, channel, nil
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
		Name:         data.Name,
		Creation:     data.Creation,
		LastModified: data.Last_modified,
		Location:     data.Location,
		Start:        data.Start,
		End:          data.End,
		Extra:        data.Extra,
	}
	err = db.CreateChannel(event, o.database)
	if err != nil {
		return nil, nil, err
	}
	msg, channel := finalizeHandling(message, canal, generic)
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
		Name:         data.Name,
		Creation:     data.Creation,
		LastModified: data.Last_modified,
		Location:     data.Location,
		Start:        data.Start,
		End:          data.End,
		Extra:        data.Extra,
	}
	err = db.CreateChannel(event, o.database)
	if err != nil {
		return nil, nil, err
	}
	channel, msg := finalizeHandling(message, canal, generic)
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

	// don't need to check for validity if we use json schema
	event := define.Poll{ID: data.ID,
		Name:         data.Name,
		Creation:     data.Creation,
		LastModified: data.Last_modified,
		Location:     data.Location,
		Start:        data.Start,
		End:          data.End,
		Extra:        data.Extra,
	}

	err = db.CreateChannel(event, o.database)
	if err != nil {
		return nil, nil, err
	}
	channel, msg := finalizeHandling(message, canal, generic)
	return channel, msg, nil
}
func handleMessage(msg []byte, userId int) error {
	return nil
}

//TODO check workflow correctness
/** @returns, in order
 * message
 * channel
 */
func (o *Organizer) handleUpdateProperties(message define.Message, canal string, generic define.Generic) ([]byte, []byte, error) {
	channel, msg := finalizeHandling(message, canal, generic)
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

	//add signature to already stored message:

	//retrieve message to sign from database
	toSign := db.GetMessage([]byte(canal), []byte(message.Message_id), o.database)
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
	channel, msg := finalizeHandling(message, canal, generic)
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

/** @returns, in order
 * message
 * channel
 */
func finalizeHandling(message define.Message, canal string, generic define.Generic) ([]byte, []byte) {
	return define.CreateBroadcastMessage(generic), []byte(canal)
}
