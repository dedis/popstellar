/*interface file for actors*/
package actors

import (
	"fmt"
	"student20_pop/db"
	"student20_pop/define"
)

type Actor interface {
	//Public functions
	HandleWholeMessage(msg []byte, userId int) ([]byte, []byte, []byte)
	//Private functions
	handlePublish(generic define.Generic) ([]byte, []byte, error)
	handleCreateLAO(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error)
	handleUpdateProperties(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error)
	handleWitnessMessage(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error)
	handleLAOState(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error)
	handleCreateRollCall(message define.Message, channel string, generic define.Generic) ([]byte, []byte, error)
}

//general actors functions, act only in the "Sub" database
func handleSubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		fmt.Printf("unable to analyse paramsLight in handleSubscribe()")
		return define.ErrRequestDataInvalid
	}
	return db.Subscribe(userId, []byte(params.Channel))
}

func handleUnsubscribe(generic define.Generic, userId int) error {
	params, err := define.AnalyseParamsLight(generic.Params)
	if err != nil {
		fmt.Printf("unable to analyse paramsLight in handleUnsubscribe()")
		return define.ErrRequestDataInvalid
	}
	return db.Unsubscribe(userId, []byte(params.Channel))
}

/** creates a message to publish on a channel from a received message.
@returns, in order
 * message
 * channel
*/
func finalizeHandling(canal string, generic define.Generic) ([]byte, []byte) {
	return define.CreateBroadcastMessage(generic), []byte(canal)
}
