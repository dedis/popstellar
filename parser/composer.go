// parser implement functions to parse json strings into message structures, and reverse
package parser

import (
	"encoding/json"
	"log"
	"math/rand"
	"strconv"
	"student20_pop/event"
	"student20_pop/lib"
	"student20_pop/message"
	"time"
)

// ComposeBroadcastMessage outputs a message perfectly similar to query, but changes method to "broadcast"
func ComposeBroadcastMessage(query message.Query) []byte {
	broadcast := message.Query{
		Jsonrpc: query.Jsonrpc,
		Method:  "broadcast",
		Params:  query.Params,
		Id:      query.Id,
	}
	b, err := json.Marshal(broadcast)

	if err != nil {
		log.Println("couldn't Marshal the message to broadcast")
	}

	return b
}

// ComposeBroadcastStateLAO outputs a message confirming a LAO update after the signature threshold has been reached
func ComposeBroadcastStateLAO(lao event.LAO, laoData message.DataCreateLAO, orgPublicKey string,lastSig []byte) (queryStr []byte, err_ error) {
	//compose state update message
	state := message.DataStateLAO{
		Object:       "lao",
		Action:       "state",
		ID:           []byte(lao.ID),
		Name:         lao.Name,
		Creation:     lao.Creation,
		LastModified: time.Now().Unix(),
		Organizer:    []byte(lao.OrganizerPKey),
		Witnesses:    laoData.Witnesses,
	}

	stateStr, errs := json.Marshal(state)
	if errs != nil {
		return nil, errs
	}

	content := message.Message{
		Data:              stateStr,
		Sender:            []byte(orgPublicKey),
		// signature from received Message
		Signature:         lastSig,
		MessageId:         []byte(strconv.Itoa(rand.Int())),
		WitnessSignatures: nil,
	}

	contentStr, errs := json.Marshal(content)
	if errs != nil {
		return nil, errs
	}

	sendParams := message.Params{
		Channel: "/root/" + lao.ID,
		Message: contentStr,
	}

	paramsStr, errs := json.Marshal(sendParams)
	if errs != nil {
		return nil, errs
	}

	sendQuery := message.Query{
		Jsonrpc: "2.0",
		Method:  "broadcast",
		Params:  paramsStr,
		Id:      rand.Int(),
	}

	queryStr, errs = json.Marshal(sendQuery)
	if errs != nil {
		return nil, errs
	}
	return queryStr, errs
}

// ComposeResponse compose the response to be sent to the sender. It is assumed the error is in the correct range.
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

// composeErrorResponse composes an error response to be sent to the query sender. Called by ComposeResponse.
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

// composeResponse composes a positive response to be sent to the query seder. Called by ComposeResponse.
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
			Jsonrpc:       "2.0",
			CatchupResult: string(messages),
			Id:            query.Id,
		}
	}
	return json.Marshal(resp)
}

// selectDescriptionError returns a message.ErrorResponse depending on the error given as argument.
// The error validity is checked, and by default we return an "error invalid Data"
func selectDescriptionError(err error) []byte {
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
