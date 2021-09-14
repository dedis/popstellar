package method

import (
	"student20_pop/message2/query"
	"student20_pop/message2/query/method/message"
)

// Broadcast ....
type Broadcast struct {
	query.Base

	Params struct {
		Channel string          `json:"channel"`
		Message message.Message `json:"message"`
	} `json:"params"`
}
