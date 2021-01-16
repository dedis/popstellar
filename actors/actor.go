// actor defines how witnesses and organizers must behave.
package actors

import (
	"student20_pop/lib"
	"student20_pop/message"
)

const SigThreshold = 0

// Actor is an interface representing either an organizer or a Witness.
type Actor interface {
	//Public functions
	HandleReceivedMessage(msg []byte, userId int) (msgAndChannel []lib.MessageAndChannel, responseToSender []byte)
	GetSubscribers(channel string) []int
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
