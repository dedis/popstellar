package mfederation

import (
	"popstellar/internal/message/mmessage"
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
	return mmessage.FederationObject
}

// GetAction implements MessageData
func (FederationChallengeRequest) GetAction() string {
	return mmessage.FederationActionChallengeRequest
}

// NewEmpty implements MessageData
func (FederationChallengeRequest) NewEmpty() mmessage.MessageData {
	return &FederationChallengeRequest{}
}
