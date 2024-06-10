package mfederation

import (
	"popstellar/internal/handler/message/mmessage"
)

// FederationInit defines a message data
type FederationInit struct {
	Object string `json:"object"`
	Action string `json:"action"`

	LaoId         string           `json:"lao_id"`
	ServerAddress string           `json:"server_address"`
	PublicKey     string           `json:"public_key"`
	ChallengeMsg  mmessage.Message `json:"challenge"`
}

// GetObject implements MessageData
func (FederationInit) GetObject() string {
	return mmessage.FederationObject
}

// GetAction implements MessageData
func (FederationInit) GetAction() string {
	return mmessage.FederationActionInit
}

// NewEmpty implements MessageData
func (FederationInit) NewEmpty() mmessage.MessageData {
	return &FederationInit{}
}
