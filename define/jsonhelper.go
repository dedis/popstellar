package define

import (
	"encoding/json"
	"strconv"
	"log"
	"fmt"
)

// TODO, we have exactly this issue : https://stackoverflow.com/questions/20101954/json-unmarshal-nested-object-into-string-or-byte

/*Most generic message structure*/
type Generic struct {
	jsonrpc string
	Method  string
	Params  json.RawMessage
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
	Message json.RawMessage
}

type Message struct {
	Data              json.RawMessage
	Sender            string
	Signature         string
	MessageID         string
	WitnessSignatures []string
}

type Data map[string]interface{}

type DataCreateLAO struct {
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID string
	// name of LAO
	Name string
	//Creation Date/Time
	Creation int64 //  Unix timestamp (uint64)
	LastModified int64 //timestamp
	//Organiser: Public Key
	OrganizerPKey string
	//List of public keys where each public key belongs to one witness
	Witnesses []string
	//List of public keys where each public key belongs to one member (physical person) (subscriber)
}
type Error struct {
	code int
	description string
}

type ResponseWithGenResult struct {
	jsonrpc      	string
	result      	int
	id 				string
}
type ResponseWithCatchupResult struct {
	jsonrpc      	string
	result      	[]byte
	id 				string
}
type ResponseWithError struct {
	jsonrpc      	string
	error      		Error
	id 				string
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

func AnalyseParamsLight(params json.RawMessage) (ParamsLight, error) {
	m := ParamsLight{}
	err := json.Unmarshal(params, &m)
	return m, err
}

func AnalyseParamsFull(params json.RawMessage) (ParamsFull, error) {
	m := ParamsFull{}
	err := json.Unmarshal(params, &m)
	return m, err
}

func AnalyseMessage(message json.RawMessage) (Message, error) {
	m := Message{}
	err := json.Unmarshal(message, &m)
	return m, err
}

func AnalyseData(data json.RawMessage) (Data, error) {
	m := Data{}
	err := json.Unmarshal(data, &m)
	return m, err
}

func AnalyseDataCreateLAO(data json.RawMessage) (DataCreateLAO, error) {
	m := DataCreateLAO{}
	err := json.Unmarshal(data, &m)
	fmt.Printf("%#v", m)
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

func CreateBroadcast(message Message, generic Generic) []byte {
	broadc := Generic{
		jsonrpc:	generic.jsonrpc,
		Method:		"message",
		Params: 	generic.Params,
		id:			generic.id,
	}
	b, err := json.Marshal(broadc)

	if err != nil {
		log.Fatal("couldn't Marshal the message to broadcast")
	}
	
	return b
}

/*
* Function that converts a Lao to a Json byte array
* we suppose error is in the good range
 */

func CreateResponse(err error, /*messages [],*/generic Generic) []byte {
	if err!= nil {
		resp := ResponseWithError {
			jsonrpc	:      	"2.0",
			error  	:		selectDescriptionError(err),
			id		:		generic.id,
		}
		b, err := json.Marshal(resp)
		if err != nil {
			fmt.Println("couldn't Marshal the response")
		}
		return b

	}else{
		//if(messages == null)// Mauvaise syntaxe{
			resp := ResponseWithGenResult{
				jsonrpc:      	"2.0",
				result:      	0,
				id 		:		generic.id,
			}
		/*}else{
			resp := ResponseWithCatchupResult{
				jsonrpc:      	"2.0",
				result:      	messages,
				id: 			generic.id,
			}
		}*/
		b, err := json.Marshal(resp)
		if err != nil {
			fmt.Println("couldn't Marshal the response")
		}
		return b
	}
}

/*
*	return the associate description error
*	we check the validity (error vetween -1 and -5) before the function
 */
func selectDescriptionError(err error) Error {
	switch err {
	case ErrInvalidAction:
		return Error{
			code:-1,
			description: "invalid action",
		}
	case ErrInvalidResource:
		return Error{
			code:-2,
			description: "invalid resource",
		}
		//(e.g. channel does not exist,channel was not subscribed to, etc.)
	case ErrResourceAlreadyExists:
		return Error{
			code:-3,
			description: "resource already exists",
		}
		//(e.g. lao already exists, channel already exists, etc.)
	case ErrRequestDataInvalid:
		return Error{
			code:-4,
			description: "request data is invalid",
		}
		//(e.g. message is invalid)
	case ErrAccessDenied:
		return Error{
			code:-5,
			description: "access denied",
		}
		//(e.g. subscribing to a “restricted” channel)
	default:
		fmt.Printf("%v", err)
		// TODO decide if we crash everything or not
		// log.Fatal("type of error unrecognized")
		return Error{} //should never arrive here
	}
}
