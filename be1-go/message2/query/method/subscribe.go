package method

import "student20_pop/message2/query"

// Subscribe ...
type Subscribe struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		Channel string `json:"channel"`
	} `json:"params"`
}
