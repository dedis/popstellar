package messagedata

// ElectionSetup defines a message data
type ElectionSetup struct {
	Object  string `json:"object"`
	Action  string `json:"action"`
	ID      string `json:"id"`
	Lao     string `json:"lao"`
	Name    string `json:"name"`
	Version string `json:"version"`

	// CreatedAt is a Unix timestamp
	CreatedAt int64 `json:"created_at"`

	// StartTime is a Unix timestamp
	StartTime int64 `json:"start_time"`

	// EndTime is a Unix timestamp
	EndTime int64 `json:"end_time"`

	Questions []ElectionSetupQuestion `json:"questions"`
}

const (
	// OpenBallot is a type of election
	OpenBallot = "OPEN_BALLOT"
	// SecretBallot is a type of election
	SecretBallot = "SECRET_BALLOT"
)

// ElectionSetupQuestion defines a question of an election setup
type ElectionSetupQuestion struct {
	ID            string   `json:"id"`
	Question      string   `json:"question"`
	VotingMethod  string   `json:"voting_method"`
	BallotOptions []string `json:"ballot_options"`
	WriteIn       bool     `json:"write_in"`
}

// GetObject implements MessageData
func (ElectionSetup) GetObject() string {
	return ElectionObject
}

// GetAction implements MessageData
func (ElectionSetup) GetAction() string {
	return ElectionActionSetup
}

// NewEmpty implements MessageData
func (ElectionSetup) NewEmpty() MessageData {
	return &ElectionSetup{}
}
