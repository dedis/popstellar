package method

import "popstellar/message/query"

type Challenge struct {
	value       string `json:"value"`
	valid_until int    `json:"valid_until"`
}

// FederationInit defines a JSON RPC federationInit message
type FederationInit struct {
	query.Base

	Params struct {
		LaoID         string    `json:"lao_id"`
		ServerAddress string    `json:"server_address"`
		PublicKey     string    `json:"public_key"`
		challenge     Challenge `json:"challenge"`
	} `json:"params"`
}
