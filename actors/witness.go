package actors

// This class implements the functions an organizer provides. It stores messages in the database using the db package.
// Currently it does not do send response to channels (only ack messages as defined in the protocol). Implementation not
// finished as it is not the most important class for now. Does not support publish method.

import (
	b64 "encoding/base64"
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

// Witness implements how the Witness's backend server is supposed to behave. It currently only acts as a remote
// database, storing received messages.
type Witness struct {
	PublicKey string
	database  string
	channels  map[string][]int
}

// NewWitness is the constructor for the Witness struct. db should be a a file path (existing or not) and pkey is
// the Witness's public key.
func NewWitness(pkey string, db string) *Witness {
	return &Witness{
		PublicKey: pkey,
		database:  db,
		channels:  make(map[string][]int),
	}
}

// HandleReceivedMessage processes the received message. It parses it and calls sub-handler functions depending
// on the message's method field.
func (w *Witness) HandleReceivedMessage(receivedMsg []byte, userId int) (msgAndChannel []lib.MessageAndChannel, responseToSender []byte) {
	// if the message is an answer message just ignore it
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
	var msg []lib.MessageAndChannel = nil
	var history []byte = nil

	switch query.Method {
	case "publish":
		msg, err = w.handlePublish(query)
	case "broadcast":
		msg, err = w.handleBroadcast(query)
	case "subscribe", "unsubscribe", "catchup":
		// Even though witness do nothing for some methods, it should not return an error
		return nil, parser.ComposeResponse(nil, receivedMsg, query)
	default:
		err = lib.ErrRequestDataInvalid
	}

	return msg, parser.ComposeResponse(err, history, query)
}

// handlePublish is called by HandleReceivedMessage and is only here to implement Actor's interface. Currently a Witness
// only supports messages with method "message", "subscribe" and "unsubscribe".
func (w *Witness) handlePublish(query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	return nil, lib.ErrInvalidAction //a witness cannot handle a publish request for now
}

// handleBroadcast is the function that handles a received message with the method "message". It is called from
// HandleReceivedMessage. It parses the received message, and delegates the handling to sub-handler functions, depending
// on the "object" and "action" fields.
func (w *Witness) handleBroadcast(query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	params, errs := parser.ParseParams(query.Params)
	if errs != nil {
		return nil, lib.ErrRequestDataInvalid
	}

	msg, errs := parser.ParseMessage(params.Message)
	if errs != nil {
		return nil, lib.ErrRequestDataInvalid
	}

	data := message.Data{}
	base64Text := make([]byte, b64.StdEncoding.DecodedLen(len(msg.Data)))

	l, errs := b64.StdEncoding.Decode(base64Text, msg.Data)
	if errs != nil {
		return nil, errs
	}

	errs = json.Unmarshal(base64Text[:l], &data)
	if errs != nil {
		return nil, lib.ErrRequestDataInvalid
	}

	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "create":
			return w.handleCreateLAO(msg, params.Channel, query)
		case "update_properties":
			return w.handleUpdateProperties(msg, params.Channel, query)
		case "state":
			return w.handleLAOState(msg)
		default:
			return nil, lib.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			return w.handleWitnessMessage(msg, params.Channel, query)
		default:
			return nil, lib.ErrInvalidAction
		}
	case "roll_call":
		switch data["action"] {
		case "create":
			return w.handleCreateRollCall(msg, params.Channel, query)
		case "state":
			return nil, lib.ErrNotYetImplemented
		default:
			return nil, lib.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			return nil, lib.ErrNotYetImplemented
		case "state":
			return nil, lib.ErrNotYetImplemented
		default:
			return nil, lib.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create":
			return nil, lib.ErrNotYetImplemented
		case "state":
			return nil, lib.ErrNotYetImplemented
		default:
			return nil, lib.ErrInvalidAction
		}
	default:
		return nil, lib.ErrRequestDataInvalid
	}
}

