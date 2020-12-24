/*structs to manage received Json messages while Unmarshalling and decoding messages.*/
package message

// []byte are automatically decoded from base64 when unmarshalled, while string (and json.RawMessage) are NOT

import (
	"encoding/json"
)

/* potential enum, but doesn't typecheck in go, the checks must still be manual, so kinda useless
type Method string
const(
	Subscribe Method = "subscribe"
	Unsubscribe Method = "unsubscribe"
	Broadcast Method = "message"
	Publish Method = "publish"
	Catchup Method = "catchup"
)*/

/*Most generic message structure*/
type GenericMessage map[string]interface{}

type Query struct {
	Jsonrpc string
	Method  string
	Params  json.RawMessage
	Id      int
}

type Params struct {
	Channel string          `json:"channel"`
	Message json.RawMessage `json:"message,omitempty"`
}

type Message struct {
	Data              []byte            `json:"data"` // in base 64
	Sender            []byte            `json:"sender"`
	Signature         []byte            `json:"signature"`
	MessageId         []byte            `json:"message_id"`
	WitnessSignatures []json.RawMessage `json:"witnessSignatures"`
}

type ItemWitnessSignatures struct {
	WitnessKey []byte
	//Sign(message_id)
	Signature  []byte
}
