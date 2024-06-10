package mheartbeat

import (
	"popstellar/internal/message/mquery"
)

// Heartbeat defines a JSON RPC heartbeat message
type Heartbeat struct {
	mquery.Base
	Params map[string][]string `json:"params"`
}
