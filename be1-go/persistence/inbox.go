package persistence

import "popstellar/message/query/method/message"

// InboxState defines the state of an inbox
type InboxState struct {
	ChannelID string         `json:"channel_id"`
	State     []MessageState `json:"messages_states"`
}

// MessageState contains a message and its time of storing in the inbox
type MessageState struct {
	Message    message.Message `json:"message"`
	StoredTime int64           `json:"stored_time"`
}
