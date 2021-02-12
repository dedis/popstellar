package message

import (
	"encoding/json"

	"golang.org/x/xerrors"
)

type Answer struct {
	ID     *int    `json:"id"`
	Result *Result `json:"result"`
	Error  *Error  `json:"error"`
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

type PublicKeySignaturePair struct {
	Witness   PublicKey `json:"witness"`
	Signature Signature `json:"signature"`
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
