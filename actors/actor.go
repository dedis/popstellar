// Package actor defines how witnesses and organizers must behave
package actors

import (
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
)

const SIG_THRESHOLD = 0

type Actor interface {
	//Public functions
	HandleWholeMessage(msg []byte, userId int) (msgAndChannel []lib.MessageAndChannel, responseToSender []byte)
	GetSubscribers(channel string) []int
	//Private functions
	handleSubscribe(query message.Query, userId int) error
	handleUnsubscribe(query message.Query, userId int) error
	handlePublish(query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
	handleCreateLAO(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
	handleUpdateProperties(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
	handleWitnessMessage(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
	handleLAOState(msg message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
	handleCreateRollCall(mag message.Message, canal string, query message.Query) (msgAndChannel []lib.MessageAndChannel, err error)
}

/* creates a message to publish on a channel from a received message. */
func finalizeHandling(canal string, query message.Query) (message []byte, channel []byte) {
	return parser.ComposeBroadcastMessage(query), []byte(canal)
}
