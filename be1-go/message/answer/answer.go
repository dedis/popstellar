package answer

import (
	"encoding/json"
	"golang.org/x/xerrors"
	message "popstellar/message"
)

// Answer defines the JSON RPC answer message
type Answer struct {
	message.JSONRPCBase

	ID     *int    `json:"id"`
	Result *Result `json:"result,omitempty"`
	Error  *Error  `json:"error,omitempty"`
}

// Result can be either a 0 int, a slice of messages or an array of MessageIdsByChannelId objects
type Result struct {
	isEmpty           bool
	data              []json.RawMessage
	MessagesByChannel map[string][]json.RawMessage
}

// UnmarshalJSON implements json.Unmarshaler
func (r *Result) UnmarshalJSON(buf []byte) error {
	// if the answer return is 0, then we get the ascii value of 0, which equals
	// to 48
	if len(buf) == 1 && buf[0] == 48 {
		r.isEmpty = true
		return nil
	}

	err := json.Unmarshal(buf, &r.data)
	if err != nil {
		err = json.Unmarshal(buf, &r.MessagesByChannel)
	}

	if err != nil {
		return xerrors.Errorf("failed to unmarshal data: %v", err)
	}
	return nil
}

// IsEmpty tells if there are potentially 0 or more messages in the result.
func (r Result) IsEmpty() bool {
	return r.isEmpty
}

// GetData returns the answer data. It can be nil in case the return is empty.
func (r *Result) GetData() []json.RawMessage {
	return r.data
}

// GetMessagesByChannel returns the array of objects mapping a channel with its messages.
func (r *Result) GetMessagesByChannel() map[string][]json.RawMessage {
	return r.MessagesByChannel
}
