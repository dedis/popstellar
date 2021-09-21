package method

import "popstellar/message/query"

// Unsubscribe ....
type Unsubscribe struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		Channel string `json:"channel"`
	} `json:"params"`
}
