// Package hub defines an interface that is used for processing incoming
// JSON-RPC messages from the websocket connection and replying to them by
// either sending a Result, Error or broadcasting a message to other clients.
package hub

import (
	"popstellar/network/socket"
)

// Hub defines the methods a PoP server must implement to receive messages
// and handle clients.
type Hub interface {
	// NotifyNewServer add a socket for the hub to send message to other servers
	NotifyNewServer(socket.Socket)

	// Start invokes the processing loop for the hub.
	Start()

	// Stop closes the processing loop for the hub.
	Stop()

	// Receiver returns a channel that may be used to process incoming messages
	Receiver() chan<- socket.IncomingMessage

	// OnSocketClose returns a channel which accepts socket ids on connection
	// close events. This allows the hub to cleanup clients which close without
	// sending an unsubscribe message
	OnSocketClose() chan<- string

	// SendGreetServer sends a greet server message in the socket
	SendGreetServer(socket.Socket) error
}
