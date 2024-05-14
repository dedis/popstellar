package method

import (
	"popstellar/message/query"
	"popstellar/message/query/method/message"
)

type ParamsRumor struct {
	SenderID string                     `json:"sender_id"`
	RumorID  int                        `json:"rumor_id"`
	Messages map[string]message.Message `json:"messages"`
}

// Rumor defines a JSON RPC rumor message
type Rumor struct {
	query.Base
	ID     int         `json:"id"`
	Params ParamsRumor `json:"params"`
}
