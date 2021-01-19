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

// ComposeBroadcastStateLAO outputs a message confirming a LAO update after the signature threshold has been reached.
// The update message.Message parameter is the updateProperties message validated. It is possible to make it this way as
// we update the original's message list of signatures when we receive a signature.
func ComposeBroadcastStateLAO(lao event.LAO, orgPublicKey string, update message.Message) (queryStr []byte, err_ error) {
	//compose state update message

	state := message.DataStateLAO{
		Object:                 "lao",
		Action:                 "state",
		ID:                     []byte(lao.ID),
		Name:                   lao.Name,
		Creation:               lao.Creation,
		LastModified:           time.Now().Unix(),
		Organizer:              []byte(lao.OrganizerPKey),
		Witnesses:              lib.StringArrayToNestedByteArray(lao.Witnesses),
		ModificationId:         update.MessageId,
		ModificationSignatures: update.WitnessSignatures,
	}

	stateStr, errs := json.Marshal(state)
	if errs != nil {
		return nil, errs
	}

	content := message.Message{
		Data:              stateStr,
		Sender:            []byte(orgPublicKey),
		Signature:         nil, // TODO fix this in protocol, because back-end has no private key to sign stuff.
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
			Jsonrpc: "2.0",
			Error:   selectDescriptionError(err),
			Id:      nil,
		}

	} else {
		resp = message.Response{
			Jsonrpc: "2.0",
			Error:   selectDescriptionError(err),
			Id:      query.Id,
		}

	}
	return json.Marshal(resp)
}

// composeResponse composes a positive response to be sent to the query sender. Called by ComposeResponse.
func composeResponse(messages []byte, query message.Query) ([]byte, error) {
	var resp interface{}
	if messages == nil {
		resp = message.ResponseWithGenResult{
			Jsonrpc: "2.0",
			Result:  0,
			Id:      query.Id,
		}
	} else {
		fabricated := fabricatePlaceholderCatchup(messages)
		resp = message.Response{
			Jsonrpc:       "2.0",
			CatchupResult: fabricated,
			Id:            query.Id,
		}
	}
	return json.Marshal(resp)
}

// fabricatePlaceholderCatchup generate a json message with the current state of a channel in the data field based
// on the channel database. The other fields of message are set to nil as they can't be retrieved from this database.
// It is only there to fill an incomplete implementation and could be fully removed later as the full history of messages becomes more easily accessible.
func fabricatePlaceholderCatchup(state []byte) string {
		/* 
		in the current incomplete implementation, messages is actually not at all a message 
		but a serialized json string of the event "owning the channel". we must thus first unmarshall it, 
		reconstruct a data struct from it, marshall it back into JSON, 
		reconstruct a message struct with it and nil sender because we cannot retrieve them currently
		marshall it to JSON and send it.
		This is obviously far from perfect, but it allows to send a working response to catchup, useful for the requester
		and all this without impacting at all function signatures or protocol definition.
		As the event database does not have all the required fields, the work-around is quite dirty and to be removed in the future
		TL;DR: What is put in the field CatchupResult needs to be changed, once the "message db" is improved.
		*/
		m := message.Data{}
		json.Unmarshal(state, &m)
		var data []byte = nil
		if m["OrganizerPKey"] != nil {
			// then it's a LAO
			m := event.LAO{}
			json.Unmarshal(state, &m)
			lao := message.DataCreateLAO{
				Object:		  "lao",
				Action:		  "create",
				ID:           []byte(m.ID),
				Name:         m.Name,
				Creation:     m.Creation,
				Organizer:    []byte(m.OrganizerPKey),
				Witnesses:    lib.StringArrayToNestedByteArray(m.Witnesses),
			}
			data, _ = json.Marshal(lao)
		} else {
			// then it's a RollCall in the current implementation
			m := event.RollCall{}
			json.Unmarshal(state, &m)
			rollcall := message.DataCreateRollCall{
				Object:		  "roll_call",
				Action:		  "create",
				ID:           []byte(m.ID),
				Name:         m.Name,
				Creation:     m.Creation,
				Location:     m.Location,
				Start:        m.Start,
				Scheduled:	  m.Scheduled,
				Description:  m.Description,
			}
			data, _ = json.Marshal(rollcall)
		}

		msg := message.Message{
			Data:              data,
			Sender:		       nil,
			Signature:		   nil,
			MessageId:		   nil,
			WitnessSignatures: nil,
		}

		fabricated, _ := json.Marshal(msg)
		return string(fabricated)
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
