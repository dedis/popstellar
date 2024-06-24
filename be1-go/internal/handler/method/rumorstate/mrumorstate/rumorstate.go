package mrumorstate

import (
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/query/mquery"
)

// RumorState defines a JSON RPC rumor state message
type RumorState struct {
	mquery.Base
	ID     int              `json:"id"`
	Params RumorStateParams `json:"params"`
}

type RumorStateParams struct {
	State mrumor.RumorTimestamp `json:"state"`
}
