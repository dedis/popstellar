package message

import (
	"crypto/sha256"
	"encoding/json"
	"fmt"

	"golang.org/x/xerrors"
)

type stringer string

func (s stringer) String() string {
	return string(s)
}

type DataObject string

var (
	LaoObject      DataObject = "lao"
	MessageObject  DataObject = "message"
	MeetingObject  DataObject = "meeting"
	RollCallObject DataObject = "roll_call"
)

type DataAction string

type Timestamp int64

func (t Timestamp) String() string {
	return string(t)
}

type Data interface {
	GetAction() DataAction

	GetObject() DataObject

	GetRaw() []byte

	//GetTimestamp() Timestamp
}

type GenericData struct {
	Action DataAction `json:"action"`
	Object DataObject `json:"object"`
	Raw    []byte
}

func (g *GenericData) GetAction() DataAction {
	return g.Action
}

func (g *GenericData) GetObject() DataObject {
	return g.Object
}

func (g *GenericData) GetRaw() []byte {
	return g.Raw
}

type LaoDataAction DataAction

var (
	CreateLaoAction LaoDataAction = "create"
	UpdateLaoAction LaoDataAction = "update_properties"
	StateLaoAction  LaoDataAction = "state"
)

type CreateLAOData struct {
	*GenericData

	ID        []byte      `json:"id"`
	Name      string      `json:"name"`
	Creation  Timestamp   `json:"creation"`
	Organizer PublicKey   `json:"organizer"`
	Witnesses []PublicKey `json:"witnesses"`
}

func (c *CreateLAOData) GetTimestamp() Timestamp {
	return c.Creation
}

func (c *CreateLAOData) setID() error {
	id, err := hash(stringer("L"), c.Organizer, c.Creation, stringer(c.Name))
	if err != nil {
		return xerrors.Errorf("error creating hash: %v", err)
	}

	c.ID = id
	return nil
}

type UpdateLAOData struct {
	*GenericData

	ID           []byte      `json:"id"`
	Name         string      `json:"name"`
	LastModified Timestamp   `json:"last_modified"`
	Witnesses    []PublicKey `json:"witnesses"`
}

func (u *UpdateLAOData) GetTimestamp() Timestamp {
	return u.LastModified
}

type StateLAOData struct {
	*GenericData

	ID                     []byte                   `json:"id"`
	Name                   string                   `json:"name"`
	LastModified           Timestamp                `json:"last_modified"`
	Creation               Timestamp                `json:"creation"`
	Organizer              PublicKey                `json:"organizer"`
	Witnesses              []PublicKey              `json:"witnesses"`
	ModificationID         []byte                   `json:"modification_id"`
	ModificationSignatures []PublicKeySignaturePair `json:"modification_signatures"`
}

func (s *StateLAOData) GetTimestamp() Timestamp {
	return s.LastModified
}

type MeetingDataAction DataAction

var (
	CreateMeetingAction MeetingDataAction = "create"
	UpdateMeetingAction MeetingDataAction = "update_properties"
	StateMeetingAction  MeetingDataAction = "state"
)

type CreateMeetingData struct {
	*GenericData

	ID       []byte    `json:"id"`
	Name     string    `json:"name"`
	Creation Timestamp `json:"creation"`
	Location string    `json:"location"`

	Start Timestamp `json:"start"`
	End   Timestamp `json:"end"`

	Extra json.RawMessage `json:"extra"`
}

func (c *CreateMeetingData) GetTimestamp() Timestamp {
	return c.Creation
}

type StateMeetingData struct {
	*GenericData

	ID       []byte    `json:"id"`
	Name     string    `json:"name"`
	Creation Timestamp `json:"creation"`
	Location string    `json:"location"`

	Start Timestamp `json:"start"`
	End   Timestamp `json:"end"`

	ModificationID         []byte                   `json:"modification_id"`
	ModificationSignatures []PublicKeySignaturePair `json:"modification_signatures"`

	Extra json.RawMessage `json:"extra"`
}

func (s *StateMeetingData) GetTimestamp() Timestamp {
	return s.Creation
}

type RollCallAction DataAction

var (
	CreateRollCallAction RollCallAction = "create"
	CloseRollCallAction  RollCallAction = "close"
)

type CreateRollCallData struct {
	*GenericData

	ID          []byte    `json:"id"`
	Name        string    `json:"name"`
	Creation    Timestamp `json:"creation"`
	Start       Timestamp `json:"start"`
	Scheduled   Timestamp `json:"scheduled"`
	Location    string    `json:"location"`
	Description string    `json:"roll_call_description"`
}

type OpenRollCallActionType RollCallAction

var (
	OpenRollCallAction   OpenRollCallActionType = "open"
	ReopenRollCallAction OpenRollCallActionType = "reopen"
)

type OpenRollCallData struct {
	*GenericData

	ID    []byte    `json:"id"`
	Start Timestamp `json:"start"`
}

type CloseRollCallData struct {
	*GenericData

	ID        []byte      `json:"id"`
	Start     Timestamp   `json:"start"`
	End       Timestamp   `json:"end"`
	Attendees []PublicKey `json:"attendees"`
}

type MessageDataAction DataAction

var (
	WitnessAction MessageDataAction = "witness"
)

type WitnessMessageData struct {
	*GenericData

	MessageID []byte    `json:"message_id"`
	Signature Signature `json:"signature"`
}

func NewCreateLAOData(name string, creation Timestamp, organizer PublicKey, witnesses []PublicKey) (Data, error) {
	create := &CreateLAOData{
		GenericData: &GenericData{
			Action: DataAction(CreateLaoAction),
			Object: LaoObject,
		},
		Name:      name,
		Creation:  creation,
		Organizer: organizer,
		Witnesses: witnesses,
	}

	err := create.setID()
	if err != nil {
		return nil, xerrors.Errorf("failed to set ID for CreateLAOData: %v", err)
	}

	return create, nil
}

func hash(strs ...fmt.Stringer) ([]byte, error) {
	h := sha256.New()
	for i, str := range strs {
		s := str.String()
		if len(s) == 0 {
			return nil, xerrors.Errorf("empty string to hash() at index: %d, %v", i, strs)
		}
		h.Write([]byte(string(len(s)) + s))
	}
	return h.Sum(nil), nil
}
