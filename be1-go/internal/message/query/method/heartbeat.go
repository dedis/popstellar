package method

import (
	"popstellar/internal/message/query"
)

// Heartbeat defines a JSON RPC heartbeat message
type Heartbeat struct {
	query.Base
	Params map[string][]string `json:"params"`
}
