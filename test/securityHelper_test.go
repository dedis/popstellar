// define/securityHelpers
package main

import (
	"encoding/json"
	"testing"
)
var  data =  {
	"object": "lao",
	"action": "create",
	"id": "HS/rOHITGdRji1m+07OJSsuE0ndC4pqiuMZLcyMZUL4=",
	"name": "My LAO",
	"creation": 123,
	"last_modified": 123,
	"organizer": "MHgxMjM0NQ==",
	"witnesses": []
}
func TestMessageIsValid(t *testing.T) {
	message :=Message {
		Data  :            json.RawMessage // in base 64
		Sender :           string
		Signature:         string
		Message_id:        string
		WitnessSignatures: []string
	}



}
