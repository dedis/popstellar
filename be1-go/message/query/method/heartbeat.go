package method

import (
	"popstellar/message/query"
)

// Heartbeat defines a JSON RPC heartbeat message
type Heartbeat struct {
	query.Base
	Params HeartbeatParams `json:"params"`
}

type HeartbeatParams map[string][]string
