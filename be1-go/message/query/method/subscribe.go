package method

import "popstellar/message/query"

// Subscribe defines a JSON RPC subscribe message
type Subscribe struct {
	query.Base

	ID int `json:"id"`

	Params SubscribeParams `json:"params"`
}

type SubscribeParams struct {
	Channel string `json:"channel"`
}
