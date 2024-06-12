package mjsonrpc

import (
	"encoding/json"
	"popstellar/internal/errors"
)

const (
	// RPCTypeQuery defines the type for a Query RPC message
	RPCTypeQuery RPCType = iota

	// RPCTypeAnswer defines the type for an Answer RPC message
	RPCTypeAnswer

	// RPCUnknown fines the type for an unknown RPC message
	RPCUnknown
)

// RPCType defines the type for a JSON RPC message.
type RPCType int

// JSONRPCBase defines all the common attributes of a JSON RPC message.
type JSONRPCBase struct {
	JSONRPC string `json:"jsonrpc"`
}

// GetType returns the type of RPC message based on a json buffer.
func GetType(jsonbuf []byte) (RPCType, error) {
	var objmap map[string]json.RawMessage

	err := json.Unmarshal(jsonbuf, &objmap)
	if err != nil {
		return RPCUnknown, errors.NewInvalidMessageFieldError("failed to get objmap: %v", err)
	}

	if objmap["method"] != nil {
		return RPCTypeQuery, nil
	} else {
		return RPCTypeAnswer, nil
	}
}
