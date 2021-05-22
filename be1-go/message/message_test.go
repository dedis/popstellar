package message

import (
	"encoding/json"
	"log"
	"testing"
)

func TestMessage_1(t *testing.T) {
	rawMessage := `{"message_id":"5PDbn1XGTxIb5o-__UCbFKn-X4ySDdJfaxGdhqomvto=","sender":"rsW7gOijmzWmVRfZwg0ETqwA4SKimOA_zNKCfHjPZUI=","signature":"AA==","data":"AA==","witness_signatures":[]}`
	msg := &Message{}
	err := msg.UnmarshalJSON([]byte(rawMessage))
	log.Printf("Error: %v", err)
	b, err := json.Marshal(msg)
	log.Printf("error: %v", err)
	log.Printf("message: %s", b)
}
