package messagedata

// ChirpAdd defines a message data
type ChirpAdd struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	Text      string `json:"text"`
	Timestamp int64  `json:"timestamp"`
}
