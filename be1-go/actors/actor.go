// actor defines how witnesses and organizers must behave.
package actors

import (
	"student20_pop/lib"
	"student20_pop/message"
)

const SigThreshold = 0

// Actor is an interface representing either an organizer or a witness.
type Actor interface {
	//Public functions
	HandleReceivedMessage(msg []byte, userId int) (msgAndChannel []lib.MessageAndChannel, responseToSender []byte)
	GetSubscribers(channel string) []int

	// Perhaps something like

	// HandleQuery(query message.Query, connectionID int) which calls
	// HandleMessage(message message.Message) which calls
	// HandleData(data message.Data)

	// TODO: figure out return values for above

	/*
		// Data represents one of the possible data objects in message with other
		// fields being null.
		type Data struct {
			CreateLAO *CreateLAO
			UpdateLAOState *UpdateLAOState
			UpdateProperties *UpdateProperties
			CreateRollCall *CreateRollCall
		}

		message.Message, CreateLAO, UpdateLAOState, UpdateProperties, CreateRollCall
		can have Validate() method which does stuff in the security package (which IMHO)

	*/

	//Private functions
	handleSubscribe(query message.Query, userId int) error
	handleUnsubscribe(query message.Query, userId int) error
	handlePublish(query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
	handleCreateLAO(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
	handleUpdateProperties(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
	handleWitnessMessage(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
	handleLAOState(msg message.Message) (msgAndChannel []lib.MessageAndChannel, err error)
	handleCreateRollCall(mag message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
}
