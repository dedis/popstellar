package method

import (
	"popstellar/message/query"
)

// GetMessagesById defines a JSON RPC getMessagesById message
type GetMessagesById struct {
	query.Base

	ID int `json:"id"`

	Params map[string][]string `json:"params"`
}
