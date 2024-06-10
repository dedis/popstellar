package method

import (
	"popstellar/internal/message/mmessage"
	"popstellar/internal/message/query"
)

type ParamsRumor struct {
	SenderID string                        `json:"sender_id"`
	RumorID  int                           `json:"rumor_id"`
	Messages map[string][]mmessage.Message `json:"messages"`
}

// Rumor defines a JSON RPC rumor message
type Rumor struct {
	query.Base
	ID     int         `json:"id"`
	Params ParamsRumor `json:"params"`
}
