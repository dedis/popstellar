package actors


import (
	"encoding/json"
	"github.com/boltdb/bolt"
	"student20_pop/define"
)



func NewOrganizer() *define.Organizer {
	dbtmp, err := OpenDB(db.OrgDatabase)
	if err != nil {
		log.Fatal("couldn't start a new db")
	}


	o := &Organizer{
		db:		dbtmp,
	}
}

func (o *define.Organizer) CloseDB() {
	o.db.CloseDB()
}

// Test json input to create LAO:
//  careful with base64 needed to remove
//  careful with comma after witnesses[] and witnesses_signatures[] needed to remove

// Param msg = receivedMessage
// output by setting h.responseToSender and h.broadcast
func (org *define.Organizer) HandleWholeMessage(msg []byte, userId int, h *define.Hub) {
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

func (h *define.Hub) handleSubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return define.ErrRequestDataInvalid
	}
	return db.Subscribe(userId, []byte(params.Channel))
}

func (h *define.Hub) handleUnsubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return define.ErrRequestDataInvalid
	}
	return db.Unsubscribe(userId, []byte(params.Channel))
}

func (h *define.Hub) handlePublish(generic define.Generic) error {
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
			msg, _ := json.Marshal(generic)
			return h.handleUpdateProperties(params.Channel, msg)
		case "state":

		default:
			return define.ErrInvalidAction
		}

	case "message":
		switch data["action"] {
		case "witness":
			msg, err1 := json.Marshal(generic)
			if err1 != nil {
				return err1
			}
			err := h.handleWitnessMessage(params.Channel, msg)
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

func (h *define.Hub) handleCreateLAO(message define.Message, canal string, generic define.Generic) error {

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

	err = db.StoreMessage(message, canalLAO)
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
	err = db.CreateObject(lao)
	if err != nil {
		return err
	}

	return h.finalizeHandling(message, canal, generic)
}

func (h *define.Hub) handleCreateRollCall(message define.Message, canal string, generic define.Generic) error {
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
	err = db.CreateObject(event)
	if err != nil {
		return err
	}
	return h.finalizeHandling(message, canal, generic)
}

func (h *define.Hub) handleCreateMeeting(message define.Message, canal string, generic define.Generic) error {

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
	err = db.CreateObject(event)
	if err != nil {
		return err
	}
	return h.finalizeHandling(message, canal, generic)
}

func (h *define.Hub) finalizeHandling(message define.Message, canal string, generic define.Generic) error {
	h.message = define.CreateBroadcastMessage(message, generic)
	h.channel = []byte(canal)
	return nil
}

func (h *define.Hub) handleCreatePoll(message define.Message, canal string, generic define.Generic) error {

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
	err = db.CreateObject(event)
	if err != nil {
		return err
	}
	return h.finalizeHandling(message, canal, generic)
}
func (h *define.Hub) handleMessage(msg []byte, userId int) error {

	return nil
}

// This is organizer implementation. If Witness, should return a witness msg on object
func (h *define.Hub) handleUpdateProperties(canal string, msg []byte) error {
	h.message = msg
	h.channel = []byte(canal)
	return db.WriteMessage(msg, true)
}

func (h *define.Hub) handleWitnessMessage(canal string, msg []byte) error {
	//TODO verify signature correctness
	// decrypt msg and compare with hash of "local" data

	//add signature to already stored message:
	//extract messageID and Witness signature from received message
	r_g, err := define.AnalyseGeneric(msg)
	r_p, err := define.AnalyseParamsFull(r_g.Params)
	r_d, err := define.AnalyseMessage(r_p.Message)
	if err != nil {
		return define.ErrRequestDataInvalid
	}
	//retrieve message to sign from database
	toSign, err := db.GetMessage([]byte(canal), []byte(r_d.MessageID))
	//extract signature list
	g := define.Generic{}
	p := define.ParamsFull{}
	m := define.Message{}
	err = json.Unmarshal(toSign, &g)
	err = json.Unmarshal(g.Params, &p)
	err = json.Unmarshal(p.Message, &m)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	//if message was already signed by this witness, returns an error
	_, found := define.FindStr(m.WitnessSignatures, r_d.Signature)
	if found {
		return define.ErrResourceAlreadyExists
	}

	m.WitnessSignatures = append(m.WitnessSignatures, r_d.Signature)
	//build the string back
	str, err := json.Marshal(m)
	p.Message = str
	str, err = json.Marshal(p)
	g.Params = str
	str, err = json.Marshal(g)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	//store received message in DB and update "LAOUpdateProperties" message in DB
	err = db.WriteMessage(str, false)
	err = db.WriteMessage(msg, true)
	if err != nil {
		return define.ErrDBFault
	}

	//broadcast received message
	h.message = msg
	h.channel = []byte(canal)
	return nil
}

func (h *define.Hub) handleCatchup(generic define.Generic) ([]byte, error) {
	// TODO maybe pass userId as an arg in order to check access rights later on?
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		return nil, define.ErrRequestDataInvalid
	}
	history := db.GetChannelFromID([]byte(params.Channel))

	return history, nil
}