package persistence

import "popstellar/message/messagedata"

// ConsensusState defines the state of a consensus channel
type ConsensusState struct {
	ChannelPath string          `json:"channel_path"`
	Inbox       InboxState      `json:"inbox"`
	Attendees   []string        `json:"attendees"`
	Instances   []InstanceState `json:"instances"`
}

// InstanceState defines the state of a consensus instance
type InstanceState struct {
	ID       string `json:"id"`
	Role     string `json:"role"`
	LastSent string `json:"last_sent"`

	ProposedTry int64 `json:"proposed_try"`
	PromisedTry int64 `json:"promised_try"`
	AcceptedTry int64 `json:"accepted_try"`

	ProposedValue bool `json:"proposed_value"`
	AcceptedValue bool `json:"accepted_value"`

	Decided  bool `json:"decided"`
	Decision bool `json:"decision"`

	Promises []PromiseMessage `json:"promises"`
	Accepts  []AcceptMessage  `json:"accepts"`

	ElectInstances []ElectState `json:"elect_instances"`
}

// ElectState defines the state of an elect instance
type ElectState struct {
	ID                string   `json:"id"`
	AcceptorNumber    int      `json:"acceptor_number"`
	Failed            bool     `json:"failed"`
	PositiveAcceptors []string `json:"positive_acceptors"`
	NegativeAcceptors []string `json:"negative_acceptors"`
}

// PromiseMessage contains a promise message and its signature
type PromiseMessage struct {
	Message   messagedata.ConsensusPromise `json:"message"`
	Signature string                       `json:"signature"`
}

// AcceptMessage contains a promise message and its signature
type AcceptMessage struct {
	Message   messagedata.ConsensusAccept `json:"message"`
	Signature string                      `json:"signature"`
}
