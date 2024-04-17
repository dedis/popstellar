package method

import "popstellar/message/query"

// FederationChallenge defines a JSON RPC federationChallenge message
type FederationChallenge struct {
	query.Base

	Params struct {
		value       string `json:"value"`
		valid_until int    `json:"valid_until"`
	} `json:"params"`
}
