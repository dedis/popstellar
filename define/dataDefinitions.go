package src

import "hash"

/*
Data Types:
For readability (human) and proper encoding for JSON, these data types must be transmitted
and/or displayed in the given format but may be stored in bytes.

*/

/* idea for BoltDB
3 nested bucket
 key : ID => Value new Bucket(LAO_1){
	key organizerpkyey => ...LAO
	key memeber => new Bucket(LAO_MEMBERS){
		key : 1 => ...
		key : 2 => ...
	}
*/

type pKey int32          //TODO type ?
type token hash.Hash     // TODO 32 64 \\\ how to convert to byte array ??
type signature hash.Hash // TODO 32 64

type LAO struct {
	// name of LAO
	Name string
	//Creation Date/Time
	Timestamp int64 //  Unix timestamp (uint64)
	//ID hash : Name || Creation Date/Time Unix Timestamp
	Id []byte
	//Organiser: Public Key
	OrganizerPKey pKey
	//List of public keys where each public key belongs to one witness
	Witnesses []pKey
	//List of public keys where each public key belongs to one member (physical person)
	Members []pKey
	//List of public keys where each public key belongs to an event
	Events []pKey
	//signature (hash)
	Attestation signature
	//tab with all created tokens
	TokensEmitted []token
	Ip            []byte
}

/*Private information (*state stored only on the client*):
Authentication: Private Key
LAOs: [] Network
Ownership, membership (regular, witness) can easily be determined when accessing the LAO.
Participating events can also be determined when accessing the LAO object.*/
type Person struct {
	// name of person
	name string
	//public key
	id pKey
}

type Event struct {
	// name of event
	name string
	//Creation Date/Time
	timestamp int64 //  Unix timestamp (uint64)
	//id hash : SHA1(Name + Creation Date/Time Unix Timestamp)
	id []byte
	/*	LAO: Hash
		Associated LAO
	*/
	//
	attendees []pKey
	//
	location string
	//
	typeOfEvent string
	//
	other string // TODO need json here
	/*Signature by the organizer and witnesses of the corresponding
	LAO on (Name, Creation Date, LAO, Location) to attest to this event*/
	attestation []signature
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
	attestation []signature
}

type Vote struct {
	//the voter
	person pKey
	//Election ID
	electionId hash.Hash
	//vote are Hex (Point 1) || Hex (Point 2) : ElGamal encryption of a message.
	vote string
	/*Signature by the voter on SHA1(Election ID, LAO ID, Vote) to attest to their vote.*/
	attestation []signature
}
