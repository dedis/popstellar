package method

import "popstellar/internal/message/mquery"

// Unsubscribe defines a JSON RPC unsubscribe message
type Unsubscribe struct {
	mquery.Base

	ID int `json:"id"`

	Params UnsubscribeParams `json:"params"`
}

type UnsubscribeParams struct {
	Channel string `json:"channel"`
}
