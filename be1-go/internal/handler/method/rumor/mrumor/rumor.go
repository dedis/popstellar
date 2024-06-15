package mrumor

import (
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/query/mquery"
)

type RumorTimestamp map[string]int

type ParamsRumor struct {
	SenderID  string                        `json:"sender_id"`
	RumorID   int                           `json:"rumor_id"`
	Timestamp RumorTimestamp                `json:"timestamp"`
	Messages  map[string][]mmessage.Message `json:"messages"`
}

// Rumor defines a JSON RPC rumor message
type Rumor struct {
	mquery.Base
	ID     int         `json:"id"`
	Params ParamsRumor `json:"params"`
}

func (r *Rumor) IsBefore(other Rumor) bool {
	greater := false
	smaller := false
	for senderID, rumorID := range r.Params.Timestamp {
		otherRumorID, ok := other.Params.Timestamp[senderID]
		if !ok || otherRumorID < rumorID {
			smaller = true
		}
		if otherRumorID > rumorID {
			greater = true
		}
	}

	for senderID, rumorID := range other.Params.Timestamp {
		myRumorID, ok := r.Params.Timestamp[senderID]
		if !ok || myRumorID < rumorID {
			greater = true
		}
		if myRumorID > rumorID {
			smaller = true
		}
	}

	if smaller || !greater {
		return false
	}
	return true
}

func (r RumorTimestamp) IsValid(timestamp map[string]int) bool {
	for senderID, rumorID := range timestamp {
		myRumorID, ok := r[senderID]
		if !ok && rumorID != 0 {
			return false
		}
		if myRumorID < rumorID {
			return false
		}
	}

	return true
}
