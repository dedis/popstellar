package mquery

import (
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
)

const (
	MethodPublish         = "publish"
	MethodSubscribe       = "subscribe"
	MethodUnsubscribe     = "unsubscribe"
	MethodCatchUp         = "catchup"
	MethodBroadcast       = "broadcast"
	MethodHeartbeat       = "heartbeat"
	MethodGetMessagesById = "get_messages_by_id"
	MethodGreetServer     = "greet_server"
	MethodRumor           = "rumor"
)

// Base defines all the common attributes for a Query RPC message
type Base struct {
	mjsonrpc.JSONRPCBase

	Method string `json:"method"`
}
