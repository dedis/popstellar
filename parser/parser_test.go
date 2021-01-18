package parser

import (
	"encoding/json"
	"io/ioutil"
	"log"
	"reflect"
	"student20_pop/lib"
	"student20_pop/message"
	"testing"
)

// Some encoding (hashes) are actually incorrect here, but it doesn't impact the pure parser test
const correctCreateLAOString = `{
	"jsonrpc": "2.0",
	"method": "publish",
	"params": {
		"channel": "/root",
		"message": {
			"data": "ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogIllUSTVOVFk0TjJNNE1UUTBObVU1WVRJeE1USTNZbU5sTldaaU5ERTRNakJpWkRZNE9HTXlNVEl3WWpNM09HTTBPV1E1TW1RNE56Tm1aV05pTlRVNU9BPT0iLAogICAgIm5hbWUiOiAibXlfbGFvIiwKICAgICJjcmVhdGlvbiI6IDEyMzQsCiAgICAib3JnYW5pemVyIjogIk1USXoiLAogICAgIndpdG5lc3NlcyI6IHsKCiAgICB9Cn0=",
			"sender": "MTIz",
			"signature": "TUVZQ0lRRHdDUFdDcGx0Z1gzVWZCWk5HbVpqQzZLUVh6N2RkLzJvWHZwT3dHaWJSTXdJaEFQVGlBOWJ5aXA2YmZNaVdEemZQS0Q4OW83blNIeEJ4OGtvWVBKMWM1T3pr",
			"message_id": "OWQ3ZDVmNjFkNDlhYzc5NTE5M2NlMjlmYTRjZTU4MTRlZWUxOTRmY2M4OWFjYzZiMmUyMzNmYjk1ZmMwN2Q5Zg==",
			"witness_signatures": {

			}
		}
	},
	"id": 0
}`

const wrongCreateLAOString1 = `{
	"jsonrpc": "1.0",
	"method": "publish",
	"params": {
		"channel": "/root",
		"message": {
			"data": "ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogIllUSTVOVFk0TjJNNE1UUTBObVU1WVRJeE1USTNZbU5sTldaaU5ERTRNakJpWkRZNE9HTXlNVEl3WWpNM09HTTBPV1E1TW1RNE56Tm1aV05pTlRVNU9BPT0iLAogICAgIm5hbWUiOiAibXlfbGFvIiwKICAgICJjcmVhdGlvbiI6IDEyMzQsCiAgICAib3JnYW5pemVyIjogIk1USXoiLAogICAgIndpdG5lc3NlcyI6IHsKCiAgICB9Cn0=",
			"sender": "MTIz",
			"signature": "TUVZQ0lRRHdDUFdDcGx0Z1gzVWZCWk5HbVpqQzZLUVh6N2RkLzJvWHZwT3dHaWJSTXdJaEFQVGlBOWJ5aXA2YmZNaVdEemZQS0Q4OW83blNIeEJ4OGtvWVBKMWM1T3pr",
			"message_id": "OWQ3ZDVmNjFkNDlhYzc5NTE5M2NlMjlmYTRjZTU4MTRlZWUxOTRmY2M4OWFjYzZiMmUyMzNmYjk1ZmMwN2Q5Zg==",
			"witness_signatures": {

			}
		}
	},
	"id": 0
}`

