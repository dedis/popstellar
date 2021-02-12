package message

import (
	"crypto/sha256"
	"encoding/json"

	"golang.org/x/xerrors"
)

type Data struct {
	CreateLAOData *CreateLAOData
	UpdateLAOData *UpdateLAOData
	StateLAOData  *StateLAOData

	CreateMeetingData *CreateMeetingData
	StateMeetingData  *StateMeetingData

	CreateRollCallData *CreateRollCallData
	OpenRollCallData   *OpenRollCallData
	CloseRollCallData  *CloseRollCallData

	WitnessMessageData *WitnessMessageData
}

type CreateLAOData struct {
	ID     []byte `json:"id"`
	Object string `json:"object"`
	Action string `json:"action"`

	Name      string      `json:"name"`
	Creation  int64       `json:"creation"`
	Organizer PublicKey   `json:"organizer"`
	Witnesses []PublicKey `json:"witnesses"`
}

func (c *CreateLAOData) setID() error {
	// TODO: calculate hash

	return nil
}

func NewCreateLAOData(name string, creation int64, organizer PublicKey, witnesses []PublicKey) (*Data, error) {
	create := &CreateLAOData{
		Object:    "lao",
		Action:    "create",
		Name:      name,
		Creation:  creation,
		Organizer: organizer,
		Witnesses: witnesses,
	}

	err := create.setID()
	if err != nil {
		return nil, xerrors.Errorf("failed to set ID for CreateLAOData: %v", err)
	}

	return &Data{
		CreateLAOData: create,
	}, nil
}

type UpdateLAOData struct {
	ID     []byte `json:"id"`
	Object string `json:"object"`
	Action string `json:"action"`

	Name         string      `json:"name"`
	LastModified int64       `json:"last_modified"`
	Witnesses    []PublicKey `json:"witnesses"`
}

type StateLAOData struct {
	ID     []byte `json:"id"`
	Object string `json:"object"`
	Action string `json:"action"`

	Name                   string                   `json:"name"`
	LastModified           int64                    `json:"last_modified"`
	Creation               int64                    `json:"creation"`
	Organizer              PublicKey                `json:"organizer"`
	Witnesses              []PublicKey              `json:"witnesses"`
	ModificationID         []byte                   `json:"modification_id"`
	ModificationSignatures []PublicKeySignaturePair `json:"modification_signatures"`
}

type CreateMeetingData struct {
	ID     []byte `json:"id"`
	Object string `json:"object"`
	Action string `json:"action"`

	Name     string `json:"name"`
	Creation int64  `json:"creation"`
	Location string `json:"location"`

	Start int64 `json:"start"`
	End   int64 `json:"end"`

	Extra json.RawMessage `json:"extra"`
}

type StateMeetingData struct {
	ID     []byte `json:"id"`
	Object string `json:"object"`
	Action string `json:"action"`

	Name     string `json:"name"`
	Creation int64  `json:"creation"`
	Location string `json:"location"`

	Start int64 `json:"start"`
	End   int64 `json:"end"`

	ModificationID         []byte                   `json:"modification_id"`
	ModificationSignatures []PublicKeySignaturePair `json:"modification_signatures"`

	Extra json.RawMessage `json:"extra"`
}

type CreateRollCallData struct {
	ID     []byte `json:"id"`
	Object string `json:"object"`
	Action string `json:"action"`

	Name        string `json:"name"`
	Creation    int64  `json:"creation"`
	Start       int64  `json:"start"`
	Scheduled   int64  `json:"scheduled"`
	Location    string `json:"location"`
	Description string `json:"roll_call_description"`
}

type OpenRollCallActionType string

var (
	OpenRollCallAction   OpenRollCallActionType = "open"
	ReopenRollCallAction OpenRollCallActionType = "reopen"
)

type OpenRollCallData struct {
	ID     []byte                 `json:"id"`
	Object string                 `json:"object"`
	Action OpenRollCallActionType `json:"action"`

	Start int64 `json:"start"`
}

