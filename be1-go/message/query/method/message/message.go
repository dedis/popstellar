package message

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/message/answer"
)

// Message defines a JSON RPC message
type Message struct {
	Data              string             `json:"data"`
	Sender            string             `json:"sender"`
	Signature         string             `json:"signature"`
	MessageID         string             `json:"message_id"`
	WitnessSignatures []WitnessSignature `json:"witness_signatures"`
}

// UnmarshalData fills the provided elements with the message data stored in the
// data field. Recall that the Data field contains a base64URL representation of
// a message data, it takes care of properly decoding it. The provided element
// 'e' MUST be a pointer.
func (m Message) UnmarshalData(e interface{}) error {
	jsonData, err := base64.URLEncoding.DecodeString(m.Data)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to decode base64: %v", err)
	}

	err = json.Unmarshal(jsonData, e)
	if err != nil {
		return answer.NewInvalidMessageFieldError("failed to unmarshal jsonData: %v", err)
	}

	return nil
}

// WitnessSignature defines a witness signature in a message
type WitnessSignature struct {
	Witness   string `json:"witness"`
	Signature string `json:"signature"`
}
