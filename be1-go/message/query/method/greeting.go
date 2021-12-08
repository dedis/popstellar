package method

import (
	"popstellar/message/query"
)

// Greeting defines a JSON RPC greeting message
type Greeting struct {
	query.Base

	Params struct {
		Channel string          `json:"channel"`
		Sender string 			`json:"sender"`
		Address string 			`json:"address"`
	} `json:"params"`
}
