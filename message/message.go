/*functions to manage received Json messages. Unmarshalls and decode messages.*/
package message

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
	Channel string
}

type ParamsIncludingMessage struct {
	Channel string
	Message json.RawMessage
}

type Message struct {
	Data              json.RawMessage `json:"data"` // in base 64
	Sender            string          `json:"sender"`
	Signature         string          `json:"signature"`
	MessageId         string          `json:"message_id"`
	WitnessSignatures []string        `json:"witnessSignatures"`
}

type ItemWitnessSignatures struct {
	Witness   string
	Signature string
}
