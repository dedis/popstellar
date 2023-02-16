package persistence

// GeneralChirpingState defines the state of a general chirping channel
type GeneralChirpingState struct {
	ChannelPath string     `json:"channel_path"`
	Inbox       InboxState `json:"inbox"`
}
