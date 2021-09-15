package message

import (
	"encoding/base64"
	"encoding/json"
	"fmt"

	"golang.org/x/xerrors"
)

// Answer represents an `Answer` message. It may contain either a `Result` or
// an `Error`.
type Answer struct {
	ID     *int    `json:"id"`
	Result *Result `json:"result,omitempty"`
	Error  *Error  `json:"error,omitempty"`
}

// Result represents a `Result` message.
type Result struct {
	General *int
	Catchup []Message
}

// Error represents an `Error` message.
type Error struct {
	Code        int    `json:"code"`
	Description string `json:"description"`
}

// PublicKey represents a user's public key.
type PublicKey Base64URLBytes

// Signature represents a signature.
type Signature Base64URLBytes

func (s Signature) Encode() string {
	return base64.URLEncoding.EncodeToString(s)
}

// String returns the base64 encoded representation of the public key.
func (p PublicKey) String() string {
	return base64.URLEncoding.EncodeToString(p)
}

// PublicKeySignaturePair represents a witness' public key and it's signature.
type PublicKeySignaturePair struct {
	Witness   PublicKey `json:"witness"`
	Signature Signature `json:"signature"`
}

// MarshalJSON implements custom JSON marshaling for a `Result` message.
func (r *Result) MarshalJSON() ([]byte, error) {
	if r.General != nil {
		if *r.General == 0 {
			return json.Marshal(r.General)
		}
		return nil, xerrors.Errorf("invalid result value: %d", *r.General)
	}

	return json.Marshal(r.Catchup)
}

// UnmarshalJSON implements custom JSON unmarshaling for a `Result`.
// It sets the value of `Result.General` or `Result.Catchup` depending on th
// result type.
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

// Error returns the string representation of an Error message.
func (e *Error) Error() string {
	return e.Description
}

// NewError returns a *message.Error
func NewError(code int, description string) *Error {
	return &Error{
		Code:        code,
		Description: description,
	}
}

// NewErrorf returns a formatted *message.Error
func NewErrorf(code int, format string, values ...interface{}) *Error {
	return &Error{
		Code:        code,
		Description: fmt.Sprintf(format, values...),
	}
}

// NewInvalidActionError an error with the code -1 for an invalid action.
func NewInvalidActionError(action DataAction) *Error {
	return &Error{
		Code:        -1,
		Description: fmt.Sprintf("invalid action: %s", action),
	}
}

// MarshalJSON marshals an Answer message
func (a Answer) MarshalJSON() ([]byte, error) {
	type internal struct {
		JSONRpc string  `json:"jsonrpc"`
		Result  *Result `json:"result,omitempty"`
		Error   *Error  `json:"error,omitempty"`
		ID      *int    `json:"id"`
	}

	tmp := internal{
		JSONRpc: "2.0",
		ID:      a.ID,
		Result:  a.Result,
		Error:   a.Error,
	}

	return json.Marshal(tmp)
}
