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
	return messagedata.FederationObject
}

// GetAction implements MessageData
func (FederationChallengeRequest) GetAction() string {
	return messagedata.FederationActionChallengeRequest
}

// NewEmpty implements MessageData
func (FederationChallengeRequest) NewEmpty() messagedata.MessageData {
	return &FederationChallengeRequest{}
}
