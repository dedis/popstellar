package mrumorstate

import (
	"popstellar/internal/handler/query/mquery"
)

// Rumor defines a JSON RPC rumor message
type RumorState struct {
	mquery.Base
	ID     int            `json:"id"`
	Params map[string]int `json:"params"`
}
