package mcatchup

import (
	"popstellar/internal/handler/query/mquery"
)

// Catchup define a JSON RPC catchup message
type Catchup struct {
	mquery.Base

	ID int `json:"id"`

	Params CatchupParams `json:"params"`
}

type CatchupParams struct {
	Channel string `json:"channel"`
}
