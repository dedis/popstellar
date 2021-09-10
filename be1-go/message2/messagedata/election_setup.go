package messagedata

// ElectionSetup ...
type ElectionSetup struct {
	Object    string                  `json:"object"`
	Action    string                  `json:"action"`
	ID        string                  `json:"id"`
	Lao       string                  `json:"lao"`
	Name      string                  `json:"name"`
	Version   string                  `json:"version"`
	CreatedAt int                     `json:"created_at"`
	StartTime int                     `json:"start_time"`
	EndTime   int                     `json:"end_time"`
	Questions []ElectionSetupQuestion `json:"questions"`
}

// ElectionSetupQuestion ...
type ElectionSetupQuestion struct {
	ID            string   `json:"id"`
	Question      string   `json:"question"`
	VotingMethod  string   `json:"voting_method"`
	BallotOptions []string `json:"ballot_options"`
	WriteIn       bool     `json:"write_in"`
}

// QuestionResult ...
type QuestionResult struct {
	BallotOption string `json:"ballot_option"`
	Count        int    `json:"count"`
}
