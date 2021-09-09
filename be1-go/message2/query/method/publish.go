package method

import "student20_pop/message2/query/method/message"

// Publish ...
type Publish struct {
	ID int

	Params struct {
		Channel string
		Message message.Message
	}
}
