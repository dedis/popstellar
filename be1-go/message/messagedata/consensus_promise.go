package messagedata

// ConsensusPromise defines a message data
type ConsensusPromise struct {
	Object     string `json:"object"`
	Action     string `json:"action"`
	InstanceID string `json:"id"`
	MessageID  string `string:"message_id"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	Value ValuePromise `json:"value"`
}

type ValuePromise struct {
	AcceptedTry   int64 `json:"accepted_try"`
	AcceptedValue bool  `json:"accepted_value"`
	PromisedTry   int64 `json:"promised_try"`
}
