package parser

import (
	"encoding/json"
	"log"
	"strings"
	"student20_pop/lib"
	"student20_pop/message"
)

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

/**
 * Function that takes a byte array as input and returns
 * a Message struct
 */
func ParseQuery(query []byte) (message.Query, error) {
	m := message.Query{}
	err := json.Unmarshal(query, &m)
	if err != nil {
		return m, lib.ErrIdNotDecoded
	}
	switch m.Method {
	case "subscribe", "unsubscribe", "message", "publish", "catchup":
		return m, err
	default:
		log.Printf("the method is not valid, it is instead %v", m.Method)
		return m, lib.ErrRequestDataInvalid
	}
}

func ParseParams(params json.RawMessage) (message.Params, error) {
	m := message.Params{}
	err := json.Unmarshal(params, &m)
	if !strings.HasPrefix(m.Channel, "/root") {
		log.Printf("channel id doesn't start with /root but is %v", m.Channel)
		return m, lib.ErrRequestDataInvalid
	}
	return m, err
}

func ParseParamsIncludingMessage(params json.RawMessage) (message.ParamsIncludingMessage, error) {
	m := message.ParamsIncludingMessage{}
	err := json.Unmarshal(params, &m)
	if err != nil {
		return m, lib.ErrEncodingFault
	}
	//d, err := lib.Decode(m.Channel)
	//m.Channel = string(d)
	if !strings.HasPrefix(m.Channel, "/root") {
		log.Printf("channel id doesn't start with /root but is %v", m.Channel)
		return m, lib.ErrRequestDataInvalid
	}
	return m, err
}

func ParseMessage(msg json.RawMessage) (message.Message, error) {
	m := message.Message{}
	err := json.Unmarshal(msg, &m)
	return m, err
}

func ParseWitnessSignature(witnessSignatures json.RawMessage) (message.ItemWitnessSignatures, error) {
	m := message.ItemWitnessSignatures{}
	err := json.Unmarshal(witnessSignatures, &m)
	return m, err
}

func ParseData(data string) (message.Data, error) {
	m := message.Data{}
	err := json.Unmarshal([]byte(data), &m)
	if dataConstAreValid(m) {
		return m, err
	} else {
		return m, lib.ErrRequestDataInvalid
	}
}

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

func ParseDataCreateLAO(data json.RawMessage) (message.DataCreateLAO, error) {
	m := message.DataCreateLAO{}
	err := json.Unmarshal(data, &m)
	return m, err
}

func ParseDataWitnessMessage(data json.RawMessage) (message.DataWitnessMessage, error) {
	m := message.DataWitnessMessage{}
	err := json.Unmarshal(data, &m)
	if err != nil {
		log.Printf("error unmarshalling the string : %v", err)
		return m, err
	}
	return m, err
}

func ParseDataCreateMeeting(data json.RawMessage) (message.DataCreateMeeting, error) {
	m := message.DataCreateMeeting{}
	err := json.Unmarshal(data, &m)
	return m, err
}
func ParseDataCreateRollCall(data json.RawMessage) (message.DataCreateRollCall, error) {
	m := message.DataCreateRollCall{}
	err := json.Unmarshal(data, &m)
	return m, err
}

func ParseDataCreatePoll(data json.RawMessage) (message.DataCreatePoll, error) {
	m := message.DataCreatePoll{}
	err := json.Unmarshal(data, &m)
	return m, err
}
