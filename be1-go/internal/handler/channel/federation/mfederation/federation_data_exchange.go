package mfederation

import (
	"popstellar/internal/handler/channel"
)

// FederationTokensExchange defines a message data
type FederationTokensExchange struct {
	Object string `json:"object"`
	Action string `json:"action"`

	LaoId      string   `json:"lao_id"`
	RollcallId string   `json:"roll_call_id"`
	Timestamp  int64    `json:"timestamp"`
	Tokens     []string `json:"tokens"`
}

// GetObject implements MessageData
func (FederationTokensExchange) GetObject() string {
	return channel.FederationObject
}

// GetAction implements MessageData
func (FederationTokensExchange) GetAction() string {
	return channel.FederationActionTokensExchange
}

// NewEmpty implements MessageData
func (FederationTokensExchange) NewEmpty() channel.MessageData {
	return &FederationTokensExchange{}
}
