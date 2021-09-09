package message2

import (
	"encoding/json"
	"student20_pop/message2/answer"
	"student20_pop/message2/query"

	"golang.org/x/xerrors"
)

const (
	// RPCTypeQuery ...
	RPCTypeQuery RPCType = iota

	// RPCTypeAnswer ....
	RPCTypeAnswer

	// RPCUnknown ...
	RPCUnknown
)

// RPCType ...
type RPCType int

// JSONRPC ...
type JSONRPC struct {
	JSONRPC string

	query.Query
	answer.Answer
}

func (j *JSONRPC) UnmarshalJSON(buf []byte) error {
	var objmap map[string]json.RawMessage

	err := json.Unmarshal(buf, &objmap)
	if err != nil {
		return xerrors.Errorf("failed to get objmap: %v", err)
	}

	err = json.Unmarshal(objmap["jsonrpc"], &j.JSONRPC)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal JSONRPC: %v", err)
	}

	if objmap["method"] != nil {
		var q query.Query

		err = json.Unmarshal(buf, &q)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal query: %v", err)
		}

		j.Query = q
	} else {
		var a answer.Answer

		err = json.Unmarshal(buf, &a)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal answer: %v", err)
		}

		j.Answer = a
	}

	return nil
}

// Type ...
func (j JSONRPC) Type() RPCType {
	switch {
	case j.Result != nil || j.Error != nil:
		return RPCTypeAnswer
	case j.Method != "":
		return RPCTypeQuery
	}

	return RPCUnknown
}
