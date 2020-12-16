package message

type Data map[string]interface{}

type DataCreateLAO struct {
	Object string
	Action string //if we put "action" with little a it crashes
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID string
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Organiser: Public Key
	Organizer string
	//List of public keys where each public key belongs to one witness
	Witnesses []string
	//List of public keys where each public key belongs to one member (physical person) (subscriber)
}

type DataCreateMeeting struct {
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID string
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Last_modified int64  //timestamp
	Location string //optional
	//Organiser: Public Key
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
}
type DataCreateRollCall struct {
	//TODO right now same attribute as meeting
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID string
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Last_modified int64  //timestamp
	Location string //optional
	//Organiser: Public Key
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
}
type DataCreatePoll struct {
	//TODO right now same attribute as meeting
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID string
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	//Last_modified int64  //timestamp
	Location string //optional
	//Organiser: Public Key
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
}
type DataWitnessMessage struct {
	Object     string
	Action     string
	Message_id string
	Signature  string
}
