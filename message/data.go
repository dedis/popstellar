// message defines the received Json messages and their nested fields
package message

import (
	"encoding/json"
)

type Data map[string]interface{}

// []byte are automatically decoded from base64 when unmarshalled, while strings (and json.RawMessage) are NOT

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
	//ID hash : SHA256('M'||lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the Meeting
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	// meeting's location, optional
	Location string `json:"location,omitempty"`
	// meeting's Start time timestamp (Unix) (uint64)
	Start int64 `json:"start"`
	// meeting's End time timestamp (Unix) (uint64)
	End int64 `json:"end"`
	// arbitrary object, optional
	Extra string `json:"extra,omitempty"`
}

type DataCreateRollCall struct {
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash SHA256('R'||lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the roll call
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	// roll call's location, optional
	Location string `json:"location,omitempty"`
	// roll call's Start time timestamp (Unix) (uint64)
	Start int64 `json:"start"`
	// roll call's scheduled time timestamp (Unix) (uint64)
	Scheduled int64 `json:"scheduled"`
	// An optional description of the roll call
	Description string `json:"roll_call_description,omitempty"`
}

type DataCloseRollCall struct {
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash SHA256('R'||lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the roll call
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
	//The start time corresponds to the time the event is opened/reopened
	Start int64 `json:"start"`
}

type DataCreatePoll struct {
	Object string `json:"object"`
	Action string `json:"action"`
	//ID hash : SHA256(lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the poll
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	// meeting's location, optional
	Location string `json:"location,omitempty"`
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
	// hash : (Organizer|| Creation|| Name)" same id as createLao !
	ID []byte `json:"id"`
	// Name of the LAO
	Name string `json:"name"`
	//Last modification's timestamp (Unix) (uint64)
	LastModified int64 `json:"last_modified"`
	// list of Witnesses' Public keys
	Witnesses [][]byte `json:"witnesses"`
}

type DataStateLAO struct {
	Object string `json:"object"`
	Action string `json:"action"`
	// ID of LAO. Not recomputed if the name changes
	ID []byte `json:"id"`
	// Name of the LAO
	Name string `json:"name"`
	//Creation's timestamp (Unix) (uint64)
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
	// hash : SHA256('M'||lao_id||creation||name)
	ID []byte `json:"id"`
	// Name of the Meeting
	Name string `json:"name"`
	//Creation timestamp (Unix) (uint64)
	Creation int64 `json:"creation"`
	//LastModified timestamp (Unix) (uint64)
	LastModified int64 `json:"last_modified"`
	//optional
	Location string `json:"location,omitempty"`
	//Start timestamp (Unix) (uint64)
	Start int64 `json:"start"`
	//End timestamp (optional) (Unix) (uint64)
	End int64 `json:"end"`
	// optional
	Extra string `json:"extra,omitempy"`
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
	//Sign(message_id) by the witness over the message_id field of the message to witness
	Signature []byte `json:"signature"`
}
