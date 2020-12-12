/*interface file for actors*/
package actors

import (
	"fmt"
	"student20_pop/db"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
)

type Actor interface {
	//Public functions
	HandleWholeMessage(msg []byte, userId int) (message, channel, responseToSender []byte)
	//Private functions
	handlePublish(generic message.Generic) (message, channel []byte, err error)
	handleCreateLAO(msg message.Message, canal string, generic message.Generic) (message, channel []byte, err error)
	handleUpdateProperties(msg message.Message, canal string, generic message.Generic) (message, channel []byte, err error)
	handleWitnessMessage(msg message.Message, canal string, generic message.Generic) (message, channel []byte, err error)
	handleLAOState(msg message.Message, canal string, generic message.Generic) (message, channel []byte, err error)
	handleCreateRollCall(mag message.Message, canal string, generic message.Generic) (message, channel []byte, err error)
}

//general actors functions, act only in the "Sub" database
func handleSubscribe(generic message.Generic, userId int) error {
	params, err := parser.ParseParamsLight(generic.Params)
	if err != nil {
		fmt.Printf("unable to analyse paramsLight in handleSubscribe()")
		return lib.ErrRequestDataInvalid
	}
	return db.Subscribe(userId, []byte(params.Channel))
}

func handleUnsubscribe(generic message.Generic, userId int) error {
	params, err := parser.ParseParamsLight(generic.Params)
	if err != nil {
		fmt.Printf("unable to analyse paramsLight in handleUnsubscribe()")
		return lib.ErrRequestDataInvalid
	}
	return db.Unsubscribe(userId, []byte(params.Channel))
}

/* creates a message to publish on a channel from a received message. */
func finalizeHandling(canal string, generic message.Generic) (message []byte, channel []byte) {
	return parser.ComposeBroadcastMessage(generic), []byte(canal)
}
