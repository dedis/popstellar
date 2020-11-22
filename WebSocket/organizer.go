package WebSocket


import (
	"github.com/boltdb/bolt"
	"student20_pop/define"
	"student20_pop/db"
	"log"
)

type Organizer struct {
	//Database instance
	db *bolt.DB
}


func NewOrganizer() *Organizer {
	dbtmp, err := db.OpenDB(db.Database)
	if err != nil {
		log.Fatal("couldn't start a new db")
	}


	o := &Organizer{
		db:		dbtmp,
	}

	return o
}

func (o *Organizer) CloseDB() {
	o.db.Close()
}

// Test json input to create LAO:
//  careful with base64 needed to remove
//  careful with comma after witnesses[] and witnesses_signatures[] needed to remove

// Param msg = receivedMessage
// output by setting h.responseToSender and h.broadcast
func (h *hub) HandleWholeMessage(msg []byte, userId int) {
	generic, err := define.AnalyseGeneric(msg)
	if err != nil {
		err = define.ErrRequestDataInvalid
		h.responseToSender = define.CreateResponse(err, nil, generic)
		return
	}

	var history []byte = nil

	switch generic.Method {
	case "subscribe":
		err = h.handleSubscribe(generic, userId)
	case "unsubscribe":
		err = h.handleUnsubscribe(generic, userId)
	case "publish":
		err = h.handlePublish(generic)
	//case "message": err = h.handleMessage() // Potentially, we never receive a "message" and only output "message" after a "publish" in order to broadcast. Or they are only notification, and we just want to check that it was a success
	case "catchup":
		history, err = h.handleCatchup(generic)
	default:
		err = define.ErrRequestDataInvalid
	}

	h.responseToSender = define.CreateResponse(err, history, generic)
}

func (h *hub) handleSubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return define.ErrRequestDataInvalid
	}
	return db.Subscribe(userId, []byte(params.Channel))
}

func (h *hub) handleUnsubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return define.ErrRequestDataInvalid
	}
	return db.Unsubscribe(userId, []byte(params.Channel))
}

func (h *hub) handlePublish(generic define.Generic) error {
	params, err := define.AnalyseParamsFull(generic.Params)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	message, err := define.AnalyseMessage(params.Message)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	data, err := define.AnalyseData(message.Data)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "create":
			return h.handleCreateLAO(message, params.Channel, generic)
		case "update_properties":
			return h.handleUpdateProperties(message, params.Channel, generic)
		case "state":

		default:
			return define.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			err := h.handleWitnessMessage(message, params.Channel, generic)
			if err != nil {
				return err
			}
			if len(message.WitnessSignatures) == SIG_TRESHOLD-1 {
				//TODO: update state and send state broadcast
			}
			return err
		default:
			return define.ErrInvalidAction
		}
	case "roll call":
		switch data["action"] {
		case "create":
			return h.handleCreateRollCall(message, params.Channel, generic)
		case "state":

		default:
			return define.ErrInvalidAction
		}
	case "meeting":
		switch data["action"] {
		case "create":
			return h.handleCreateMeeting(message, params.Channel, generic)
		case "state":

		default:
			return define.ErrInvalidAction
		}
	case "poll":
		switch data["action"] {
		case "create":
			return h.handleCreatePoll(message, params.Channel, generic)
		case "state":

		default:
			return define.ErrInvalidAction
		}
	default:
		return define.ErrRequestDataInvalid
	}

	return nil
}

