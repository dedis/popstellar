package method

import (
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method/message"
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
