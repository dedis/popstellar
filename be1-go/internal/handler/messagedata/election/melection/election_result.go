package melection

import (
	"popstellar/internal/handler/messagedata"
)

// ElectionResult defines a message data
type ElectionResult struct {
	Object    string                   `json:"object"`
	Action    string                   `json:"action"`
	Questions []ElectionResultQuestion `json:"questions"`
}

// ElectionResultQuestion defines a question of an election result
type ElectionResultQuestion struct {
	ID     string                         `json:"id"`
	Result []ElectionResultQuestionResult `json:"result"`
}

// ElectionResultQuestionResult defines a result of questions
type ElectionResultQuestionResult struct {
	BallotOption string `json:"ballot_option"`
	Count        int    `json:"count"`
}

// GetObject implements MessageData
func (ElectionResult) GetObject() string {
	return messagedata.ElectionObject
}

// GetAction implements MessageData
func (ElectionResult) GetAction() string {
	return messagedata.ElectionActionResult
}

// NewEmpty implements MessageData
func (ElectionResult) NewEmpty() messagedata.MessageData {
	return &ElectionResult{}
}
