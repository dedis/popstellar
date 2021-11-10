package method

import "popstellar/message/query/method/message"

// Answer defines a JSON RPC catchup answer message
type Answer struct {
	JSONRPC string            `json:"jsonrpc"`
	ID      int               `json:"id"`
	Result  []message.Message `json:"result"`
}

// Result defines a JSON RPC result message
type Result struct {
	JSONRPC string `json:"jsonrpc"`
	ID      int    `json:"id"`
	Result  int    `json:"result"`
}
