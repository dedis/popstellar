package messagedata

// MessageWitness ...
type MessageWitness struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	MessageID string `json:"message_id"`
	Signature string `json:"signature"`
}
