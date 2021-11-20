package messagedata

// ChirpBroadcast defines a message data
type ChirpBroadcast struct {
	Object string `json:"object"`
	Action string `json:"action"`
	ChirpId string `json:"chirp_id"`
	Channel string `json:"channel"`
	Timestamp int64 `json:"timestamp"`
}
