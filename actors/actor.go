/*interface file for actors*/
package actors

import (
	"fmt"
	"log"
	"student20_pop/db"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
)

type Actor interface {
	//Public functions
	HandleWholeMessage(msg []byte, userId int) (message, channel, responseToSender []byte)
	//Private functions
	handlePublish(query message.Query) (message, channel []byte, err error)
	handleCreateLAO(msg message.Message, canal string, query message.Query) (message, channel []byte, err error)
	handleUpdateProperties(msg message.Message, canal string, query message.Query) (message, channel []byte, err error)
	handleWitnessMessage(msg message.Message, canal string, query message.Query) (message, channel []byte, err error)
	handleLAOState(msg message.Message, canal string, query message.Query) (message, channel []byte, err error)
	handleCreateRollCall(mag message.Message, canal string, query message.Query) (message, channel []byte, err error)
}

func filterAnswers(receivedMsg []byte) (bool, error) {
	genericMsg, err := parser.ParseGenericMessage(receivedMsg)
	if err != nil {
		return false, err
	}

	/* TODO we *could* check that the int is correctly 0 for answers and [-5;-1] for errors. 
	But is it really worth it? Anyways, not sure we want to answer with "error:request data invalid" to an error with an out-of-range errId...
	We're already logging the msg received, that should be enough to debug?
	*/
	_, isAnswerMsg := genericMsg["result"]
	if isAnswerMsg {
		log.Printf("an answer has been received, with %v", string(receivedMsg))
		return true, nil
	}

	_, isErrorMsg := genericMsg["error"]
	if isErrorMsg {
		log.Printf("an answer has been received, with %v", string(receivedMsg))
		return true, nil
	}
	return false, nil
}

//general actors functions, act only in the "Sub" database
func handleSubscribe(query message.Query, userId int) error {
	params, err := parser.ParseParams(query.Params)
	if err != nil {
		fmt.Printf("unable to analyse paramsLight in handleSubscribe()")
		return lib.ErrRequestDataInvalid
	}
	return db.Subscribe(userId, []byte(params.Channel))
}

func handleUnsubscribe(query message.Query, userId int) error {
	params, err := parser.ParseParams(query.Params)
	if err != nil {
		fmt.Printf("unable to analyse paramsLight in handleUnsubscribe()")
		return lib.ErrRequestDataInvalid
	}
	return db.Unsubscribe(userId, []byte(params.Channel))
}

/* creates a message to publish on a channel from a received message. */
func finalizeHandling(canal string, query message.Query) (message []byte, channel []byte) {
	return parser.ComposeBroadcastMessage(query), []byte(canal)
}
