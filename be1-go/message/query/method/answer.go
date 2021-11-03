package method

// Answer defines a JSON RPC answer message
type Answer struct {
	JSONRPC string
	ID      int
	Result  []Publish
}
