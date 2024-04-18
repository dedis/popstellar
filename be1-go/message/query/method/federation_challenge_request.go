package method

import "popstellar/message/query"

// FederationChallengeRequest defines a JSON RPC federationChallengeRequest message
type FederationChallengeRequest struct {
	query.Base

	Params struct {
		timestamp string `json:"timestamp"`
	} `json:"params"`
}
