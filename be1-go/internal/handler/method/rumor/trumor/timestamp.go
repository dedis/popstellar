package trumor

import (
	"popstellar/internal/handler/method/rumor/mrumor"
)

type RumorTimestamp map[string]int

func (r RumorTimestamp) IsValid(rumor mrumor.Rumor) bool {
	myRumorID, ok := r[rumor.Params.SenderID]
	if !ok && rumor.Params.RumorID != 0 {
		return false
	}
	if ok && myRumorID+1 != rumor.Params.RumorID {
		return false
	}

	for senderID, rumorID := range rumor.Params.Timestamp {
		if senderID == rumor.Params.SenderID {
			continue
		}

		myRumorID, ok := r[senderID]
		if !ok {
			return false
		}
		if myRumorID < rumorID {
			return false
		}
	}

	return true
}
