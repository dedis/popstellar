package messagedata

// ElectionSetup ...
type ElectionSetup struct {
	Object    string
	Action    string
	ID        string
	Lao       string
	Name      string
	Version   string
	CreatedAt int `json:"created_at"`
	StartTime int `json:"start_time"`
	EndTime   int `json:"end_time"`
	Questions []ElectionSetupQuestion
}

// ElectionSetupQuestion ...
type ElectionSetupQuestion struct {
	ID            string
	Question      string
	VotingMethod  string   `json:"voting_method"`
	BallotOptions []string `json:"ballot_options"`
	WriteIn       bool     `json:"write_in"`
}

// QuestionResult ...
type QuestionResult struct {
	BallotOption string `json:"ballot_option"`
	Count        int
}
