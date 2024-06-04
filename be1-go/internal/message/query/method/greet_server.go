package method

import "popstellar/internal/message/query"

type GreetServerParams struct {
	PublicKey     string `json:"public_key"`
	ServerAddress string `json:"server_address"`
	ClientAddress string `json:"client_address"`
}

// GreetServer defines a JSON RPC greetServer message
type GreetServer struct {
	query.Base
	Params GreetServerParams `json:"params"`
}
