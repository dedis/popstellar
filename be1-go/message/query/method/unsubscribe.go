package method

import "popstellar/message/query"

// Unsubscribe defines a JSON RPC unsubscribe message
type Unsubscribe struct {
	query.Base

	ID int `json:"id"`

	Params UnsubscribeParams `json:"params"`
}

type UnsubscribeParams struct {
	Channel string `json:"channel"`
}
