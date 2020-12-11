/*
This class implements the functions an organizer provides. It stores messages in the database using the db package.
Currently it does not do send response to channels (only ack messages as defined in the protocol). Implementation not
finished as it is not the most important class for now. Does not support publish method.
*/

package actors

import (
	b64 "encoding/base64"
	"encoding/json"
	"fmt"
	"student20_pop/db"
	"student20_pop/define"
)

type Witness struct {
	PublicKey string
	database  string
}

func NewWitness(pkey string, db string) *Witness {
	return &Witness{
		PublicKey: pkey,
		database:  db,
	}
}

/* processes what is received from the websocket
 * msg : receivedMessage
 */
func (w *Witness) HandleWholeMessage(receivedMsg []byte, userId int) (message, channel, responseToSender []byte) {
	generic, err := define.AnalyseGeneric(receivedMsg)
	if err != nil {
		return nil, nil, define.CreateResponse(define.ErrRequestDataInvalid, nil, generic)

	}

	var history []byte = nil
	var msg []byte = nil
	var chann []byte = nil

	switch generic.Method {
	case "publish":
		msg, chann, err = w.handlePublish(generic)
	case "message":
		msg, chann, err = w.handleMessage(generic)
	default:
		msg, chann, err = nil, nil, define.ErrRequestDataInvalid
	}

	return msg, chann, define.CreateResponse(err, history, generic)
}

func (w *Witness) handlePublish(generic define.Generic) (messsage, channel []byte, err error) {
	return nil, nil, define.ErrInvalidAction //a witness cannot handle a publish request for now
}

/** @returns, in order
 * message
 * channel
 * error
 */
func (w *Witness) handleMessage(generic define.Generic) ([]byte, []byte, error) {
	params, err := define.AnalyseParamsFull(generic.Params)
	if err != nil {
		return nil, nil, define.ErrRequestDataInvalid
	}

	message, err := define.AnalyseMessage(params.Message)
	if err != nil {
		return nil, nil, define.ErrRequestDataInvalid
	}

	data := define.Data{}
	base64Text := make([]byte, b64.StdEncoding.DecodedLen(len(message.Data)))

	l, err := b64.StdEncoding.Decode(base64Text, message.Data)
	if err != nil {
		return nil, nil, err
	}

	err = json.Unmarshal(base64Text[:l], &data)
	if err != nil {
		return nil, nil, define.ErrRequestDataInvalid
	}

	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "create":
			return w.handleCreateLAO(message, params.Channel, generic)
		case "update_properties":
			return w.handleUpdateProperties(message, params.Channel, generic)
		case "state":
			return w.handleLAOState(message, params.Channel, generic)
		default:
			return nil, nil, define.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			return w.handleWitnessMessage(message, params.Channel, generic)
		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "roll call":
		switch data["action"] {
		case "create":
			return w.handleCreateRollCall(message, params.Channel, generic)
		case "state":
			return nil, nil, define.ErrInvalidAction
		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			return nil, nil, define.ErrInvalidAction
		case "state":
			return nil, nil, define.ErrInvalidAction
		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create":
			return nil, nil, define.ErrInvalidAction
		case "state":
			return nil, nil, define.ErrInvalidAction
		default:
			return nil, nil, define.ErrInvalidAction
		}
	default:
		return nil, nil, define.ErrRequestDataInvalid
	}
}

func (w *Witness) handleCreateLAO(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {
	if channel != "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}

	if !define.LAOIsValid(data, message, true) {
		return nil, nil, define.ErrInvalidResource
	}

	err = db.CreateMessage(message, channel, w.database)
	if err != nil {
		return nil, nil, err
	}

	lao := define.LAO{
		ID:            data.ID,
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: data.Organizer,
		Witnesses:     data.Witnesses,
	}
	err = db.CreateChannel(lao, w.database)

	return nil, nil, err
}

/*witness does not yet send stuff to channel*/
func (w *Witness) handleUpdateProperties(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {
	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}
	if !define.LAOIsValid(data, message, false) {
		return nil, nil, define.ErrInvalidResource
	}

	//stores received message in DB
	err = db.CreateMessage(message, channel, w.database)
	if err != nil {
		return nil, nil, err
	}

	return nil, nil, err
}

func (w *Witness) handleWitnessMessage(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {

	data, err := define.AnalyseDataWitnessMessage(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}

	//stores received message in DB
	err = db.CreateMessage(message, channel, w.database)
	if err != nil {
		return nil, nil, err
	}

	msg := db.GetMessage([]byte(channel), []byte(data.Message_id), w.database)
	if msg == nil {
		fmt.Printf("no message with ID %v in the database", data.Message_id)
		return nil, nil, define.ErrInvalidResource
	}
	storedMessage, err := define.AnalyseMessage(msg)

	if err != nil {
		fmt.Printf("unable to unmarshall the message stored in the database")
		return nil, nil, define.ErrDBFault
	}

	err = define.VerifySignature(message.Sender, storedMessage.Data, data.Signature)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}

	//adds the signature to signature list
	storedMessage.WitnessSignatures = append(storedMessage.WitnessSignatures, data.Signature)

	//update message in DB
	err = db.UpdateMessage(storedMessage, channel, w.database)

	return nil, nil, err
}

func (w *Witness) handleLAOState(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {
	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}

	if !define.LAOIsValid(data, message, false) {
		return nil, nil, define.ErrInvalidResource
	}

	//TODO correct usage of VerifyWitnessSignatures
	err = define.VerifyWitnessSignatures(nil, message.WitnessSignatures, message.Sender)
	if err != nil {
		return nil, nil, define.ErrRequestDataInvalid
	}

	lao := define.LAO{
		ID:            data.ID,
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: data.Organizer,
		Witnesses:     data.Witnesses,
	}

	err = db.UpdateChannel(lao, w.database)

	return nil, nil, err
}

func (w *Witness) handleCreateRollCall(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {

	data, err := define.AnalyseDataCreateRollCall(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}

	if !define.RollCallCreatedIsValid(data, message) {
		return nil, nil, define.ErrInvalidResource
	}

	rollCall := define.RollCall{
		ID:       data.ID,
		Name:     data.Name,
		Creation: data.Creation,
		Location: data.Location,
		Start:    data.Start,
		End:      data.End,
	}

	err = db.CreateChannel(rollCall, w.database)
	if err != nil {
		return nil, nil, err
	}

	err = db.CreateMessage(message, channel, w.database)
	if err != nil {
		return nil, nil, err
	}

	return nil, nil, err
}
