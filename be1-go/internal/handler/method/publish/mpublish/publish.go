package mpublish

import (
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/query/mquery"
)

// Publish defines a JSON RPC publish message
type Publish struct {
	mquery.Base

	ID int `json:"id"`

	Params PublishParams `json:"params"`
}

type PublishParams struct {
	Channel string           `json:"channel"`
	Message mmessage.Message `json:"message"`
}
