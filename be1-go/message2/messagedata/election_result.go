package messagedata

// ElectionResult ...
type ElectionResult struct {
	Object    string
	Action    string
	Questions []ElectionResultQuestion
}

// ElectionResultQuestion ...
type ElectionResultQuestion struct {
	ID     string
	Result []ElectionResultQuestionResult
}

// ElectionResultQuestionResult ... sorry for the long name ...
type ElectionResultQuestionResult struct {
	BallotOption string `json:"ballot_option"`
	Count        int
}
