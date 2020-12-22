package message

import "encoding/json"

type ErrorResponse struct {
	Code        int
	Description string
}

//We still need ResponseIDNotDecoded and ResponseWithGenResult because we cannot omit integers
type ResponseIDNotDecoded struct {
	Jsonrpc       string
	ErrorResponse json.RawMessage
	Id            []byte
}
type ResponseWithGenResult struct {
	Jsonrpc string
	Result  int
	Id      int
}
type Response struct {
	Jsonrpc       string
	ErrorResponse json.RawMessage `json:",omitempty"`
	CatchupResult string `json:",omitempty"`
	Id            int
}
