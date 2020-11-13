package define

import (
	"encoding/json"
	"strconv"
)

/*Most generic message structure*/
type Generic struct {
	schema     string
	id         string
	Properties []byte
}

/*
type Action string
const(
	Subscribe Action = "subscribe"
	Unsubscribe Action = "unsubscribe"
	Message Action = "message"
	Catchup Action = "catchup"
	Return Action = "return"
)*/

type Properties struct {
	//Action Action
	Action  string
	Channel string
	Message []byte
	Result  int64
	// Data []byte
	ReqID int64
}

type Message struct {
	Data              string
	Sender            string
	Signature         string
	MessageID         string
	WitnessSignatures []string
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

func AnalyseProperties(properties []byte) (Properties, error) {
	m := Properties{}
	err := json.Unmarshal(properties, &m)
	return m, err
}

func AnalyseMessage(message []byte) (Message, error) {
	m := Message{}
	err := json.Unmarshal(message, &m)
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
