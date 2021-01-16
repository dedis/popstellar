// event defines the structures for real-life events
package event

import "hash"

type LAO struct {
	// hash : Name || Creation, not updated on update.
	ID string
	// Name of the LAO
	Name string
	//Creation timestamp (Unix) (uint64)
	Creation int64
	//Organizer's Public Key
	OrganizerPKey string
	//List of witnesses' public key
	Witnesses []string
}

type Meeting struct {
	// hash : Name || Creation
	ID string
	// Name of the Meeting
	Name string
	// Creation timestamp (Unix) (uint64)
	Creation int64
	// Last modification's timestamp (Unix) (uint64)
	LastModified int64
	Location     string
	// Meeting's Start time timestamp (Unix) (uint64)
	Start int64
	// Meeting's End time timestamp (Unix) (uint64)
	End   int64
	Extra string
}

type RollCall struct {
	// hash : ('R' || lao_id || Creation || Name)
	ID string
	// Name of the Roll Call
	Name string
	// Creation timestamp (Unix) (uint64)
	Creation int64
	// Last modification's timestamp (Unix) (uint64)
	LastModified int64
	// RollCall's Location
	Location string
	// Meeting's Start time timestamp (Unix) (uint64)
	Start int64
	// Meeting's Scheduled time timestamp (Unix) (uint64)
	Scheduled int64
	// Meeting's End time timestamp (Unix) (uint64)
	End int64
	//List of Attendees' Public keys
	Attendees [][]byte
	// An optional Description of the roll call
	Description string
}

// not implemented in the protocol specifications yet
type Poll struct {
	// hash : ('P' || lao_id || Creation || Name)
	ID string
	// Name of the poll
	Name string
	// Creation timestamp (Unix) (uint64)
	Creation int64
	// Last modification's timestamp (Unix) (uint64)
	LastModified int64
	// Poll's Location
	Location string
	// Poll's Start time timestamp (Unix) (uint64)
	Start int64
	// Poll's End time timestamp (Unix) (uint64)
	End   int64
	Extra string
}

// not implemented in the protocol specifications yet
type Election struct {
	// hash : ('E' || lao_id || Creation || Name)
	ID string
	// Name of election
	Name string
	// Creation timestamp (Unix) (uint64)
	Creation int64
	//Default Ballot Options
	Options []string
	//Signature by the organizer and witnesses of the corresponding LAO on (ID) to attest to this event
	Attestation [][]byte
}

// not implemented in the protocol specifications yet
type Vote struct {
	//the voter
	Person []byte
	//Election ID
	ElectionId hash.Hash
	//vote are Hex (Point 1) || Hex (Point 2)
	Vote string
	//Signature by the voter on SHA1(Election ID, LAO ID, Vote) to attest to their vote.
	Attestation [][]byte
}
