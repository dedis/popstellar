package mrumor

import (
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/query/mquery"
)

type ParamsRumor struct {
	SenderID  string                        `json:"sender_id"`
	RumorID   int                           `json:"rumor_id"`
	Timestamp map[string]int                `json:"timestamp"`
	Messages  map[string][]mmessage.Message `json:"messages"`
}

// Rumor defines a JSON RPC rumor message
type Rumor struct {
	mquery.Base
	ID     int         `json:"id"`
	Params ParamsRumor `json:"params"`
}

func (r *Rumor) IsSmallerOrEqual(rumor Rumor) bool {
	isSmallerOrEqual := true
	isIndependant := true

	i, ok := rumor.Params.Timestamp[r.Params.SenderID]
	if ok {
		return r.Params.RumorID <= i
	}

	for rSenderID, rRumorID := range r.Params.Timestamp {
		rumorID, ok := rumor.Params.Timestamp[rSenderID]
		if !ok {
			continue
		}

		isIndependant = false

		if rumorID < rRumorID {
			isSmallerOrEqual = false
		}
	}

	if isIndependant {
		return r.GetTimestampSum() < rumor.GetTimestampSum()
	}

	return isSmallerOrEqual && r.GetTimestampSum() < rumor.GetTimestampSum()
}

func (r *Rumor) GetTimestampSum() int {
	sum := 0

	for _, i := range r.Params.Timestamp {
		sum += i
	}

	return sum
}
