package method

import (
	"popstellar/message/query"
	"popstellar/message/query/method/message"
)

// GetMessagesById defines a JSON RPC getMessagesById message
type GetMessagesById struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		MessageIdsByChannelId *message.MessageIdsByChannelId `json:"message_ids_by_channel_id"`
	} `json:"params"`
}
