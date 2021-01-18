package actors

// This class implements the functions an organizer provides. It stores messages in the database using the db package.
// Currently it does not do send response to channels (only ack messages as defined in the protocol). Implementation not
// finished as it is not the most important class for now. Does not support publish method.

import (
	"bytes"
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

// witness implements how the witness's backend server is supposed to behave. It currently only acts as a remote
// database, storing received messages. The database field is only the name of the file the database is stored to.
type witness struct {
	PublicKey string
	database  string
	channels  map[string][]int
}

// NewWitness is the constructor for the witness struct. db should be a a file path (existing or not) and pkey is
// the witness's public key.
func NewWitness(pkey string, db string) *witness {
	return &witness{
		PublicKey: pkey,
		database:  db,
		channels:  make(map[string][]int),
	}
}

// HandleReceivedMessage processes the received message. It parses it and calls sub-handler functions depending
// on the message's method field.
func (w *witness) HandleReceivedMessage(receivedMsg []byte, userId int) (msgAndChannel []lib.MessageAndChannel, responseToSender []byte) {
	// if the message is an answer message just ignore it
	isAnswer, err := parser.FilterAnswers(receivedMsg)
	if err != nil {
		log.Printf("unable to parse received message. Cannot decide whether message is answer or is not.")
		return nil, parser.ComposeResponse(lib.ErrIdNotDecoded, nil, message.Query{})
	}
	if isAnswer {
		return nil, nil
	}

	query, err := parser.ParseQuery(receivedMsg)
	if err != nil {
		log.Printf("Unable to parse received message as a query")
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

// handlePublish is called by HandleReceivedMessage and is only here to implement Actor's interface. Currently a witness
// only supports messages with method "message", "subscribe" and "unsubscribe".
func (w *witness) handlePublish(query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	return nil, lib.ErrInvalidAction //a witness cannot handle a publish request for now
}

// handleBroadcast is the function that handles a received message with the method "message". It is called from
// HandleReceivedMessage. It parses the received message, and delegates the handling to sub-handler functions, depending
// on the "object" and "action" fields.
func (w *witness) handleBroadcast(query message.Query) (msgAndChannel []lib.MessageAndChannel, err_ error) {
	params, errs := parser.ParseParams(query.Params)
	if errs != nil {
		log.Printf("Unable to parse received message as a query")
		return nil, lib.ErrRequestDataInvalid
	}

	msg, errs := parser.ParseMessage(params.Message)
	if errs != nil {
		log.Printf("Unable to parse received message as a message")
		return nil, lib.ErrRequestDataInvalid
	}

	data := message.Data{}
	dataDecoded := make([]byte, b64.StdEncoding.DecodedLen(len(msg.Data)))

	l, errs := b64.StdEncoding.Decode(dataDecoded, msg.Data)
	if errs != nil {
		log.Printf("Tried to decode invalid B64 string: %s", msg.Data)
		return nil, lib.ErrEncodingFault
	}

	errs = json.Unmarshal(dataDecoded[:l], &data)
	if errs != nil {
		log.Printf("could not parse decoded query.Data into a message.Data structure")
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
		case "create", "state":
			return nil, lib.ErrNotYetImplemented
		default:
			return nil, lib.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create", "state":
			return nil, lib.ErrNotYetImplemented
		default:
			return nil, lib.ErrInvalidAction
		}
	default:
		return nil, lib.ErrRequestDataInvalid
	}
}

// handleCreateLAO is the function that handles the creation of a LAO. It checks the message's validity,
// creates a new Channel in the witness's database and stores the received message
func (w *witness) handleCreateLAO(msg message.Message, channel string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	if channel != "/root" {
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
	errs = db.CreateChannel(lao, w.database)
	if errs != nil {
		log.Printf("An error occured, unable to create channel in the database")
		return nil, errs
	}

	errs = db.CreateMessage(msg, channel, w.database)
	if errs != nil {
		log.Printf("An error occured, could not store message to the database")
		return nil, errs
	}

	return nil, nil
}

// handleUpdateProperties handles a received message with field object and action set respectively to "lao" and
// "update_properties". It checks the message's validity and stores it in the witness's database.
func (w *witness) handleUpdateProperties(msg message.Message, channel string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		log.Printf("could not parse received data in a message.DataCreateLAO structure")
		return nil, lib.ErrInvalidResource
	}
	if !security.LAOIsValid(data, false) {
		log.Printf("Invalid changes were requested.")
		return nil, lib.ErrInvalidResource
	}

	//store received message in DB
	errs = db.CreateMessage(msg, channel, w.database)
	if errs != nil {
		log.Printf("unable to store received message in the database")
		return nil, err
	}

	return nil, errs
}

// handleWitnessMessage handles a received message with fields object and action set respectively to "message" and
// "witness". It checks the message's validity, retrieves the message to be signed from the database,
// verifies signature's correctness and eventually appends the new signature to the original message, before writing it
// back to the database, along with received message.
func (w *witness) handleWitnessMessage(msg message.Message, channel string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err_ error) {
	data, err := parser.ParseDataWitnessMessage(msg.Data)
	if err != nil {
		log.Printf("could not parse received data in a message.DataWitnessMessage structure")
		return nil, lib.ErrInvalidResource
	}

	sendMsg := db.GetMessage([]byte(channel), data.MessageId, w.database)
	if sendMsg == nil {
		log.Printf("no message with ID %v in the database", data.MessageId)
		return nil, lib.ErrInvalidResource
	}
	storedMessage, err := parser.ParseMessage(sendMsg)
	if err != nil {
		log.Printf("unable to unmarshall the message stored in the database")
		return nil, lib.ErrDBFault
	}

	//check that the field message_id in the dataWitnessMessage is correct
	//can seem stupid as if we detected the message with its message_id it
	//should be the correct one but useful to know if the db has been corrupted
	signatureB64 := b64.StdEncoding.EncodeToString(data.Signature)
	elementsToHashForMessageId := []string{b64.StdEncoding.EncodeToString(storedMessage.Data), signatureB64}
	messageIdRecomputed := security.HashItems(elementsToHashForMessageId)
	if !bytes.Equal(storedMessage.MessageId, messageIdRecomputed) {
		log.Printf("message_id of witnessMessage invalid: %v should be: %v", string(data.MessageId), string(messageIdRecomputed))
		return nil, lib.ErrInvalidResource
	}

	err = security.VerifySignature(msg.Sender, storedMessage.Data, data.Signature)
	if err != nil {
		log.Printf("Invalid signature in received witness message.")
		return nil, lib.ErrInvalidResource
	}

	//adds the signature to signature list
	// in the future should check if not already in the list
	storedMessage.WitnessSignatures = append(storedMessage.WitnessSignatures, data.Signature)

	//update message in DB
	err = db.UpdateMessage(storedMessage, channel, w.database)
	if err != nil {
		log.Printf("Unable to update signature list of message in the database")
		return nil, err
	}

	//stores received message in DB
	err = db.CreateMessage(msg, channel, w.database)
	if err != nil {
		log.Printf("could not store received message in the database")
		return nil, err
	}

	return nil, nil
}

// handleLAOState is the function that handles a received message with fields object and action set respectively to
// "lao" and "state". It verify that the message is correct, retrieves the LAO to update and updates it in the witness's
// database, and stores the received message.
func (w *witness) handleLAOState(msg message.Message) (msgAndChannel []lib.MessageAndChannel, err error) {
	data, errs := parser.ParseDataCreateLAO(msg.Data)
	if errs != nil {
		log.Printf("could not parse recieved data in a message.CreateLAO structure")
		return nil, lib.ErrInvalidResource
	}

	if !security.LAOIsValid(data, false) {
		log.Printf("LAO state update is not valid. Changes aborted")
		return nil, lib.ErrInvalidResource
	}

	errs = security.VerifyWitnessSignatures(data.Witnesses, msg.WitnessSignatures, msg.Sender)
	if errs != nil {
		log.Printf("not enough valid witness signatures to accept the state update")
		return nil, lib.ErrRequestDataInvalid
	}

	lao := event.LAO{
		ID:            string(data.ID),
		Name:          data.Name,
		Creation:      data.Creation,
		OrganizerPKey: string(data.Organizer),
		Witnesses:     lib.NestedByteArrayToStringArray(data.Witnesses),
	}

	errs = db.UpdateChannel(lao, w.database)
	if errs != nil {
		log.Printf("could not update the channel in the database")
		return nil, errs
	}

	errs = db.CreateMessage(msg, "/root/"+lao.ID, w.database)
	if errs != nil {
		log.Printf("could not store received message in the database")
		return nil, errs
	}

	return nil, errs
}

// handleCreateRollCall is the function that handles a received message with fields object and action set respectively
// to "roll_call" and "create". It  verifies the message's validity, creates a new channel in the witness's database and
// stores the received message.
func (w *witness) handleCreateRollCall(msg message.Message, channel string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error) {
	if strings.HasPrefix(channel, "/root/") {
		log.Printf("Channel name has to begin with /root/")
		return nil, lib.ErrInvalidResource
	}

	data, errs := parser.ParseDataCreateRollCall(msg.Data)
	if errs != nil {
		log.Printf("could not parse received data into a message.DataCreateRollCall structure")
		return nil, lib.ErrInvalidResource
	}

	laoID := strings.TrimPrefix(channel, "/root/")
	if !security.RollCallCreatedIsValid(data, laoID) {
		log.Printf("Roll Call data not valid. Roll call not created")
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
		log.Printf("failed to create a new channel in the database")
		return nil, errs
	}

	errs = db.CreateMessage(msg, channel, w.database)
	if errs != nil {
		log.Printf("failed to store a new message in the database")
		return nil, errs
	}

	return nil, nil
}
