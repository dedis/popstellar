package method

import (
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method/message"
)

// Broadcast defines a JSON RPC broadcast message
type Broadcast struct {
	query.Base

	Params BroadcastParams `json:"params"`
}

type BroadcastParams struct {
	Channel string          `json:"channel"`
	Message message.Message `json:"message"`
}
