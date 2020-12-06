/*
This class implements the functions an organizer provides. It stores messages in the database using the db package.
Currently it does not do send response to channels (only ack messages as defined in the protocol) as we decided thought
the front-end should implement "witness a message".
*/

package actors

import (
	b64 "encoding/base64"
	"encoding/json"
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

/** processes what is received from the WebSocket
 * Currently only supports updateProperties
 * msg : receivedMessage
 * returns, in order :
 * message to send on channel, or nil
 * channel for the message, or nil
 * response to the sender, or nil
 */
func (w *Witness) HandleWholeMessage(msg []byte, userId int) ([]byte, []byte, []byte) {
	generic, err := define.AnalyseGeneric(msg)
	if err != nil {
		return nil, nil, define.CreateResponse(define.ErrRequestDataInvalid, nil, generic)

	}

	var history []byte = nil
	var message []byte = nil
	var channel []byte = nil

	switch generic.Method {
	case "publish":
		message, channel, err = w.handlePublish(generic)
	default:
		message, channel, err = nil, nil, define.ErrRequestDataInvalid
	}

	return message, channel, define.CreateResponse(err, history, generic)
}

/** @returns, in order
 * message
 * channel
 * error
 */
func (w *Witness) handlePublish(generic define.Generic) ([]byte, []byte, error) {
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

		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			//return w.handleCreateMeeting(message, params.Channel, generic)
		case "state":

		default:
			return nil, nil, define.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create":
			//return w.handleCreatePoll(message, params.Channel, generic)
		case "state":

		default:
			return nil, nil, define.ErrInvalidAction
		}
	default:
		return nil, nil, define.ErrRequestDataInvalid
	}

	return nil, nil, nil
}

func (w *Witness) handleCreateLAO(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {
	if channel != "/root" {
		return nil, nil, define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}

	if !define.LAOCreatedIsValid(data, message) {
		return nil, nil, define.ErrInvalidResource
	}

	canalLAO := channel + data.ID

	err = db.CreateMessage(message, canalLAO, w.database)
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
	err = db.CreateChannel(lao, w.database)

	return nil, nil, err
}

/*witness does not yet send stuff to channel*/
func (w *Witness) handleUpdateProperties(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {
	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}
	if !define.LAOCreatedIsValid(data, message) {
		return nil, nil, define.ErrInvalidResource
	}

	//stores received message in DB
	canalLAO := channel + data.ID
	err = db.CreateMessage(message, canalLAO, w.database)
	if err != nil {
		return nil, nil, err
	}

	//TODO create a response signing the message -- or not ? should it be front-end ?

	return nil, nil, err
}

func (w *Witness) handleWitnessMessage(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {

	//shall a witness increment count on base message as well ?

	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}
	if !define.LAOCreatedIsValid(data, message) {
		return nil, nil, define.ErrInvalidResource
	}

	//stores received message in DB
	canalLAO := channel + data.ID
	err = db.CreateMessage(message, canalLAO, w.database)
	return nil, nil, err
}

func (w *Witness) handleLAOState(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {
	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return nil, nil, define.ErrInvalidResource
	}

	if !define.LAOStateIsValid(data, message) {
		return nil, nil, define.ErrInvalidResource
	}

	//TODO is the action valid ? was there enough witness signatures ?

	lao := define.LAO{
		ID:            data.ID,
		Name:          data.Name,
		Creation:      data.Creation,
		LastModified:  data.Last_modified,
		OrganizerPKey: data.Organizer,
		Witnesses:     data.Witnesses,
	}

	err = db.UpdateChannel(lao, w.database)

	return nil, nil, err
}

func (w *Witness) handleCreateRollCall(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error) {
	return nil, nil, db.CreateMessage(message, channel, w.database)
}
