package method

import (
	"popstellar/message/query"
)

// GetMessagesById defines a JSON RPC getMessagesById message
type GetMessagesById struct {
	query.Base

	ID int `json:"id"`

	Params GetMessagesByIdParams `json:"params"`
}

type GetMessagesByIdParams map[string][]string