// handleCreateLAO is the function that handles the creation of a LAO. It checks the message's validity,
// creates a new Channel in the Witness's database and stores the received message
func (w *Witness) handleCreateLAO(msg message.Message, chann string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	if chann != "/root" {
		return nil, lib.ErrInvalidResource
	}

	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}

	if !security.LAOIsValid(data, true) {
		return nil, lib.ErrInvalidResource
	}

	errs = db.CreateMessage(msg, chann, w.database)
	if errs != nil {
		return nil, errs
	}

	lao := event.LAO{
		ID:            string(data.ID),
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: string(data.Organizer),
		Witnesses:     lib.ArrayArrayByteToArrayString(data.Witnesses),
	}
	errs = db.CreateChannel(lao, w.database)

	return nil, errs
}

// handleUpdateProperties handles a received message with field object and action set respectively to "lao" and
// "update_properties". It checks the message's validity and stores it in the Witness's database.
func (w *Witness) handleUpdateProperties(msg message.Message, chann string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}
	if !security.LAOIsValid(data, false) {
		return nil, lib.ErrInvalidResource
	}

	//stores received message in DB
	errs = db.CreateMessage(msg, chann, w.database)
	if errs != nil {
		return nil, err
	}

	return nil, errs
}

// handleWitnessMessage handles a received message with fields object and action set respectively to "message" and
// "witness". It checks the message's validity, retrieves the message to be signed from the database,
// verifies signature's correctness and eventually appends the new signature to the original message, before writing it
// back to the database, along with received message.
func (w *Witness) handleWitnessMessage(msg message.Message, chann string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	data, errs := parser.ParseDataWitnessMessage(msg.Data)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}

	//stores received message in DB
	errs = db.CreateMessage(msg, chann, w.database)
	if errs != nil {
		return nil, errs
	}

	sendMsg := db.GetMessage([]byte(chann), data.MessageId, w.database)
	if sendMsg == nil {
		log.Printf("no message with ID %v in the database", data.MessageId)
		return nil, lib.ErrInvalidResource
	}
	storedMessage, errs := parser.ParseMessage(sendMsg)

	if errs != nil {
		log.Printf("unable to unmarshall the message stored in the database")
		return nil, lib.ErrDBFault
	}

	errs = security.VerifySignature(msg.Sender, storedMessage.Data, data.Signature)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}

	//adds the signature to signature list
	storedMessage.WitnessSignatures = append(storedMessage.WitnessSignatures, data.Signature)

	//update message in DB
	errs = db.UpdateMessage(storedMessage, chann, w.database)

	return nil, errs
}

// handleLAOState is the function that handles a received message with fields object and action set respectively to
// "lao" and "state". It verify that the message is correct, retrieves the LAO to update and updates it in the Witness's
// database, and stores the received message.
func (w *Witness) handleLAOState(msg message.Message) (msgAndChannel []lib.MessageAndChannel, err error) {
	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}

	if !security.LAOIsValid(data, false) {
		return nil, lib.ErrInvalidResource
	}

	errs = security.VerifyWitnessSignatures(data.Witnesses, msg.WitnessSignatures, msg.Sender)
	if errs != nil {
		return nil, lib.ErrRequestDataInvalid
	}

	lao := event.LAO{
		ID:            string(data.ID),
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: string(data.Organizer),
		Witnesses:     lib.ArrayArrayByteToArrayString(data.Witnesses),
	}

	errs = db.UpdateChannel(lao, w.database)

	return nil, errs
}

// handleCreateRollCall is the function that handles a received message with fields object and action set respectively
// to "roll_call" and "create". It  verifies the message's validity, creates a new channel in the Witness's database and
// stores the received message.
func (w *Witness) handleCreateRollCall(msg message.Message, chann string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	if chann != "/root" {
		return nil, lib.ErrInvalidResource
	}

	data, errs := parser.ParseDataCreateRollCall(msg.Data)
	if errs != nil {
		return nil, lib.ErrInvalidResource
	}

	laoId := strings.TrimPrefix("/root", chann)
	if !security.RollCallCreatedIsValid(data, laoId) {
		return nil, lib.ErrInvalidResource
	}

	rollCall := event.RollCall{
		ID:       string(data.ID),
		Name:     data.Name,
		Creation: data.Creation,
		Location: data.Location,
		Start:    data.Start,
	}

	errs = db.CreateChannel(rollCall, w.database)
	if errs != nil {
		return nil, errs
	}

	errs = db.CreateMessage(msg, chann, w.database)
	if errs != nil {
		return nil, errs
	}

	return nil, errs
}
