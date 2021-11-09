package method

// ServerCatchupAnswer defines a JSON RPC catchup answer message
type ServerCatchupAnswer struct {
	JSONRPC string   `json:"jsonrpc"`
	ID      int      `json:"id"`
	Result  []string `json:"result"`
}

// ServerResult defines a JSON RPC result message
type ServerResult struct {
	JSONRPC string `json:"jsonrpc"`
	ID      int    `json:"id"`
	Result  int    `json:"result"`
}
