package messagedata

// MessageWitness ...
type MessageWitness struct {
	Object    string
	Action    string
	MessageID string `json:"message_id"`
	Signature string
}
