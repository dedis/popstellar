package messagedata

// ConsensusPropose defines a message data
type ConsensusPropose struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	MessageID string `json:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValuePropose `json:"value"`

	AcceptorSignature []string `json:"acceptor_signature"`
}

type ValuePropose struct {
	ProposedTry   int64 `json:"proposed_try"`
	ProposedValue bool  `json:"proposed_value"`
}
