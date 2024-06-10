package method

import (
	"popstellar/internal/message/mmessage"
	"popstellar/internal/message/query"
)

// Publish defines a JSON RPC publish message
type Publish struct {
	query.Base

	ID int `json:"id"`

	Params PublishParams `json:"params"`
}

type PublishParams struct {
	Channel string           `json:"channel"`
	Message mmessage.Message `json:"message"`
}
