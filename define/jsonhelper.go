package define

import (
	"encoding/json"
	"strconv"
)

/*Most generic message structure*/
type Generic struct {
	jsonrpc string
	Method	string
	Params	[]byte
	id      string
}

/* potential enum, but doesn't typecheck in go, the checks must still be manual, so kinda useless
type Method string
const(
	Subscribe Method = "subscribe"
	Unsubscribe Method = "unsubscribe"
	Message Method = "message"
	Publish Method = "publish"
	Catchup Method = "catchup"
)*/

type ParamsLight struct {
	Channel string
}

type ParamsFull struct {
	Channel string
	Message []byte
}

type Message struct {
	Data              []byte
	Sender            string
	Signature         string
	MessageID         string
	WitnessSignatures []string
}

type DataCreateLao struct {
	// Object	string
	// Action	string
}

/**
 * Function that takes a byte array as input and returns
 * a Message struct
 */
func AnalyseGeneric(generic []byte) (Generic, error) {
	m := Generic{}
	err := json.Unmarshal(generic, &m)
	return m, err
}

func AnalyseParamsLight(params []byte) (ParamsLight, error) {
	m := ParamsLight{}
	err := json.Unmarshal(params, &m)
	return m, err
}

func AnalyseParamsFull(params []byte) (ParamsFull, error) {
	m := ParamsFull{}
	err := json.Unmarshal(params, &m)
	return m, err
}

func AnalyseMessage(message []byte) (Message, error) {
	m := Message{}
	err := json.Unmarshal(message, &m)
	return m, err
}

func AnalyseDataCreateLAO(data []byte) (DataCreateLAO, error) {
	m := DataCreateLAO{}
	err := json.Unmarshal(data, &m)
	return m, err
}

/**
 * Function that reads a JSON message in order to create a new LAO
 */
/*
func JsonLaoCreate(message []byte) (MessageLaoCreate, error) {
	m := MessageLaoCreate{}
	err := json.Unmarshal(message, &m)
	return m, err
}

func DataToMessageEventCreate(data []byte) (MessageEventCreate, error) {
	m := MessageEventCreate{}
	err := json.Unmarshal(data, &m)
	return m, err
}*/

/**
 * Function that converts a Lao to a Json byte array
 */
func LaoToJson(lao LAO) []byte {
	str := []byte("{")

	str = append(str, []byte(`"type": lao, `)...)
	str = append(str, []byte(`"id": `+string(lao.ID)+`, `)...)
	str = append(str, []byte(`"name": `+lao.Name+`, `)...)
	str = append(str, []byte(`"organizerpkey": `+string(lao.OrganizerPKey)+`, `)...)
	//str = append(str, []byte(`"creationtime": `+string(lao.Creation)+`, `)...)
	//str = append(str, []byte(`"ip":`+string(lao.Ip)+`, `)...)
	//str = append(str, []byte(`"attestation":`+string(lao.Attestation)+`, `)...)

	// TODO create string for witness/members/...
	str = append(str, []byte(`"witness": , `)...)
	str = append(str, []byte(`"members": , `)...)
	str = append(str, []byte(`"events": , `)...)

	str = append(str, []byte("}")...)
	return str
}

/**
 * Function that generate JSON string from slice
 * returns title:{} if data is empty
 */
func SliceToJson(title string, data [][]byte) string {
	str := title + ": {"
	for i, d := range data {
		str += `"`
		str += strconv.Itoa(i)
		str += `": "`
		str += string(d)
		//if not last occurence
		if i != len(data) {
			str += ", "
		}
	}
	str += "}"
	return str
}
