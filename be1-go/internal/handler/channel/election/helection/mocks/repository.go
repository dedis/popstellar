// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import (
	mock "github.com/stretchr/testify/mock"
	kyber "go.dedis.ch/kyber/v3"
	types "popstellar/internal/handler/channel/election/telection"
	message "popstellar/internal/handler/message/mmessage"
)

// Repository is an autogenerated mock type for the Repository type
type Repository struct {
	mock.Mock
}

// GetElectionAttendees provides a mock function with given fields: electionID
func (_m *Repository) GetElectionAttendees(electionID string) (map[string]struct{}, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for GetElectionAttendees")
	}

	var r0 map[string]struct{}
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (map[string]struct{}, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) map[string]struct{}); ok {
		r0 = rf(electionID)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string]struct{})
		}
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetElectionCreationTime provides a mock function with given fields: electionID
func (_m *Repository) GetElectionCreationTime(electionID string) (int64, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for GetElectionCreationTime")
	}

	var r0 int64
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (int64, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) int64); ok {
		r0 = rf(electionID)
	} else {
		r0 = ret.Get(0).(int64)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetElectionQuestions provides a mock function with given fields: electionID
func (_m *Repository) GetElectionQuestions(electionID string) (map[string]types.Question, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for GetElectionQuestions")
	}

	var r0 map[string]types.Question
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (map[string]types.Question, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) map[string]types.Question); ok {
		r0 = rf(electionID)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string]types.Question)
		}
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetElectionQuestionsWithValidVotes provides a mock function with given fields: electionID
func (_m *Repository) GetElectionQuestionsWithValidVotes(electionID string) (map[string]types.Question, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for GetElectionQuestionsWithValidVotes")
	}

	var r0 map[string]types.Question
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (map[string]types.Question, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) map[string]types.Question); ok {
		r0 = rf(electionID)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string]types.Question)
		}
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetElectionSecretKey provides a mock function with given fields: electionID
func (_m *Repository) GetElectionSecretKey(electionID string) (kyber.Scalar, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for GetElectionSecretKey")
	}

	var r0 kyber.Scalar
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (kyber.Scalar, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) kyber.Scalar); ok {
		r0 = rf(electionID)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(kyber.Scalar)
		}
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetElectionType provides a mock function with given fields: electionID
func (_m *Repository) GetElectionType(electionID string) (string, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for GetElectionType")
	}

	var r0 string
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (string, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) string); ok {
		r0 = rf(electionID)
	} else {
		r0 = ret.Get(0).(string)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetLAOOrganizerPubKey provides a mock function with given fields: electionID
func (_m *Repository) GetLAOOrganizerPubKey(electionID string) (kyber.Point, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for GetLAOOrganizerPubKey")
	}

	var r0 kyber.Point
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (kyber.Point, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) kyber.Point); ok {
		r0 = rf(electionID)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(kyber.Point)
		}
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// IsElectionEnded provides a mock function with given fields: electionID
func (_m *Repository) IsElectionEnded(electionID string) (bool, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for IsElectionEnded")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (bool, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) bool); ok {
		r0 = rf(electionID)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// IsElectionStarted provides a mock function with given fields: electionID
func (_m *Repository) IsElectionStarted(electionID string) (bool, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for IsElectionStarted")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (bool, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) bool); ok {
		r0 = rf(electionID)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// IsElectionStartedOrEnded provides a mock function with given fields: electionID
func (_m *Repository) IsElectionStartedOrEnded(electionID string) (bool, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for IsElectionStartedOrEnded")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (bool, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) bool); ok {
		r0 = rf(electionID)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// StoreElectionEndWithResult provides a mock function with given fields: channelID, msg, electionResultMsg
func (_m *Repository) StoreElectionEndWithResult(channelID string, msg message.Message, electionResultMsg message.Message) error {
	ret := _m.Called(channelID, msg, electionResultMsg)

	if len(ret) == 0 {
		panic("no return value specified for StoreElectionEndWithResult")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, message.Message, message.Message) error); ok {
		r0 = rf(channelID, msg, electionResultMsg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreMessageAndData provides a mock function with given fields: channelID, msg
func (_m *Repository) StoreMessageAndData(channelID string, msg message.Message) error {
	ret := _m.Called(channelID, msg)

	if len(ret) == 0 {
		panic("no return value specified for StoreMessageAndData")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, message.Message) error); ok {
		r0 = rf(channelID, msg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// NewRepository creates a new instance of Repository. It also registers a testing interface on the mock and a cleanup function to assert the mocks expectations.
// The first argument is typically a *testing.T value.
func NewRepository(t interface {
	mock.TestingT
	Cleanup(func())
}) *Repository {
	mock := &Repository{}
	mock.Mock.Test(t)

	t.Cleanup(func() { mock.AssertExpectations(t) })

	return mock
}
