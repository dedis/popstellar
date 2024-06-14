package trumor

type RumorTimestamp map[string]int

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
