package message

// Message ...
type Message struct {
	Data              string
	Sender            string
	Signature         string
	MessageID         string `json:"message_id"`
	WitnessSignatures []WitnessSignature
}

// WitnessSignature ...
type WitnessSignature struct {
	Witness   string
	Signature string
}
