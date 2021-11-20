package messagedata

// ChirpDelete defines a message data
type ChirpDelete struct {
	Object    string `json:"object"`
	Action    string `json:"action"`
	ChirpId   string `json:"chirp_id"`
	Timestamp int    `json:"timestamp"`
}
