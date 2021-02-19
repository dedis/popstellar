package message

import (
	"encoding/base64"
	"encoding/json"

	"golang.org/x/xerrors"
)

type Answer struct {
	ID     *int    `json:"id"`
	Result *Result `json:"result,omitempty"`
	Error  *Error  `json:"error,omitempty"`
}

type Result struct {
	General *int
	Catchup []Message
}

type Error struct {
	Code        int    `json:"code"`
	Description string `json:"description"`
}

type PublicKey []byte
type Signature []byte

func (p PublicKey) String() string {
	return base64.StdEncoding.EncodeToString(p)
}

type PublicKeySignaturePair struct {
	Witness   PublicKey `json:"witness"`
	Signature Signature `json:"signature"`
}

func (r *Result) MarshalJSON() ([]byte, error) {
	if r.General != nil {
		if *r.General == 0 {
			return json.Marshal(r.General)
		}
		return nil, xerrors.Errorf("invalid result value: %d", *r.General)
	}

	return json.Marshal(r.Catchup)
}

func (r *Result) UnmarshalJSON(data []byte) error {
	if len(data) == 0 {
		return xerrors.Errorf("failed to parse Result: empty buffer")
	}

	if len(data) == 1 && data[0] == '0' {
		val := 0
		r.General = &val
		return nil
	}

	catchup := []Message{}

	err := json.Unmarshal(data, &catchup)
	if err != nil {
		return xerrors.Errorf("failed to parse []Message: %v", err)
	}

	r.Catchup = catchup
	return nil
}

func (e *Error) Error() string {
	return e.Description
}

func (a Answer) MarshalJSON() ([]byte, error) {
	type internal struct {
		JSONRpc string  `json:"jsonrpc"`
		Result  *Result `json:"result,omitempty"`
		Error   *Error  `json:"error,omitempty"`
	}

	tmp := internal{
		JSONRpc: "2.0",
		Result:  a.Result,
		Error:   a.Error,
	}

	return json.Marshal(tmp)
}
