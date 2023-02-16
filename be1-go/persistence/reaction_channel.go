package persistence

// ReactionState defines the state of a reaction channel
type ReactionState struct {
	ChannelPath string     `json:"channel_path"`
	Inbox       InboxState `json:"inbox"`
	Attendees   []string   `json:"attendees"`
}
