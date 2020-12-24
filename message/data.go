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
	Object string
	Action string
	//ID hash : SHA256(lao_id||creation||name)
	ID []byte
	// Name of the meeting
	Name string
	//Creation's timestamp (Unix) (uint64)
	Creation int64 //  Unix timestamp (uint64)
	// meeting's location, optional
	Location string
	// meeting's Start time timestamp (Unix) (uint64)
	Start int64
	// meeting's End time timestamp (Unix) (uint64)
	End int64
	// arbitrary object, optional
	Extra string
}
type DataCreateRollCall struct {
	//TODO right now same attribute as meeting
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte
	// Name of the meeting
	Name string
	//Creation's timestamp (Unix) (uint64)
	Creation int64 //  Unix timestamp (uint64)
	// meeting's location, optional
	Location string
	// meeting's Start time timestamp (Unix) (uint64)
	Start int64
	// meeting's End time timestamp (Unix) (uint64)
	End int64
	// arbitrary object, optional
	Extra string
}
type DataCreatePoll struct {
	//TODO right now same attribute as meeting
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte
	// Name of the meeting
	Name string
	//Creation's timestamp (Unix) (uint64)
	Creation int64 //  Unix timestamp (uint64)
	// meeting's location, optional
	Location string
	// meeting's Start time timestamp (Unix) (uint64)
	Start int64
	// meeting's End time timestamp (Unix) (uint64)
	End int64
	// arbitrary object, optional
	Extra string
}



type DataUpdateLAO struct {
	Object string
	Action string
	//ID hash : OriginalName || Creation
	ID []byte
	// new name of the LAO
	Name string
	//Last modification's timestamp (Unix) (uint64)
	Last_modified int64
	// list of Witnesses' Public keys
	Witnesses [][]byte
}

type DataStateLAO struct {
	Object string
	Action string
	//ID hash : SHA256(organizer||creation||name) OriginalName || Creation
	ID []byte
	// new name of the LAO
	Name string
	//Creation timestamp (Unix) (uint64)
	Creation int64
	//Last modification timestamp (Unix) (uint64)
	Last_modified int64
	//Organizer's Public Key
	Organizer []byte
	// list of Witnesses' Public keys
	Witnesses [][]byte
	// id of the modification (either creation/update)
	Modification_id []byte
	// signatures of the witnesses on the modification message (either creation/update)
	Modification_signatures []json.RawMessage
}

type DataStateMeeting struct {
	Object string
	Action string
	//ID hash : OriginalName || Creation Date/Time Unix Timestamp
	ID []byte
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Last_modified Date/Time
	Last_modified int64  //  Unix timestamp (uint64)
	Location      string //optional
	Start         int64  /* Timestamp */
	End           int64  /* Timestamp, optional */
	Extra         string /* arbitrary object, optional */
	//Organiser: Public Key
	Organizer string
	//List of public keys where each public key belongs to one witness
	Witnesses []string
	// id of the modification (either creation/update)
	Modification_id []byte
	// signatures of the witnesses on the modification message (either creation/update)
	Modification_signatures []json.RawMessage
}

type DataWitnessMessage struct {
	Object     string
	Action     string
	Message_id []byte
	//signature by the witness over the data field of the message : Sign(data)
	Signature  []byte
}
