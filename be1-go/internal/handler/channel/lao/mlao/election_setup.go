package mlao

import (
	"encoding/base64"
	"popstellar/internal/errors"
	"popstellar/internal/handler/channel"
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

func (message ElectionSetup) Verify(laoID string) error {
	_, err := base64.URLEncoding.DecodeString(message.Lao)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode lao: %v", err)
	}

	if message.Lao != laoID {
		return errors.NewInvalidMessageFieldError("lao id is %s, should be %s", message.Lao, laoID)
	}

	_, err = base64.URLEncoding.DecodeString(message.ID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode election id: %v", err)
	}

	// verify election setup message id
	expectedID := channel.Hash(
		ElectionFlag,
		laoID,
		strconv.Itoa(int(message.CreatedAt)),
		message.Name,
	)
	if message.ID != expectedID {
		return errors.NewInvalidMessageFieldError("election id is %s, should be %s", message.ID, expectedID)
	}
	if len(message.Name) == 0 {
		return errors.NewInvalidMessageFieldError("election name is empty")
	}
	if message.Version != OpenBallot && message.Version != SecretBallot {
		return errors.NewInvalidMessageFieldError("election version is %s, should be %s or %s", message.Version,
			OpenBallot, SecretBallot)
	}
	if message.CreatedAt < 0 {
		return errors.NewInvalidMessageFieldError("election created at is %d, should be minimum 0", message.CreatedAt)
	}
	if message.StartTime < message.CreatedAt {
		return errors.NewInvalidMessageFieldError("election start should be greater that creation time")
	}
	if message.EndTime < message.StartTime {
		return errors.NewInvalidMessageFieldError("election end should be greater that start time")
	}
	if len(message.Questions) == 0 {
		return errors.NewInvalidMessageFieldError("election contains no questions")
	}
	return nil
}

// GetObject implements MessageData
func (ElectionSetup) GetObject() string {
	return channel.ElectionObject
}

// GetAction implements MessageData
func (ElectionSetup) GetAction() string {
	return channel.ElectionActionSetup
}

// NewEmpty implements MessageData
func (ElectionSetup) NewEmpty() channel.MessageData {
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

func (q ElectionSetupQuestion) Verify(electionSetupID string) error {
	_, err := base64.URLEncoding.DecodeString(q.ID)
	if err != nil {
		return errors.NewInvalidMessageFieldError("failed to decode Question id: %v", err)
	}

	expectedID := channel.Hash(
		questionFlag,
		electionSetupID,
		q.Question,
	)

	if q.ID != expectedID {
		return errors.NewInvalidMessageFieldError("Question id is %s, should be %s", q.ID, expectedID)
	}

	if len(q.Question) == 0 {
		return errors.NewInvalidMessageFieldError("Question is empty")
	}

	if q.VotingMethod != PluralityMethod && q.VotingMethod != ApprovalMethod {
		return errors.NewInvalidMessageFieldError("Question voting method is %s, should be %s or %s",
			q.VotingMethod, PluralityMethod, ApprovalMethod)
	}

	return nil
}
