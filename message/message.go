package message

type Message struct {
	MessageID         []byte                   `json:"message_id"`
	Data              Data                     `json:"data"`
	Sender            PublicKey                `json:"sender"`
	Signature         Signature                `json:"signature"`
	WitnessSignatures []PublicKeySignaturePair `json:"witness_signatures"`
}
