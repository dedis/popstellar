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
	"student20_pop/event"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
	"student20_pop/security"
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

/* processes what is received from the websocket */
func (w *Witness) HandleWholeMessage(receivedMsg []byte, userId int) (message_, channel_, responseToSender []byte) {
	// in case the message is already an answer message (positive ack or error), ignore and answer noting to avoid falling into infinite error loops
	isAnswer, err := filterAnswers(receivedMsg)
	if err != nil {
		return nil, nil, parser.ComposeResponse(lib.ErrIdNotDecoded, nil, message.Query{})
	}
	if isAnswer {
		return nil, nil, nil
	}

	query, err := parser.ParseQuery(receivedMsg)
	if err != nil {
		return nil, nil, parser.ComposeResponse(lib.ErrIdNotDecoded, nil, query)
	}

	var history []byte = nil
	var msg []byte = nil
	var channel []byte = nil

	switch query.Method {
	case "publish":
		msg, channel, err = w.handlePublish(query)
	case "message":
		msg, channel, err = w.handleMessage(query)
	case "subscribe", "unsubscribe", "catchup":
		// Even though witness do nothing for some methods, it should not return an error
		return nil, nil, nil
	default:
		msg, channel, err = nil, nil, lib.ErrRequestDataInvalid
	}

	return msg, channel, parser.ComposeResponse(err, history, query)
}

func (w *Witness) handlePublish(query message.Query) (messsage, channel []byte, err error) {
	return nil, nil, lib.ErrInvalidAction //a witness cannot handle a publish request for now
}

func (w *Witness) handleMessage(query message.Query) (returnMessage, channel []byte, err error) {
	params, errs := parser.ParseParamsIncludingMessage(query.Params)
	if errs != nil {
		return nil, nil, lib.ErrRequestDataInvalid
	}

	msg, errs := parser.ParseMessage(params.Message)
	if errs != nil {
		return nil, nil, lib.ErrRequestDataInvalid
	}

	data := message.Data{}
	base64Text := make([]byte, b64.StdEncoding.DecodedLen(len(msg.Data)))

	l, errs := b64.StdEncoding.Decode(base64Text, msg.Data)
	if errs != nil {
		return nil, nil, errs
	}

	errs = json.Unmarshal(base64Text[:l], &data)
	if errs != nil {
		return nil, nil, lib.ErrRequestDataInvalid
	}

	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "create":
			return w.handleCreateLAO(msg, params.Channel, query)
		case "update_properties":
			return w.handleUpdateProperties(msg, params.Channel, query)
		case "state":
			return w.handleLAOState(msg, params.Channel, query)
		default:
			return nil, nil, lib.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			return w.handleWitnessMessage(msg, params.Channel, query)
		default:
			return nil, nil, lib.ErrInvalidAction
		}
	case "roll call":
		switch data["action"] {
		case "create":
			return w.handleCreateRollCall(msg, params.Channel, query)
		case "state":
			return nil, nil, lib.ErrNotYetImplemented
		default:
			return nil, nil, lib.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			return nil, nil, lib.ErrNotYetImplemented
		case "state":
			return nil, nil, lib.ErrNotYetImplemented
		default:
			return nil, nil, lib.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create":
			return nil, nil, lib.ErrNotYetImplemented
		case "state":
			return nil, nil, lib.ErrNotYetImplemented
		default:
			return nil, nil, lib.ErrInvalidAction
		}
	default:
		return nil, nil, lib.ErrRequestDataInvalid
	}
}

/*handles the creation of an LAO*/
func (w *Witness) handleCreateLAO(msg message.Message, chann string, query message.Query) (message, channel []byte, err error) {
	if chann != "/root" {
		return nil, nil, lib.ErrInvalidResource
	}

	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		return nil, nil, lib.ErrInvalidResource
	}

	if !security.LAOIsValid(data, true) {
		return nil, nil, lib.ErrInvalidResource
	}

	errs = db.CreateMessage(msg, chann, w.database)
	if errs != nil {
		return nil, nil, errs
	}

	lao := event.LAO{
		ID:            string(data.ID),
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: string(data.Organizer),
		Witnesses:     lib.ConvertSliceSliceByteToSliceString(data.Witnesses),
	}
	errs = db.CreateChannel(lao, w.database)

	return nil, nil, errs
}

