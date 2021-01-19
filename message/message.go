/*structs to manage received Json messages while Unmarshalling and decoding messages.*/
package message

// []byte are automatically decoded from base64 when unmarshalled, while string (and json.RawMessage) are NOT

import (
	"encoding/json"
)


/*Most generic message structure*/
type GenericMessage map[string]interface{}

type Query struct {
	Jsonrpc string          `json:"jsonrpc"`
	Method  string          `json:"method"`
	Params  json.RawMessage `json:"params"`
	Id      int             `json:"id"`
}

type Params struct {
	Channel string          `json:"channel"`
	Message json.RawMessage `json:"message,omitempty"`
}

type Message struct {
	Data              []byte            `json:"data"`       // decoded from base 64
	Sender            []byte            `json:"sender"`     // decoded from base 64
	Signature         []byte            `json:"signature"`  // decoded from base 64
	MessageId         []byte            `json:"message_id"` // decoded from base 64
	WitnessSignatures []json.RawMessage `json:"witnessSignatures"`
}

type ItemWitnessSignatures struct {
	WitnessKey []byte `json:"witness"` // decoded from base 64
	//Sign(message_id)
	Signature []byte `json:"signature"` // decoded from base 64
}
