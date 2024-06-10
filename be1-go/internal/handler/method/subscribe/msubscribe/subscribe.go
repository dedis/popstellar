package msubscribe

import (
	"popstellar/internal/handler/query/mquery"
)

// Subscribe defines a JSON RPC subscribe message
type Subscribe struct {
	mquery.Base

	ID int `json:"id"`

	Params SubscribeParams `json:"params"`
}

type SubscribeParams struct {
	Channel string `json:"channel"`
}
