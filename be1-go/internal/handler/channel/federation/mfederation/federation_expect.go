package mfederation

import (
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/message/mmessage"
)

// FederationExpect defines a message data
type FederationExpect struct {
	Object string `json:"object"`
	Action string `json:"action"`

	LaoId         string           `json:"lao_id"`
	ServerAddress string           `json:"server_address"`
	PublicKey     string           `json:"public_key"`
	ChallengeMsg  mmessage.Message `json:"challenge"`
}

// GetObject implements MessageData
func (FederationExpect) GetObject() string {
	return channel.FederationObject
}

// GetAction implements MessageData
func (FederationExpect) GetAction() string {
	return channel.FederationActionExpect
}

// NewEmpty implements MessageData
func (FederationExpect) NewEmpty() channel.MessageData {
	return &FederationExpect{}
}