func (h *hub) handleCreateLAO(message define.Message, canal string, generic define.Generic) error {

	if canal != "/root" {
		return define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateLAO(message.Data)
	if err != nil {
		return define.ErrInvalidResource
	}

	err = define.LAOCreatedIsValid(data, message)
	if err != nil {
		return err
	}

	canalLAO := canal + data.ID

	err = db.CreateMessage(message, canalLAO)
	if err != nil {
		return err
	}

	lao := define.LAO{
		ID:            data.ID,
		Name:          data.Name,
		Creation:      data.Creation,
		LastModified:  data.LastModified,
		OrganizerPKey: data.OrganizerPKey,
		Witnesses:     data.Witnesses,
	}
	err = db.CreateChannel(lao)
	if err != nil {
		return err
	}

	return h.finalizeHandling(message, canal, generic)
}

func (h *hub) handleCreateRollCall(message define.Message, canal string, generic define.Generic) error {
	if canal != "/root" {
		return define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateRollCall(message.Data)
	if err != nil {
		return define.ErrInvalidResource
	}

	err = define.RollCallCreatedIsValid(data, message)
	if err != nil {
		return err
	}

	// don't need to check for validity if we use json schema
	event := define.RollCall{ID: data.ID,
		Name:         data.Name,
		Creation:     data.Creation,
		LastModified: data.LastModified,
		Location:     data.Location,
		Start:        data.Start,
		End:          data.End,
		Extra:        data.Extra,
	}
	err = db.CreateChannel(event)
	if err != nil {
		return err
	}
	return h.finalizeHandling(message, canal, generic)
}

func (h *hub) handleCreateMeeting(message define.Message, canal string, generic define.Generic) error {

	if canal == "/root" {
		return define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreateMeeting(message.Data)
	if err != nil {
		return define.ErrInvalidResource
	}

	// don't need to check for validity if we use json schema
	event := define.Meeting{ID: data.ID,
		Name:         data.Name,
		Creation:     data.Creation,
		LastModified: data.LastModified,
		Location:     data.Location,
		Start:        data.Start,
		End:          data.End,
		Extra:        data.Extra,
	}
	err = db.CreateChannel(event)
	if err != nil {
		return err
	}
	return h.finalizeHandling(message, canal, generic)
}

func (h *hub) handleCreatePoll(message define.Message, canal string, generic define.Generic) error {

	if canal != "0" {
		return define.ErrInvalidResource
	}

	data, err := define.AnalyseDataCreatePoll(message.Data)
	if err != nil {
		return define.ErrInvalidResource
	}

	// don't need to check for validity if we use json schema
	event := define.Poll{ID: data.ID,
		Name:         data.Name,
		Creation:     data.Creation,
		LastModified: data.LastModified,
		Location:     data.Location,
		Start:        data.Start,
		End:          data.End,
		Extra:        data.Extra,
	}
	err = db.CreateChannel(event)
	if err != nil {
		return err
	}
	return h.finalizeHandling(message, canal, generic)
}
func (h *hub) handleMessage(msg []byte, userId int) error {

	return nil
}

// This is organizer implementation. If Witness, should return a witness msg on object
func (h *hub) handleUpdateProperties(message define.Message, canal string, generic define.Generic) error {
	h.message = define.CreateBroadcastMessage(generic)
	h.channel = []byte(canal)
	return db.UpdateMessage(message, canal)
}

func (h *hub) handleWitnessMessage(message define.Message, canal string, generic define.Generic) error {
	//TODO verify signature correctness
	// decrypt msg and compare with hash of "local" data

	//add signature to already stored message:

	//retrieve message to sign from database
	toSign := db.GetMessage([]byte(canal), []byte(message.MessageID))
	if toSign == nil {
		return define.ErrInvalidResource
	}
	

	toSignStruct, err := define.AnalyseMessage(toSign)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	//if message was already signed by this witness, returns an error
	_, found := define.FindStr(toSignStruct.WitnessSignatures, message.Signature)
	if found {
		return define.ErrResourceAlreadyExists
	}

	toSignStruct.WitnessSignatures = append(toSignStruct.WitnessSignatures, message.Signature)


	// update "LAOUpdateProperties" message in DB
	err = db.UpdateMessage(toSignStruct, canal)
	if err != nil {
		return define.ErrDBFault
	}
	//store received message in DB
	err = db.CreateMessage(message, canal)
	if err != nil {
		return define.ErrDBFault
	}

	//broadcast received message
	h.message = define.CreateBroadcastMessage(generic)
	h.channel = []byte(canal)
	return nil
}


func (h *hub) handleCatchup(generic define.Generic) ([]byte, error) {
	// TODO maybe pass userId as an arg in order to check access rights later on?
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return nil, define.ErrRequestDataInvalid
	}
	history := db.GetChannelFromID([]byte(params.Channel))

	return history, nil
}

func (h *hub) finalizeHandling(message define.Message, canal string, generic define.Generic) error {
	h.message = define.CreateBroadcastMessage(generic)
	h.channel = []byte(canal)
	return nil
}