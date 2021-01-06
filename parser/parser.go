package parser

import (
	"encoding/json"
	"log"
	"strings"
	"student20_pop/lib"
	"student20_pop/message"
)

// ParseGenericMessage parses a json []byte into a message.GenericMessage struct.
// it also checks that the "jsonrpc" field is equal to "2.0"
func ParseGenericMessage(genericMessage []byte) (message.GenericMessage, error) {
	m := message.GenericMessage{}
	err := json.Unmarshal(genericMessage, &m)
	if err != nil {
		return m, lib.ErrIdNotDecoded
	}
	if m["jsonrpc"] != "2.0" {
		log.Printf("jsonrpc field is not 2.0 but %v", m["jsonrpc"])
		return m, lib.ErrRequestDataInvalid
	}
	return m, err
}

// ParseQuery parses a json []byte into a message.Query structure. It checks that the field "method" is correctly set
// according to the protocol defined in jsonRPC
func ParseQuery(query []byte) (message.Query, error) {
	m := message.Query{}
	err := json.Unmarshal(query, &m)
	if err != nil {
		return m, lib.ErrIdNotDecoded
	}
	switch m.Method {
	case "subscribe", "unsubscribe", "broadcast", "publish", "catchup":
		return m, err
	default:
		log.Printf("the method is not valid, it is instead %v", m.Method)
		return m, lib.ErrRequestDataInvalid
	}
}

// ParseParams parses a json.RawMessage into a message.Params structure. It checks that the field "Channel" starts with
// "/root"
func ParseParams(params json.RawMessage) (message.Params, error) {
	m := message.Params{}
	err := json.Unmarshal(params, &m)
	//L3Jvb3Q= is b64 for "/root"
	decodedChannel, err := lib.Decode(m.Channel)
	if !strings.HasPrefix(string(decodedChannel), "/root") || err != nil {
		log.Printf("channel id doesn't start with /root but is %v", m.Channel)
		return m, lib.ErrRequestDataInvalid
	}
	return m, err
}

// ParseMessage parses a json.RawMessage into a message.Message structure.
func ParseMessage(msg json.RawMessage) (message.Message, error) {
	m := message.Message{}
	err := json.Unmarshal(msg, &m)
	return m, err
}

// ParseWitnessSignature parses a json.RawMessage into a message.ItemWitnessSignatures structure.
func ParseWitnessSignature(witnessSignatures json.RawMessage) (message.ItemWitnessSignatures, error) {
	m := message.ItemWitnessSignatures{}
	err := json.Unmarshal(witnessSignatures, &m)
	return m, err
}

// ParseData parses a json.RawMessage into a message.Data structure. It calls dataConstAreValid to check restrictions
// defined in the jsonRPC protocol.
func ParseData(data string) (message.Data, error) {
	m := message.Data{}
	err := json.Unmarshal([]byte(data), &m)
	if dataConstAreValid(m) {
		return m, err
	} else {
		return m, lib.ErrRequestDataInvalid
	}
}

// dataConstAreValid verifies following constraints :
// * object is one of "lao", "message", "meeting"
// * action is one of "create", update_properties", "state", "witness"
// * creation and last modified are positive integer
func dataConstAreValid(m message.Data) bool {
	switch m["object"] {
	case "lao", "message", "meeting":
		switch m["action"] {
		case "create", "update_properties", "state", "witness":
		default:
			log.Printf("the action is not valid, it is instead %v", m["action"])
			return false
		}
	default:
		log.Printf("the object is not valid, it is instead %v", m["object"])
		return false
	}

	creation, okC := m["creation"].(int)
	lastm, okL := m["last_modified"].(int)
	if (okC && creation < 0) || (okL && lastm < 0) {
		log.Printf("the timestamps are smaller than 0")
		return false
	}
	return true
}

// ParseDataCommon parses a json.RawMessage into a message.DataCommon structure. Used to extract only the fields "object"
// and "action"
func ParseDataCommon(data json.RawMessage) (message.DataCommon, error) {
	m := message.DataCommon{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// ParseDataCreateLAO parses a json.RawMessage into a message.DataCreateLAO structure.
func ParseDataCreateLAO(data json.RawMessage) (message.DataCreateLAO, error) {
	m := message.DataCreateLAO{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// ParseDataWitnessMessage parses a json.RawMessage into a message.DataWitnessMessage structure.
func ParseDataWitnessMessage(data json.RawMessage) (message.DataWitnessMessage, error) {
	m := message.DataWitnessMessage{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// ParseDataWitnessMessage parses a json.RawMessage into a message.DataWitnessMessage structure.
func ParseDataOpenRollCall(data json.RawMessage) (message.DataOpenRollCall, error) {
	m := message.DataOpenRollCall{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// ParseDataCreateMeeting parses a json.RawMessage into a message.DataCreateMeeting structure.
func ParseDataCreateMeeting(data json.RawMessage) (message.DataCreateMeeting, error) {
	m := message.DataCreateMeeting{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// ParseDataCreateRollCall parses a json.RawMessage into a message.DataCreateRollCall structure.
func ParseDataCreateRollCall(data json.RawMessage) (message.DataCreateRollCall, error) {
	m := message.DataCreateRollCall{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// ParseDataCloseRollCall parses a json.RawMessage into a message.DataCreateRollCall structure.
func ParseDataCloseRollCall(data json.RawMessage) (message.DataCloseRollCall, error) {
	m := message.DataCloseRollCall{}
	err := json.Unmarshal(data, &m)
	return m, err
}
// ParseDataCreatePoll parses a json.RawMessage into a message.DataCreatePoll structure.
func ParseDataCreatePoll(data json.RawMessage) (message.DataCreatePoll, error) {
	m := message.DataCreatePoll{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// ParseDataUpdateLAO parses a json.RawMessage into a message.DataUpdateLAO structure.
func ParseDataUpdateLAO(data json.RawMessage) (message.DataUpdateLAO, error) {
	m := message.DataUpdateLAO{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// ParseDataStateLAO parses a json.RawMessage into a message.DataStateLAO structure.
func ParseDataStateLAO(data json.RawMessage) (message.DataStateLAO, error) {
	m := message.DataStateLAO{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// ParseDataStateMeeting parses a json.RawMessage into a message.DataStateMeeting structure.
func ParseDataStateMeeting(data json.RawMessage) (message.DataStateMeeting, error) {
	m := message.DataStateMeeting{}
	err := json.Unmarshal(data, &m)
	return m, err
}

// FilterAnswers returns true if the message was an answer message
func FilterAnswers(receivedMsg []byte) (bool, error) {
	genericMsg, err := ParseGenericMessage(receivedMsg)
	if err != nil {
		return false, err
	}

	// We don't check that the int is correctly 0 for answers and [-5;-1] for errors.
	// We don't want to answer to an error with another error, and we're already logging the message for debugging.

	_, isAnswerMsg := genericMsg["result"]
	if isAnswerMsg {
		log.Printf("positive response received : %v", string(receivedMsg))
		return true, nil
	}

	_, isErrorMsg := genericMsg["error"]
	if isErrorMsg {
		log.Printf("error response received : %v", string(receivedMsg))
		return true, nil
	}
	return false, nil
}
