package mrumor

import (
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/message/mquery"
)

type ParamsRumor struct {
	SenderID string                        `json:"sender_id"`
	RumorID  int                           `json:"rumor_id"`
	Messages map[string][]mmessage.Message `json:"messages"`
}

// Rumor defines a JSON RPC rumor message
type Rumor struct {
	mquery.Base
	ID     int         `json:"id"`
	Params ParamsRumor `json:"params"`
}
