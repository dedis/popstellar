package messagedata

import (
	"encoding/base64"
	"popstellar/message/answer"
	"strconv"
)

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

const ElectionFlag = "Election"

func (message ElectionSetup) Verify(laoID string) *answer.Error {
	var errAnswer *answer.Error
	_, err := base64.URLEncoding.DecodeString(message.Lao)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode lao: %v", err)
		return errAnswer
	}

	if message.Lao != laoID {
		errAnswer = answer.NewInvalidMessageFieldError("lao id is %s, should be %s", message.Lao, laoID)
		return errAnswer
	}

	_, err = base64.URLEncoding.DecodeString(message.ID)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode election id: %v", err)
		return errAnswer
	}

	// verify election setup message id
	expectedID := Hash(
		ElectionFlag,
		laoID,
		strconv.Itoa(int(message.CreatedAt)),
		message.Name,
	)
	if message.ID != expectedID {
		errAnswer = answer.NewInvalidMessageFieldError("election id is %s, should be %s", message.ID, expectedID)
		return errAnswer
	}
	if len(message.Name) == 0 {
		errAnswer = answer.NewInvalidMessageFieldError("election name is empty")
		return errAnswer
	}
	if message.Version != OpenBallot && message.Version != SecretBallot {
		errAnswer = answer.NewInvalidMessageFieldError("election version is %s, should be %s or %s", message.Version, OpenBallot, SecretBallot)
		return errAnswer
	}
	if message.CreatedAt < 0 {
		errAnswer = answer.NewInvalidMessageFieldError("election created at is %d, should be minimum 0", message.CreatedAt)
		return errAnswer
	}
	if message.StartTime < message.CreatedAt {
		errAnswer = answer.NewInvalidMessageFieldError("election start should be greater that creation time")
		return errAnswer
	}
	if message.EndTime < message.StartTime {
		errAnswer = answer.NewInvalidMessageFieldError("election end should be greater that start time")
		return errAnswer
	}
	if len(message.Questions) == 0 {
		errAnswer = answer.NewInvalidMessageFieldError("election contains no questions")
		return errAnswer
	}
	return nil
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

const (
	// OpenBallot is a type of election
	OpenBallot = "OPEN_BALLOT"
	// SecretBallot is a type of election
	SecretBallot    = "SECRET_BALLOT"
	questionFlag    = "Question"
	PluralityMethod = "Plurality"
	ApprovalMethod  = "Approval"
)

// ElectionSetupQuestion defines a question of an election setup
type ElectionSetupQuestion struct {
	ID            string   `json:"id"`
	Question      string   `json:"question"`
	VotingMethod  string   `json:"voting_method"`
	BallotOptions []string `json:"ballot_options"`
	WriteIn       bool     `json:"write_in"`
}

func (q ElectionSetupQuestion) Verify(electionSetupID string) *answer.Error {
	var errAnswer *answer.Error
	_, err := base64.URLEncoding.DecodeString(q.ID)
	if err != nil {
		errAnswer = answer.NewInvalidMessageFieldError("failed to decode Question id: %v", err)
		return errAnswer
	}
	expectedID := Hash(
		questionFlag,
		electionSetupID,
		q.Question,
	)
	if q.ID != expectedID {
		errAnswer = answer.NewInvalidMessageFieldError("Question id is %s, should be %s", q.ID, expectedID)
		return errAnswer
	}
	if len(q.Question) == 0 {
		errAnswer = answer.NewInvalidMessageFieldError("Question is empty")
		return errAnswer
	}
	if q.VotingMethod != PluralityMethod && q.VotingMethod != ApprovalMethod {
		errAnswer = answer.NewInvalidMessageFieldError("Question voting method is %s, should be %s or %s",
			q.VotingMethod, PluralityMethod, ApprovalMethod)
		return errAnswer
	}
	return nil
}
