package message

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
)

// Message defines a JSON RPC message
type Message struct {
	Data              string             `json:"data"`
	Sender            string             `json:"sender"`
	Signature         string             `json:"signature"`
	MessageID         string             `json:"message_id"`
	WitnessSignatures []WitnessSignature `json:"witness_signatures"`
}

// WitnessSignature defines a witness signature in a message
type WitnessSignature struct {
	Witness   string `json:"witness"`
	Signature string `json:"signature"`
}

// UnmarshalData fills the provided elements with the message data stored in the
// data field. Recall that the Data field contains a base64URL representation of
// a message data, it takes care of properly decoding it. The provided element
// 'e' MUST be a pointer.
func (m Message) UnmarshalData(e interface{}) error {
	jsonData, err := base64.URLEncoding.DecodeString(m.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode base64: %v", err)
	}

	err = json.Unmarshal(jsonData, e)
	if err != nil {
		return errors.NewJsonUnmarshalError(err.Error())
	}

	return nil
}

func (m Message) VerifyMessage() error {
	dataBytes, err := base64.URLEncoding.DecodeString(m.Data)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode data: %v", err)
	}

	publicKeySender, err := base64.URLEncoding.DecodeString(m.Sender)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode public key: %v", err)
	}

	signatureBytes, err := base64.URLEncoding.DecodeString(m.Signature)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode signature: %v", err)
	}

	err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to verify signature : %v", err)
	}

	expectedMessageID := Hash(m.Data, m.Signature)
	if expectedMessageID != m.MessageID {
		return errors.NewInvalidActionError("messageID is wrong: expected %s found %s", expectedMessageID, m.MessageID)
	}

	return nil
}

// Hash returns the sha256 created from an array of strings
func Hash(strs ...string) string {
	h := sha256.New()
	for _, s := range strs {
		h.Write([]byte(fmt.Sprintf("%d", len(s))))
		h.Write([]byte(s))
	}

	return base64.URLEncoding.EncodeToString(h.Sum(nil))
}
