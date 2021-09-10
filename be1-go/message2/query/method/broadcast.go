package method

import "student20_pop/message2/query/method/message"

// Broadcast ....
type Broadcast struct {
	Params struct {
		Channel string          `json:"channel"`
		Message message.Message `json:"message"`
	}
}
