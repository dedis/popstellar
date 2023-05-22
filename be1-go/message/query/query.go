package query

import message "popstellar/message"

const (
	MethodPublish         = "publish"
	MethodSubscribe       = "subscribe"
	MethodUnsubscribe     = "unsubscribe"
	MethodCatchUp         = "catchup"
	MethodBroadcast       = "broadcast"
	MethodHeartbeat       = "heartbeat"
	MethodGetMessagesById = "get_messages_by_id"
	MethodGreetServer     = "greet_server"
)

// Base defines all the common attributes for a Query RPC message
type Base struct {
	message.JSONRPCBase

	Method string `json:"method"`
}
