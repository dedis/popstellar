package method

import "popstellar/message/query"

// Subscribe defines a JSON RPC subscribe message
type Subscribe struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		Channel string `json:"channel"`
	} `json:"params"`
}
