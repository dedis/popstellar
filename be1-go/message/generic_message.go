package message

import (
	"encoding/json"

	"golang.org/x/xerrors"
)

type GenericMessage struct {
	Query  *Query
	Answer *Answer
}

func (m *GenericMessage) UnmarshalJSON(data []byte) error {
	type internal struct {
		Method string `json:"method"`
	}

	tmp := &internal{}
	err := json.Unmarshal(data, tmp)
	if err != nil {
		return xerrors.Errorf("failed to find method in GenericMessage: %s", err)
	}

	if tmp.Method == "" {
		answer := &Answer{}

		err = json.Unmarshal(data, answer)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal Answer in GenericMessage: %s", err)
		}

		m.Answer = answer
		return nil
	}

	query := &Query{}

	err = json.Unmarshal(data, query)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal Query in GenericMessage: %s", err)
	}

	m.Query = query
	return nil
}
