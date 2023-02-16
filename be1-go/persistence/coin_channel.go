package persistence

// CoinState defines the state of a coin channel
type CoinState struct {
	ChannelPath string     `json:"channel_path"`
	Inbox       InboxState `json:"inbox"`
}
