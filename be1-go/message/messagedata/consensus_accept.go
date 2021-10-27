package messagedata

// ConsensusAccept defines a message data
type ConsensusAccept struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	MessageID string `json:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValueAccept `json:"value"`
}

type ValueAccept struct {
	AcceptedTry   int64 `json:"accepted_try"`
	AcceptedValue bool  `json:"accepted_value"`
}
