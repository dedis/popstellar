package method

import (
	"popstellar/internal/message/mquery"
)

// GetMessagesById defines a JSON RPC getMessagesById message
type GetMessagesById struct {
	mquery.Base

	ID int `json:"id"`

	Params map[string][]string `json:"params"`
}
