package define

import "hash"

/*
Data Types:
For readability (human) and proper encoding for JSON, these data types must be transmitted
and/or displayed in the given format but may be stored in bytes.
*/

type LAO struct {
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID []byte
	// name of LAO
	Name string
	//Creation Date/Time
	Creation     int64 //  Unix timestamp (uint64)
	LastModified int64 //timestamp
	//Organiser: Public Key
	OrganizerPKey []byte
	//List of public keys where each public key belongs to one witness
	Witnesses [][]byte
	//List of public keys where each public key belongs to one member (physical person) (subscriber)
}

type Event struct {
	//id hash : SHA1(Name + Creation Date/Time Unix Timestamp)
	ID []byte
	// name of event
	Name string
	//Creation Date/Time
	Creation     int64 //  Unix timestamp (uint64)
	LastModified int64 //timestamp
	Location     string
	Start        int64 //  Unix timestamp (uint64)
	End          int64 //timestamp
	Extra        []byte
}

type Election struct {
	// name of election
	name string
	//Creation Date/Time
	timestamp int64 //  Unix timestamp (uint64)
	//id hash : SHA1(Name + Creation Date/Time Unix Timestamp)
	id hash.Hash
	/*LAO: Hash Associated LAO*/
	//Default Ballot Options
	options []string
	/*Signature by the organizer and witnesses of the corresponding LAO on (ID) to attest to this event*/
	attestation [][]byte
}

type Vote struct {
	//the voter
	person []byte
	//Election ID
	electionId hash.Hash
	//vote are Hex (Point 1) || Hex (Point 2) : ElGamal encryption of a message.
	vote string
	/*Signature by the voter on SHA1(Election ID, LAO ID, Vote) to attest to their vote.*/
	attestation [][]byte
}
