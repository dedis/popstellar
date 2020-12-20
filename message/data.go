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
	Witness Action = "witness"
)*/

type DataCreateLAO struct {
	//Necessary to re write names because the map data["Object"] is not the same as data["object"]
	Object string `json:"object"`
	Action string `json:"action"`//if we put "action" with little a it crashes
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte `json:"id"`
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 `json:"creation"`//  Unix timestamp (uint64)
	//Organiser: Public Key
	Organizer []byte
	//List of public keys where each public key belongs to one witness
	Witnesses [][]byte
	//List of public keys where each public key belongs to one member (physical person) (subscriber)
}

type DataCreateMeeting struct {
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Last_modified int64  //timestamp
	Location string //optional
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
}
type DataCreateRollCall struct {
	//TODO right now same attribute as meeting
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Last_modified int64  //timestamp
	Location string //optional
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
}
type DataCreatePoll struct {
	//TODO right now same attribute as meeting
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Last_modified int64  //timestamp
	Location string //optional
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
}
type DataWitnessMessage struct {
	Object     string
	Action     string
	Message_id []byte
	Signature  []byte
}

type DataUpdateLAO struct {
	Object string
	Action string //if we put "action" with little a it crashes
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte
	// name of LAO
	Name string
	//Last_modified Date/Time
	Last_modified int64 //  Unix timestamp (uint64)
	//List of public keys where each public key belongs to one witness
	Witnesses [][]byte
}

type DataStateLAO struct {
	Object string
	Action string //if we put "action" with little a it crashes
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Last_modified Date/Time
	Last_modified int64 //  Unix timestamp (uint64)
	//Organiser: Public Key
	Organizer []byte
	//List of public keys where each public key belongs to one witness
	Witnesses [][]byte
	// id of the modification (either creation/update)
	Modification_id []byte
	// signatures of the witnesses on the modification message (either creation/update)
	Modification_signatures []json.RawMessage
}

type DataStateMeeting struct {
	Object string
	Action string //if we put "action" with little a it crashes
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Last_modified Date/Time
	Last_modified int64 //  Unix timestamp (uint64)
	Location string //optional
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
	//Organiser: Public Key
	Organizer string
	//List of public keys where each public key belongs to one witness
	Witnesses []string
	// id of the modification (either creation/update)
	Modification_id []byte
	// signatures of the witnesses on the modification message (either creation/update)
	Modification_signatures []json.RawMessage
}