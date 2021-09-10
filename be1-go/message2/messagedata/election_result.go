package messagedata

// ElectionResult ...
type ElectionResult struct {
	Object    string                   `json:"object"`
	Action    string                   `json:"action"`
	Questions []ElectionResultQuestion `json:"questions"`
}

// ElectionResultQuestion ...
type ElectionResultQuestion struct {
	ID     string                         `json:"id"`
	Result []ElectionResultQuestionResult `json:"result"`
}

// ElectionResultQuestionResult ... sorry for the long name ...
type ElectionResultQuestionResult struct {
	BallotOption string `json:"ballot_option"`
	Count        int    `json:"count"`
}
