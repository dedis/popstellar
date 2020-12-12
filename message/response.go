package message

import "encoding/json"

type ErrorResponse struct {
	Code        int
	Description string
}

type ResponseWithGenResult struct {
	Jsonrpc string
	Result  int
	Id      int
}
type ResponseWithCatchupResult struct {
	Jsonrpc string
	Result  string
	Id      int
}
type ResponseWithError struct {
	Jsonrpc       string
	ErrorResponse json.RawMessage
	Id            int
}
type ResponseIDNotDecoded struct {
	Jsonrpc       string
	ErrorResponse json.RawMessage
	Id            []byte
}
