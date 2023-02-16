package persistence

// ChirpState defines the state of a chirp channel
type ChirpState struct {
	ChannelPath string     `json:"channel_path"`
	OwnerKey    string     `json:"owner_string"`
	Inbox       InboxState `json:"inbox"`
}
