package hub

import "student20_pop/message"

// Hub defines the methods a PoP server must implement to receive messages
// and handle clients.
type Hub interface {
	Start(done chan struct{})

	Recv(msg IncomingMessage)

	RemoveClient(client *Client)
}

// IncomingMessage wraps the raw message from the websocket connection and pairs
// it with a `Client` instance.
type IncomingMessage struct {
	Client  *Client
	Message []byte
}

// Channel represents a PoP channel - like a LAO.
type Channel interface {
	Subscribe(client *Client, msg message.Subscribe) error

	Unsubscribe(client *Client, msg message.Unsubscribe) error

	Publish(msg message.Publish) error

	Catchup(msg message.Catchup) []message.Message
}
