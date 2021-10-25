package messagedata

// ChirpAddBroadcast defines a message data
type ChirpAddBroadcast struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	PostId    string `json:"post_id"`
	Channel   string `json:"channel"`
	Timestamp int64    `json:"timestamp"`
}
