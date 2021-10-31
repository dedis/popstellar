package messagedata

// ConsensusPrepare defines a message data
type ConsensusPrepare struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"instance_id"`
	MessageID  string `json:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValuePrepare `json:"value"`
}

type ValuePrepare struct {
	ProposedTry int64 `json:"proposed_try"`
}
