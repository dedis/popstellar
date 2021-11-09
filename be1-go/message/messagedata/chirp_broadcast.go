package messagedata

type ChirpBroadcast struct {
Object string `json:"object"`
Action string `json:"action"`
ChirpId string `json:"chirp_id"`
Channel string `json:"channel"`
Timestamp int64 `json:"timestamp"`
}
