package message

import "encoding/json"

type ErrorResponse struct {
	Code        int    `json:"code"`
	Description string `json:"description"`
}

//We still need ResponseIDNotDecoded and ResponseWithGenResult because we cannot omit integers

type ResponseIDNotDecoded struct {
	Jsonrpc       string          `json:"jsonrpc"`
	ErrorResponse json.RawMessage `json:"result,omitempty"`
	Id            []byte          `json:"id"`
}

type ResponseWithGenResult struct {
	Jsonrpc string `json:"jsonrpc"`
	Result  int    `json:"result"`
	Id      int    `json:"id"`
}

type Response struct {
	Jsonrpc       string          `json:"jsonrpc"`
	ErrorResponse json.RawMessage `json:"error,omitempty"`
	CatchupResult string          `json:"result,omitempty"`
	Id            int             `json:"id"`
}
