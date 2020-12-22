package parser

import (
	"encoding/json"
	"log"
	"student20_pop/lib"
	"student20_pop/message"
)

func ComposeBroadcastMessage(query message.Query) []byte {
	broadcast := message.Query{
		Jsonrpc: query.Jsonrpc,
		Method:  "message",
		Params:  query.Params,
		Id:      query.Id,
	}
	b, err := json.Marshal(broadcast)

	if err != nil {
		log.Println("couldn't Marshal the message to broadcast")
	}

	return b
}

/*
 * Function that create the response to the sender
 * we suppose error is in the good range
 */
func ComposeResponse(err error, messages []byte, query message.Query) []byte {
	var resp []byte
	var internalErr error
	if err != nil {
		resp, internalErr = composeErrorResponse(err, query)
	} else {
		resp, internalErr = composeResponse(messages, query)
	}

	if internalErr != nil {
		log.Println("couldn't Marshal the response")
	}
	return resp
}

func composeErrorResponse(err error, query message.Query) ([]byte, error) {
	var resp interface{}
	if err == lib.ErrIdNotDecoded {
		resp = message.ResponseIDNotDecoded{
			Jsonrpc:       "2.0",
			ErrorResponse: selectDescriptionError(err),
			Id:            nil,
		}

	} else {
		resp = message.Response{
			Jsonrpc:       "2.0",
			ErrorResponse: selectDescriptionError(err),
			Id:            query.Id,
		}

	}
	return json.Marshal(resp)
}

func composeResponse(messages []byte, query message.Query) ([]byte, error) {
	var resp interface{}
	if messages == nil {
		resp = message.ResponseWithGenResult{
			Jsonrpc: "2.0",
			Result:  0,
			Id:      query.Id,
		}
	} else {
		resp = message.Response{
			Jsonrpc: "2.0",
			CatchupResult:  string(messages),
			Id:      query.Id,
		}
	}
	return json.Marshal(resp)
}

/*
*	return the associate description error
*	we check the validity (error between -1 and -5) before the function
 */
func 	selectDescriptionError(err error) []byte {
	var errResp message.ErrorResponse
	switch err {
	case lib.ErrInvalidAction:
		errResp = message.ErrorResponse{
			Code:        -1,
			Description: "invalid action",
		}
	case lib.ErrInvalidResource:
		errResp = message.ErrorResponse{
			Code:        -2,
			Description: "invalid resource",
		}
		//(e.g. channel does not exist,channel was not subscribed to, etc.)
	case lib.ErrResourceAlreadyExists:
		errResp = message.ErrorResponse{
			Code:        -3,
			Description: "resource already exists",
		}
		//(e.g. lao already exists, channel already exists, etc.)
	case lib.ErrRequestDataInvalid:
		errResp = message.ErrorResponse{
			Code:        -4,
			Description: "request data is invalid",
		}
	case lib.ErrIdNotDecoded:
		errResp = message.ErrorResponse{
			Code:        -4,
			Description: "Id could not be decoded",
		}
		//(e.g. message is invalid)
	case lib.ErrAccessDenied:
		errResp = message.ErrorResponse{
			Code:        -5,
			Description: "access denied",
		}
		//(e.g. subscribing to a “restricted” channel)
	case lib.ErrNotYetImplemented:
		errResp = message.ErrorResponse{
			Code:        -6,
			Description: "Feature not implemented",
		}

	default:
		log.Printf("%v", err)
		errResp = message.ErrorResponse{
			Code:        -4,
			Description: "request data is invalid",
		}
	}

	b, err := json.Marshal(errResp)

	if err != nil {
		log.Fatal("couldn't Marshal the message to broadcast")
	}

	return b
}
