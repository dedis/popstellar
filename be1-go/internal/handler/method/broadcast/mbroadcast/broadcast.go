package mbroadcast

import (
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/query/mquery"
)

// Broadcast defines a JSON RPC broadcast message
type Broadcast struct {
	mquery.Base

	Params BroadcastParams `json:"params"`
}

type BroadcastParams struct {
	Channel string           `json:"channel"`
	Message mmessage.Message `json:"message"`
}
