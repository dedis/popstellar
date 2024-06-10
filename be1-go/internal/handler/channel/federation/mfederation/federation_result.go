package mfederation

import (
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/message/mmessage"
)

// FederationResult defines a message data
type FederationResult struct {
	Object string `json:"object"`
	Action string `json:"action"`
	Status string `json:"status"`

	Reason string `json:"reason,omitempty"`

	PublicKey    string           `json:"public_key,omitempty"`
	ChallengeMsg mmessage.Message `json:"challenge"`
}

// GetObject implements MessageData
func (FederationResult) GetObject() string {
	return messagedata.FederationObject
}

// GetAction implements MessageData
func (FederationResult) GetAction() string {
	return messagedata.FederationActionResult
}

// NewEmpty implements MessageData
func (FederationResult) NewEmpty() messagedata.MessageData {
	return &FederationResult{}
}
