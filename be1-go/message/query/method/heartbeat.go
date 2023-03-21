package method

import (
	"popstellar/message/query"
)

// Heartbeat defines a JSON RPC heartbeat message
type Heartbeat struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		Maps *struct {
			Channel string  `json:"channel"`
			Ids     *string `json:"ids"`
		} `json:"maps"`
	}
}
