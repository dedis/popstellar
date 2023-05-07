package method

import "popstellar/message/query"

type ServerInfo struct {
	PublicKey     string `json:"public_key"`
	ServerAddress string `json:"server_address"`
	ClientAddress string `json:"client_address"`
}

// GreetServer defines a JSON RPC greetServer message
type GreetServer struct {
	query.Base

	ID int `json:"id"`

	Params ServerInfo `json:"params"`
}
