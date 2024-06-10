package method

import (
	"popstellar/internal/message/mmessage"
	"popstellar/internal/message/query"
)

// Broadcast defines a JSON RPC broadcast message
type Broadcast struct {
	query.Base

	Params BroadcastParams `json:"params"`
}

type BroadcastParams struct {
	Channel string           `json:"channel"`
	Message mmessage.Message `json:"message"`
}
