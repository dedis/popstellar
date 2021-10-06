package consensus

import (
	"popstellar/channel"
	"popstellar/channel/inbox"
	"popstellar/message/answer"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"strconv"
	"sync"

	"github.com/rs/zerolog"
)

const (
	msgID = "msg id"
)

// Channel defins a consensus channel
type Channel struct {
	sockets channel.Sockets

	inbox *inbox.Inbox

	// /root/<lao_id>/<id>
	channelID string

	witnessMu sync.Mutex
	witnesses []string

	hub channel.HubFunctionalities

	attendees map[string]struct{}

	log zerolog.Logger
}

// NewChannel returns a new initialized consensus channel
func NewChannel(channelID string, hub channel.HubFunctionalities, log zerolog.Logger) channel.Channel {
	inbox := inbox.NewInbox(channelID)

	log = log.With().Str("channel", "consensus").Logger()

	return Channel{
		sockets:   channel.NewSockets(),
		inbox:     inbox,
		channelID: channelID,
		hub:       hub,
		attendees: make(map[string]struct{}),
		log:       log,
	}
}

// Subscribe is used to handle a subscribe message from the client
func (c *Channel) Subscribe(socket socket.Socket, msg method.Subscribe) error {
	c.log.Info().Str(msgID, strconv.Itoa(msg.ID)).Msg("received a subscribe")
	c.sockets.Upsert(socket)

	return nil
}

// Unsubscribe is used to handle an unsubscribe message.
func (c *Channel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	c.log.Info().Str(msgID, strconv.Itoa(msg.ID)).Msg("received an unsubscribe")

	ok := c.sockets.Delete(socketID)

	if !ok {
		return answer.NewError(-2, "client is not subscribed to this channel")
	}

	return nil
}

// Catchup is used to handle a catchup message.
func (c *Channel) Catchup(catchup method.Catchup) []message.Message {
	c.log.Info().Str(msgID, strconv.Itoa(catchup.ID)).Msg("received a catchup")

	return c.inbox.GetSortedMessages()
}
