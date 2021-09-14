package query

import "student20_pop/message2"

// Base defines all the common attributes for a Query RPC message
type Base struct {
	message2.JSONRPCBase

	Method string `json:"method"`
}
