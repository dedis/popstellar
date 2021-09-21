package query

import message "student20_pop/message"

// Base defines all the common attributes for a Query RPC message
type Base struct {
	message.JSONRPCBase

	Method string `json:"method"`
}
