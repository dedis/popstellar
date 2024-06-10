package method

import "popstellar/internal/message/mquery"

type GreetServerParams struct {
	PublicKey     string `json:"public_key"`
	ServerAddress string `json:"server_address"`
	ClientAddress string `json:"client_address"`
}

// GreetServer defines a JSON RPC greetServer message
type GreetServer struct {
	mquery.Base
	Params GreetServerParams `json:"params"`
}