type CloseRollCallData struct {
	ID     []byte `json:"id"`
	Object string `json:"object"`
	Action string `json:"action"`

	Start     int64       `json:"start"`
	End       int64       `json:"end"`
	Attendees []PublicKey `json:"attendees"`
}

type WitnessMessageData struct {
	Object string `json:"object"`
	Action string `json:"action"`

	MessageID []byte    `json:"message_id"`
	Signature Signature `json:"signature"`
}

func (d *Data) UnmarshalJSON(data []byte) error {
	type internal struct {
		Object string `json:"object"`
		Action string `json:"action"`
	}

	tmp := &internal{}

	err := json.Unmarshal(data, tmp)
	if err != nil {
		return xerrors.Errorf("failed to parse object and action: %v", err)
	}

	switch tmp.Object {
	case "lao":
		err := d.parseLAOData(tmp.Action, data)
		if err != nil {
			return xerrors.Errorf("failed to parse lao data object: %v", err)
		}
		return nil
	case "message":
		err := d.parseMessageData(tmp.Action, data)
		if err != nil {
			return xerrors.Errorf("failed to parse message data object: %v", err)
		}
		return nil
	case "meeting":
		err := d.parseMeetingData(tmp.Action, data)
		if err != nil {
			return xerrors.Errorf("failed to parse meeting data object: %v", err)
		}
		return nil
	case "roll_call":
		err := d.parseRollCallData(tmp.Action, data)
		if err != nil {
			return xerrors.Errorf("failed to parse roll call data object: %v", err)
		}
		return nil
	default:
		return xerrors.Errorf("invalid data object: %s", tmp.Object)
	}
}

func (d *Data) parseRollCallData(action string, data []byte) error {
	switch action {
	case "create":
		create := &CreateRollCallData{}

		err := json.Unmarshal(data, create)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		d.CreateRollCallData = create
		return nil
	case "open", "reopen":
		open := &OpenRollCallData{}

		err := json.Unmarshal(data, open)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		d.OpenRollCallData = open
		return nil
	case "close":
		closeInst := &CloseRollCallData{}

		err := json.Unmarshal(data, closeInst)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		d.CloseRollCallData = closeInst
		return nil
	default:
		return xerrors.Errorf("invalid action: %s", action)
	}
}

func (d *Data) parseMessageData(action string, data []byte) error {
	if action != "witness" {
		return xerrors.Errorf("invalid action type: %s", action)
	}

	witness := &WitnessMessageData{}
	err := json.Unmarshal(data, witness)
	if err != nil {
		return xerrors.Errorf("failed to parse witness action: %v", err)
	}

	d.WitnessMessageData = witness
	return nil
}

func (d *Data) parseMeetingData(action string, data []byte) error {
	switch action {
	case "create":
		create := &CreateMeetingData{}

		err := json.Unmarshal(data, create)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		d.CreateMeetingData = create
		return nil
	case "state":
		state := &StateMeetingData{}

		err := json.Unmarshal(data, state)
		if err != nil {
			return xerrors.Errorf("failed to parse state lao data: %v", err)
		}

		d.StateMeetingData = state
		return nil
	default:
		return xerrors.Errorf("invalid action: %s", action)

	}
}

func (d *Data) parseLAOData(action string, data []byte) error {
	switch action {
	case "create":
		create := &CreateLAOData{}

		err := json.Unmarshal(data, create)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		d.CreateLAOData = create
		return nil
	case "update_properties":
		update := &UpdateLAOData{}

		err := json.Unmarshal(data, update)
		if err != nil {
			return xerrors.Errorf("failed to parse update lao data: %v", err)
		}

		d.UpdateLAOData = update
		return nil
	case "state":
		state := &StateLAOData{}

		err := json.Unmarshal(data, state)
		if err != nil {
			return xerrors.Errorf("failed to parse state lao data: %v", err)
		}

		d.StateLAOData = state
		return nil
	default:
		return xerrors.Errorf("invalid action: %s", action)
	}
}