/*witness does not yet send stuff to channel*/
func (w *Witness) handleUpdateProperties(msg message.Message, chann string, query message.Query) (message, channel []byte, err error) {
	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		return nil, nil, lib.ErrInvalidResource
	}
	if !security.LAOIsValid(data, false) {
		return nil, nil, lib.ErrInvalidResource
	}

	//stores received message in DB
	errs = db.CreateMessage(msg, chann, w.database)
	if errs != nil {
		return nil, nil, err
	}

	return nil, nil, errs
}

func (w *Witness) handleWitnessMessage(msg message.Message, chann string, query message.Query) (message, channel []byte, err error) {

	data, errs := parser.ParseDataWitnessMessage(msg.Data)
	if errs != nil {
		return nil, nil, lib.ErrInvalidResource
	}

	//stores received message in DB
	errs = db.CreateMessage(msg, chann, w.database)
	if errs != nil {
		return nil, nil, errs
	}

	sendMsg := db.GetMessage([]byte(chann), []byte(data.Message_id), w.database)
	if sendMsg == nil {
		fmt.Printf("no message with ID %v in the database", data.Message_id)
		return nil, nil, lib.ErrInvalidResource
	}
	storedMessage, errs := parser.ParseMessage(sendMsg)

	if errs != nil {
		fmt.Printf("unable to unmarshall the message stored in the database")
		return nil, nil, lib.ErrDBFault
	}

	errs = security.VerifySignature(msg.Sender, storedMessage.Data, data.Signature)
	if errs != nil {
		return nil, nil, lib.ErrInvalidResource
	}

	//adds the signature to signature list
	storedMessage.WitnessSignatures = append(storedMessage.WitnessSignatures, data.Signature)

	//update message in DB
	errs = db.UpdateMessage(storedMessage, chann, w.database)

	return nil, nil, errs
}

func (w *Witness) handleLAOState(msg message.Message, chann string, query message.Query) (message, channel []byte, err error) {
	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		return nil, nil, lib.ErrInvalidResource
	}

	if !security.LAOIsValid(data, false) {
		return nil, nil, lib.ErrInvalidResource
	}

	//TODO correct usage of VerifyWitnessSignatures
	errs = security.VerifyWitnessSignatures(nil, msg.WitnessSignatures, msg.Sender)
	if errs != nil {
		return nil, nil, lib.ErrRequestDataInvalid
	}

	lao := event.LAO{
		ID:            string(data.ID),
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: string(data.Organizer),
		Witnesses:     lib.ConvertSliceSliceByteToSliceString(data.Witnesses),
	}

	errs = db.UpdateChannel(lao, w.database)

	return nil, nil, errs
}

func (w *Witness) handleCreateRollCall(msg message.Message, chann string, query message.Query) (message, channel []byte, err error) {

	data, errs := parser.ParseDataCreateRollCall(msg.Data)
	if errs != nil {
		return nil, nil, lib.ErrInvalidResource
	}

	if !security.RollCallCreatedIsValid(data, msg) {
		return nil, nil, lib.ErrInvalidResource
	}

	rollCall := event.RollCall{
		ID:       string(data.ID),
		Name:     data.Name,
		Creation: data.Creation,
		Location: data.Location,
		Start:    data.Start,
		End:      data.End,
	}

	errs = db.CreateChannel(rollCall, w.database)
	if errs != nil {
		return nil, nil, errs
	}

	errs = db.CreateMessage(msg, chann, w.database)
	if errs != nil {
		return nil, nil, errs
	}

	return nil, nil, errs
}
