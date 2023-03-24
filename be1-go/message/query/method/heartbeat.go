package method

import (
	"popstellar/message/query"
	"popstellar/message/query/method/message"
)

// Heartbeat defines a JSON RPC heartbeat message
type Heartbeat struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		MessageIdsByChannelId *message.MessageIdsByChannelId `json:"message_ids_by_channel_id"`
	} `json:"params"`
}
