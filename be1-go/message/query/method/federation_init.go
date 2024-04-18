package method

import "popstellar/message/query"

// FederationInit defines a JSON RPC federationInit message
type FederationInit struct {
	query.Base

	Params struct {
		LaoID         string              `json:"lao_id"`
		ServerAddress string              `json:"server_address"`
		PublicKey     string              `json:"public_key"`
		Challenge     FederationChallenge `json:"challenge"`
	} `json:"params"`
}
