package mfederation

import (
	"popstellar/internal/handler/channel"
)

// FederationChallengeRequest defines a message data
type FederationChallengeRequest struct {
	Object string `json:"object"`
	Action string `json:"action"`

	// Timestamp is a Unix timestamp
	Timestamp int64 `json:"timestamp"`
}

// GetObject implements MessageData
func (FederationChallengeRequest) GetObject() string {
	return channel.FederationObject
}

// GetAction implements MessageData
func (FederationChallengeRequest) GetAction() string {
	return channel.FederationActionChallengeRequest
}

// NewEmpty implements MessageData
func (FederationChallengeRequest) NewEmpty() channel.MessageData {
	return &FederationChallengeRequest{}
}
