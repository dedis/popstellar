package message

import (
	"encoding/json"
	"log"

	"golang.org/x/xerrors"
)

// GenericMessage represents a message that may be sent as a payload on websocket.
// It may either be a `Query` or an `Answer`.
type GenericMessage struct {
	Query  *Query
	Answer *Answer
}

// UnmarshalJSON implements custom unmarshaling logic for a GenericMessage.
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

type IdStruct struct {
	ID *int `json:"id"`
}

func (m *GenericMessage) GetID(data []byte) (int, bool) {
	IdStruct := &IdStruct{}
	json.Unmarshal(data, IdStruct)
	log.Printf("%d", *IdStruct.ID)
	if IdStruct.ID != nil {
		return *IdStruct.ID, true
	}
	return -1, false
}
