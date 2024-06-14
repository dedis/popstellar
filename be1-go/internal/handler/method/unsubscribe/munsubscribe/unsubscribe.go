package munsubscribe

import (
	"popstellar/internal/handler/query/mquery"
)

// Unsubscribe defines a JSON RPC unsubscribe message
type Unsubscribe struct {
	mquery.Base

	ID int `json:"id"`

	Params UnsubscribeParams `json:"params"`
}

type UnsubscribeParams struct {
	Channel string `json:"channel"`
}
