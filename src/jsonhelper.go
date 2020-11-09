package src

import (
	"encoding/json"
	"strconv"
)

/*Basic message structure*/
type Message struct {
	Item   []byte
	Action []byte
	Data   []byte
	RequestID  []byte
}

/*Struct used to create LAOs*/
type MessageLaoCreate struct {
	ID            []byte
	OrganizerPkey []byte
	Timestamp     int64
	Name          []byte
	Ip            []byte
	Attestation   []byte
}

/*Struct used to subscribe/unsubscribe to channel*/
type MessageRegistration struct {
	userId []byte
	channelId []byte
	action []byte
}

/**
 * Function that takes a byte array as input and returns
 * a Message struct
 */
func AnalyseMsg(message []byte) (Message, error) {
	m := Message{}
	err := json.Unmarshal(message, m)
	return m, err
}

/**
 * Function that reads a JSON message in order to create a new LAO
 */
func JsonLaoCreate(message []byte) (MessageLaoCreate, error) {
	m := MessageLaoCreate{}
	err := json.Unmarshal(message, m)
	return m, err
}

/**
 * Function that reads a JSON message in order to register\unregister one
 */
func JsonRegistration(message []byte) (MessageRegistration, error) {
	m := MessageRegistration{}
	err := json.Unmarshal(message, m)
	return m, err
}
/**
 * Function that converts a Lao to a Json byte array
 */
func LaoToJson(lao LAO) []byte {
	str := []byte("{")

	str = append(str, []byte(`"type": lao, `)...)
	str = append(str, []byte(`"id": `+string(lao.Id)+`, `)...)
	str = append(str, []byte(`"name": `+lao.Name+`, `)...)
	str = append(str, []byte(`"organizerpkey": `+string(lao.OrganizerPKey)+`, `)...)
	str = append(str, []byte(`"creationtime": `+string(lao.Timestamp)+`, `)...)
	str = append(str, []byte(`"ip":`+string(lao.Ip)+`, `)...)
	str = append(str, []byte(`"attestation":`+string(lao.Attestation)+`, `)...)

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
