package method

import "popstellar/message/query"


// FederationExpect defines a JSON RPC federationExpect message
type FederationExpect struct {
	query.Base

	Params struct {
		LaoID         string              `json:"lao_id"`
		ServerAddress string              `json:"server_address"`
		PublicKey     string              `json:"public_key"`
		challenge     FederationChallenge `json:"challenge"`
	} `json:"params"`
}
