package method

import "student20_pop/message/query"

// Catchup ....
type Catchup struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		Channel string `json:"channel"`
	} `json:"params"`
}
