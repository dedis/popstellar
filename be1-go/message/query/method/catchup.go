package method

import "popstellar/message/query"

// Catchup define a JSON RPC catchup message
type Catchup struct {
	query.Base

	ID int `json:"id"`

	Params CatchupParams `json:"params"`
}

type CatchupParams struct {
	Channel string `json:"channel"`
}
