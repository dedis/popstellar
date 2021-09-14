package method

import (
	"student20_pop/message2/query"
	"student20_pop/message2/query/method/message"
)

// Publish ...
type Publish struct {
	query.Base

	ID int `json:"id"`

	Params struct {
		Channel string          `json:"channel"`
		Message message.Message `json:"message"`
	} `json:"params"`
}
