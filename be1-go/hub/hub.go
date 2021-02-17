package hub

import "student20_pop/message"

type Hub interface {
	Start(done chan struct{})

	Recv(msg IncomingMessage)
}

type IncomingMessage struct {
	Client  *Client
	Message []byte
}

type Channel interface {
	Subscribe(client *Client, msg message.Subscribe) error

	Unsubscribe(client *Client, msg message.Unsubscribe) error

	Publish(msg message.Publish) error

	Catchup(msg message.Catchup) []message.Message
}