const wrongCreateLAOString2 = `{
	"jsonrpc": "2.0",
	"method": "publish",
	"params": {
		"channel": "/root",
		"message": {
			"data": "ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogIllUSTVOVFk0TjJNNE1UUTBObVU1WVRJeE1USTNZbU5sTldaaU5ERTRNakJpWkRZNE9HTXlNVEl3WWpNM09HTTBPV1E1TW1RNE56Tm1aV05pTlRVNU9BPT0iLAogICAgIm5hbWUiOiAibXlfbGFvIiwKICAgICJjcmVhdGlvbiI6IDEyMzQsCiAgICAib3JnYW5pemVyIjogIk1USXoiLAogICAgIndpdG5lc3NlcyI6IHsKCiAgICB9Cn0=",
			"sender": "MTIz",
			"signature": "TUVZQ0lRRHdDUFdDcGx0Z1gzVWZCWk5HbVpqQzZLUVh6N2RkLzJvWHZwT3dHaWJSTXdJaEFQVGlBOWJ5aXA2YmZNaVdEemZQS0Q4OW83blNIeEJ4OGtvWVBKMWM1T3pr",
			"message_id": "OWQ3ZDVmNjFkNDlhYzc5NTE5M2NlMjlmYTRjZTU4MTRlZWUxOTRmY2M4OWFjYzZiMmUyMzNmYjk1ZmMwN2Q5Zg==",
			"witness_signatures": {

			}
		}
	},
	"id": 0...!
}`

func getCorrectCreateLAOQueryStruct() message.Query {
	return message.Query{
		Jsonrpc: "2.0",
		Method:  "publish",
		Params: json.RawMessage(`{
		"channel": "/root",
		"message": {
			"data": "ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogIllUSTVOVFk0TjJNNE1UUTBObVU1WVRJeE1USTNZbU5sTldaaU5ERTRNakJpWkRZNE9HTXlNVEl3WWpNM09HTTBPV1E1TW1RNE56Tm1aV05pTlRVNU9BPT0iLAogICAgIm5hbWUiOiAibXlfbGFvIiwKICAgICJjcmVhdGlvbiI6IDEyMzQsCiAgICAib3JnYW5pemVyIjogIk1USXoiLAogICAgIndpdG5lc3NlcyI6IHsKCiAgICB9Cn0=",
			"sender": "MTIz",
			"signature": "TUVZQ0lRRHdDUFdDcGx0Z1gzVWZCWk5HbVpqQzZLUVh6N2RkLzJvWHZwT3dHaWJSTXdJaEFQVGlBOWJ5aXA2YmZNaVdEemZQS0Q4OW83blNIeEJ4OGtvWVBKMWM1T3pr",
			"message_id": "OWQ3ZDVmNjFkNDlhYzc5NTE5M2NlMjlmYTRjZTU4MTRlZWUxOTRmY2M4OWFjYzZiMmUyMzNmYjk1ZmMwN2Q5Zg==",
			"witness_signatures": {

			}
		}
	}`),
		Id: 0,
	}
}

func TestParseGenericMessageAndQuery(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	_, err := ParseGenericMessage([]byte(correctCreateLAOString))
	if err != nil {
		t.Error(err)
	}

	msgquery, err := ParseQuery([]byte(correctCreateLAOString))
	if err != nil {
		t.Error(err)
	}
	referenceStruct := getCorrectCreateLAOQueryStruct()
	if msgquery.Jsonrpc != referenceStruct.Jsonrpc {
		t.Errorf("jsonrpc not equal")
	}
	if msgquery.Method != referenceStruct.Method {
		t.Errorf("Method not equal")
	}
	if msgquery.Id != referenceStruct.Id {
		t.Errorf("Id not equal")
	}

	// Careful that string comparison and as such in our case reflect.DeepEqual cares about the exact tabulation (spaces vs tabs and how many)
	if string(msgquery.Params) != string(referenceStruct.Params) {
		t.Errorf("Params not equal\n%v\n%v", string(msgquery.Params), string(referenceStruct.Params))
	}

	if !reflect.DeepEqual(msgquery, referenceStruct) {
		t.Errorf("correct structs are not as expected, \n%+v\n vs, \n%+v \n%v\n%v", msgquery, referenceStruct, string(msgquery.Params), string(referenceStruct.Params))
	}

	_, err = ParseGenericMessage([]byte(wrongCreateLAOString1))
	if err != lib.ErrRequestDataInvalid {
		t.Errorf("wrong string do not create an error")
	}

	_, err = ParseGenericMessage([]byte(wrongCreateLAOString2))
	if err != lib.ErrIdNotDecoded {
		t.Errorf("wrong string do not create an error")
	}
}

func TestParseParams(t *testing.T) {
}
