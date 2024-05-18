package method

import (
	"popstellar/message/query"
	"popstellar/message/query/method/message"
)

// Publish defines a JSON RPC publish message
type Publish struct {
	query.Base

	ID int `json:"id"`

	Params PublishParams `json:"params"`
}

type PublishParams struct {
	Channel string          `json:"channel"`
	Message message.Message `json:"message"`
}
