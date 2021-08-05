package hub

import (
	"context"
	"student20_pop/message"
	"sync"
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
	Start(ctx context.Context, wg *sync.WaitGroup)

	Recv(msg IncomingMessage)

	RemoveClientSocket(client *ClientSocket)
}

// IncomingMessage wraps the raw message from the websocket connection and pairs
// it with a `Client` instance.
type IncomingMessage struct {
	Socket  *baseSocket
	Message []byte
}

// Channel represents a PoP channel - like a LAO.
type Channel interface {
	// Subscribe is used to handle a subscribe message.
	Subscribe(client *ClientSocket, msg message.Subscribe) error

	// Unsubscribe is used to handle an unsubscribe message.
	Unsubscribe(client *ClientSocket, msg message.Unsubscribe) error

	// Publish is used to handle a publish message.
	Publish(msg message.Publish) error

	// Catchup is used to handle a catchup message.
	Catchup(msg message.Catchup) []message.Message
}
