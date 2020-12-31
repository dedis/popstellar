// message defines the received Json messages and their nested fields
package message

import (
	"encoding/json"
)

type Data map[string]interface{}

// []byte are automatically decoded from base64 when unmarshalled, while string (and json.RawMessage) are NOT

/* potential enums, but doesn't typecheck in go, the checks must still be manual, so kinda useless
type Object string
const(
	Lao Object = "lao"
	Message Object = "message"
	Meeting Object = "meeting"
type Action string
const(
	Create Action = "create"
	Update_properties Action = "update_properties"
	State Action = "state"
	WitnessKey Action = "witness"
)*/

type DataCreateLAO struct {
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash : SHA256(organizer||creation||name)
	ID []byte `json:"id"`
	// Name of the LAO
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	//Organizer's Public Key
	Organizer []byte `json:"organizer"`
	//List of Witnesses' Public keys
	Witnesses [][]byte `json:"witness"`
}

type DataCreateMeeting struct {
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash : SHA256(lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the LAO
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	// meeting's location, optional
	Location string `json:"location"`
	// meeting's Start time timestamp (Unix) (uint64)
	Start int64 `json:"start"`
	// meeting's End time timestamp (Unix) (uint64)
	End int64 `json:"end"`
	// arbitrary object, optional
	Extra string `json:"extra"`
}
type DataCreateRollCallNow struct {
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash SHA256('R'||lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the roll ca
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	// roll call's location, optional
	Location string `json:"location"`
	// roll call's Start time timestamp (Unix) (uint64)
	Start int64 `json:"start"`
	// An optional description of the roll call
	RollCallDescription int64 `json:"roll_call_description",omitempty`
}
/* TODO need a clever way !
type DataCreateScheduledRollCall struct {
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash SHA256('R'||lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the roll ca
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	// roll call's location, optional
	Location string `json:"location"`
	// roll call's scheduled time timestamp (Unix) (uint64)
	Scheduled int64 `json:"scheduled"`
	// An optional description of the roll call
	RollCallDescription int64 `json:"roll_call_description",omitempty`
}*/
type DataCloseRollCall struct {
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash SHA256('R'||lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the roll ca
	Name string `json:"name"`
	//start's time timestamp (Unix) (uint64)
	Start int64 `json:"start"`
	//end's time timestamp (Unix) (uint64)
	End int64 `json:"end"`
	//List of Attendees' Public keys
	Attendees [][]byte `json:"attendees"`
}
type DataOpenRollCall struct {
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash SHA256('R'||lao_id||creation||name)
	ID []byte `json:"id"`
	//The start time correspond to the time the event is opened/reopened
	Start int64 `json:"start"`
}
type DataCreatePoll struct {
	//TODO right now same attribute as meeting
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash : SHA256(lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the LAO
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	// meeting's location, optional
	Location string `json:"location"`
	// meeting's Start time timestamp (Unix) (uint64)
	Start int64 `json:"start"`
	// meeting's End time timestamp (Unix) (uint64)
	End int64 `json:"end"`
	// arbitrary object, optional
	Extra string `json:"extra"`
}

type DataUpdateLAO struct {
	Object string `json:"object"`
	Action string `json:"action"`
	// hash : (Organizer|| Creation|| Name)" DIFFERENT from create Lao !
	ID []byte `json:"id"`
	// Name of the LAO,Meeting...
	Name string `json:"name"`
	//Last modification's timestamp (Unix) (uint64)
	LastModified int64 `json:"last_modified"`
	// list of Witnesses' Public keys
	Witnesses [][]byte `json:"witnesses"`
}

type DataStateLAO struct {
	Object string `json:"object"`
	Action string `json:"action"`
	// hash : (Organizer|| Creation|| Name)" DIFFERENT from create Lao !
	ID []byte `json:"id"`
	// Name of the LAO,Meeting...
	Name string `json:"name"`
	//Creation's timestamp (Uni	x) (uint64)
	Creation int64 `json:"creation"`
	//Last modification timestamp (Unix) (uint64)
	LastModified int64 `json:"last_modified"`
	//Organizer's Public Key
	Organizer []byte `json:"organizer"`
	// list of Witnesses' Public keys
	Witnesses [][]byte `json:"witnesses"`
	// id of the modification (either creation/update)
	ModificationId []byte `json:"modification_id"`
	// signatures of the witnesses on the modification message (either creation/update)
	ModificationSignatures []json.RawMessage `json:"modification_signatures"`
}

type DataStateMeeting struct {
	Object string `json:"object"`
	Action string `json:"action"`
	// hash : Name || Creation
	ID []byte `json:"id"`
	// Name of the LAO,Meeting...
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	//LastModified Date/Time
	LastModified int64  `json:"last_modified,"` //  Unix timestamp (uint64)
	Location     string `json:"location"`       //optional
	Start        int64  `json:"start"`          /* Timestamp */
	End          int64  `json:"end"`            /* Timestamp, optional */
	Extra        string `json:"extra"`          /* arbitrary object, optional */
	//Organiser: Public Key
	Organizer string `json:"organize"`
	// id of the modification (either creation/update)
	ModificationId []byte `json:"modification_id"`
	// signatures of the witnesses on the modification message (either creation/update)
	ModificationSignatures []json.RawMessage `json:"modification_signatures"`
}

type DataWitnessMessage struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	MessageId []byte `json:"message_id"`
	//signature by the witness over the data field of the message : Sign(data)
	Signature []byte `json:"signature"`
}
