package define

import (
	"encoding/json"
	"fmt"
	"log"
)

/*Most generic message structure*/
type Generic struct {
	Jsonrpc string
	Method  string
	Params  json.RawMessage
	Id      int
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
	Creation     int64 //  Unix timestamp (uint64)
	LastModified int64 //timestamp
	//Organiser: Public Key
	OrganizerPKey string
	//List of public keys where each public key belongs to one witness
	Witnesses []string
	//List of public keys where each public key belongs to one member (physical person) (subscriber)
}
type ErrorResponse struct {
	Code        int
	Description string
}

type ResponseWithGenResult struct {
	Jsonrpc string
	Result  int
	Id      int
}
type ResponseWithCatchupResult struct {
	Jsonrpc string
	Result  string
	Id      int
}
type ResponseWithError struct {
	Jsonrpc       string
	ErrorResponse string
	Id            int
}
type DataCreateMeeting struct {
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID string
	// name of LAO
	Name string
	//Creation Date/Time
	Creation     int64  //  Unix timestamp (uint64)
	LastModified int64  //timestamp
	Location     string //optional
	//Organiser: Public Key
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
}
type DataCreateRollCall struct {
	//TODO right now same attribute as meeting
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID string
	// name of LAO
	Name string
	//Creation Date/Time
	Creation     int64  //  Unix timestamp (uint64)
	LastModified int64  //timestamp
	Location     string //optional
	//Organiser: Public Key
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
}
type DataCreatePoll struct {
	//TODO right now same attribute as meeting
	Object string
	Action string
	//ID hash : Name || Creation Date/Time Unix Timestamp
	ID string
	// name of LAO
	Name string
	//Creation Date/Time
	Creation     int64  //  Unix timestamp (uint64)
	LastModified int64  //timestamp
	Location     string //optional
	//Organiser: Public Key
	Start int64  /* Timestamp */
	End   int64  /* Timestamp, optional */
	Extra string /* arbitrary object, optional */
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
	// TODO decode from Base64
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

func AnalyseDataCreateMeeting(data json.RawMessage) (DataCreateMeeting, error) {
	m := DataCreateMeeting{}
	err := json.Unmarshal(data, &m)
	return m, err
}
func AnalyseDataCreateRollCall(data json.RawMessage) (DataCreateRollCall, error) {
	m := DataCreateRollCall{}
	err := json.Unmarshal(data, &m)
	return m, err
}

func AnalyseDataCreatePoll(data json.RawMessage) (DataCreatePoll, error) {
	m := DataCreatePoll{}
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

func CreateBroadcastMessage(generic Generic) []byte {
	broadc := Generic{
		Jsonrpc: generic.Jsonrpc,
		Method:  "message",
		Params:  generic.Params,
		Id:      generic.Id,
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

func CreateResponse(err error, messages []byte, generic Generic) []byte {
	if err != nil {
		resp := ResponseWithError{
			Jsonrpc:       "2.0",
			ErrorResponse: string(selectDescriptionError(err)),
			Id:            generic.Id,
		}
		b, err := json.Marshal(resp)
		if err != nil {
			fmt.Println("couldn't Marshal the response")
		}
		return b

	} else {
		if messages == nil {
			resp := ResponseWithGenResult{
				Jsonrpc: "2.0",
				Result:  0,
				Id:      generic.Id,
			}
			b, err := json.Marshal(resp)
			if err != nil {
				fmt.Println("couldn't Marshal the response")
			}
			return b
		} else {
			resp := ResponseWithCatchupResult{
				Jsonrpc: "2.0",
				Result:  string(messages),
				Id:      generic.Id,
			}
			b, err := json.Marshal(resp)
			if err != nil {
				fmt.Println("couldn't Marshal the response")
			}
			return b
		}
	}
}

/*
*	return the associate description error
*	we check the validity (error vetween -1 and -5) before the function
 */
func selectDescriptionError(err error) []byte {
	var errResp ErrorResponse
	switch err {
	case ErrInvalidAction:
		errResp = ErrorResponse{
			Code:        -1,
			Description: "invalid action",
		}
	case ErrInvalidResource:
		errResp = ErrorResponse{
			Code:        -2,
			Description: "invalid resource",
		}
		//(e.g. channel does not exist,channel was not subscribed to, etc.)
	case ErrResourceAlreadyExists:
		errResp = ErrorResponse{
			Code:        -3,
			Description: "resource already exists",
		}
		//(e.g. lao already exists, channel already exists, etc.)
	case ErrRequestDataInvalid:
		errResp = ErrorResponse{
			Code:        -4,
			Description: "request data is invalid",
		}
		//(e.g. message is invalid)
	case ErrAccessDenied:
		errResp = ErrorResponse{
			Code:        -5,
			Description: "access denied",
		}
		//(e.g. subscribing to a “restricted” channel)
	default:
		fmt.Printf("%v", err)
		// TODO decide if we crash everything or not
		// log.Fatal("type of error unrecognized")
		return nil //should never arrive here
	}

	b, err := json.Marshal(errResp)

	if err != nil {
		log.Fatal("couldn't Marshal the message to broadcast")
	}

	return b
}
