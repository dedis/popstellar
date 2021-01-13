/*structs to manage received Json messages while Unmarshalling and decoding messages.*/
package message

// []byte are automatically decoded from base64 when unmarshalled, while string (and json.RawMessage) are NOT

import (
	"encoding/json"
)

/* potential enum, but doesn't type check in go, the checks must still be manual, so kinda useless
type Method string
const(
	Subscribe Method = "subscribe"
	Unsubscribe Method = "unsubscribe"
	Broadcast Method = "broadcast"
	Publish Method = "publish"
	Catchup Method = "catchup"
)*/

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
	Data              []byte            `json:"data"`       // recovered from base 64
	Sender            []byte            `json:"sender"`     // recovered from base 64
	Signature         []byte            `json:"signature"`  // recovered from base 64
	MessageId         []byte            `json:"message_id"` // recovered from base 64
	WitnessSignatures []json.RawMessage `json:"witnessSignatures"`
}

type ItemWitnessSignatures struct {
	WitnessKey []byte `json:"witness"` // recovered from base 64
	//Sign(message_id)
	Signature []byte `json:"signature"` // recovered from base 64
}
