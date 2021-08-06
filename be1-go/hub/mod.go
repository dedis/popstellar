// Package hub defines an interface that is used for processing incoming
// JSON-RPC messages from the websocket connection and replying to them
// by either sending a Result, Error or broadcasting a message to other
// clients.
//
// A concrete instance of a Hub may be an Organizer or a Witness. A baseHub
// type contains the implementation common across both types.
package hub

import (
	"context"
	"student20_pop/message"
	"student20_pop/network/socket"
)

// HubType denotes the type of the hub.
type HubType string

const (
	// OrganizerHubType represents the Organizer Hub.
	OrganizerHubType HubType = "organizer"

	// WitnessHubType represnets the Witness Hub.
	WitnessHubType HubType = "witness"
)

// Hub defines the methods a PoP server must implement to receive messages
// and handle clients.
type Hub interface {
	// Start invokes the processing loop for the hub.
	Start(ctx context.Context)

	// Receiver returns a channel that may be used to
	// process incoming messages
	Receiver() chan<- socket.IncomingMessage

	// OnSocketClose returns a channel which accepts
	// socket ids on connection close events. This
	// allows the hub to cleanup clients which close
	// without sending an unsubscribe message
	OnSocketClose() chan<- string

	// Type returns the type of Hub.
	Type() HubType
}

// Channel represents a PoP channel - like a LAO.
type Channel interface {
	// Subscribe is used to handle a subscribe message.
	Subscribe(socket socket.Socket, msg message.Subscribe) error

	// Unsubscribe is used to handle an unsubscribe message.
	Unsubscribe(socketID string, msg message.Unsubscribe) error

	// Publish is used to handle a publish message.
	Publish(msg message.Publish) error

	// Catchup is used to handle a catchup message.
	Catchup(msg message.Catchup) []message.Message
}
