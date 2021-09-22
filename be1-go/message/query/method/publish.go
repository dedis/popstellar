package method

import (
	"popstellar/message/query"
	"popstellar/message/query/method/message"
)

// Publish defines a JSON RPC publish message
type Publish struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		Channel string          `json:"channel"`
		Message message.Message `json:"message"`
	} `json:"params"`
}
