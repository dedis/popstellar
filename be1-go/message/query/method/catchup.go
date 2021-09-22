package method

import "popstellar/message/query"

// Catchup define a JSON RPC catchup message
type Catchup struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		Channel string `json:"channel"`
	} `json:"params"`
}
