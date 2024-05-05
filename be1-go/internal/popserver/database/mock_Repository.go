// Code generated by mockery v2.42.1. DO NOT EDIT.

package database

import (
	message "popstellar/message/query/method/message"

	mock "github.com/stretchr/testify/mock"
	kyber "go.dedis.ch/kyber/v3"

	types "popstellar/internal/popserver/types"
)

// MockRepository is an autogenerated mock type for the Repository type
type MockRepository struct {
	mock.Mock
}

// CheckPrevID provides a mock function with given fields: channel, nextID, expectedState
func (_m *MockRepository) CheckPrevID(channel string, nextID string, expectedState string) (bool, error) {
	ret := _m.Called(channel, nextID, expectedState)

	if len(ret) == 0 {
		panic("no return value specified for CheckPrevID")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string, string, string) (bool, error)); ok {
		return rf(channel, nextID, expectedState)
	}
	if rf, ok := ret.Get(0).(func(string, string, string) bool); ok {
		r0 = rf(channel, nextID, expectedState)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string, string, string) error); ok {
		r1 = rf(channel, nextID, expectedState)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetAllMessagesFromChannel provides a mock function with given fields: channelID
func (_m *MockRepository) GetAllMessagesFromChannel(channelID string) ([]message.Message, error) {
	ret := _m.Called(channelID)

	if len(ret) == 0 {
		panic("no return value specified for GetAllMessagesFromChannel")
	}

	var r0 []message.Message
	var r1 error
	if rf, ok := ret.Get(0).(func(string) ([]message.Message, error)); ok {
		return rf(channelID)
	}
	if rf, ok := ret.Get(0).(func(string) []message.Message); ok {
		r0 = rf(channelID)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).([]message.Message)
		}
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(channelID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetChannelType provides a mock function with given fields: channel
func (_m *MockRepository) GetChannelType(channel string) (string, error) {
	ret := _m.Called(channel)

	if len(ret) == 0 {
		panic("no return value specified for GetChannelType")
	}

	var r0 string
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (string, error)); ok {
		return rf(channel)
	}
	if rf, ok := ret.Get(0).(func(string) string); ok {
		r0 = rf(channel)
	} else {
		r0 = ret.Get(0).(string)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(channel)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetElectionAttendees provides a mock function with given fields: electionID
func (_m *MockRepository) GetElectionAttendees(electionID string) (map[string]struct{}, error) {
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
func (_m *MockRepository) GetElectionCreationTime(electionID string) (int64, error) {
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
func (_m *MockRepository) GetElectionQuestions(electionID string) (map[string]types.Question, error) {
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
func (_m *MockRepository) GetElectionQuestionsWithValidVotes(electionID string) (map[string]types.Question, error) {
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
func (_m *MockRepository) GetElectionSecretKey(electionID string) (kyber.Scalar, error) {
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
func (_m *MockRepository) GetElectionType(electionID string) (string, error) {
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
func (_m *MockRepository) GetLAOOrganizerPubKey(electionID string) (kyber.Point, error) {
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

// GetLaoWitnesses provides a mock function with given fields: laoID
func (_m *MockRepository) GetLaoWitnesses(laoID string) (map[string]struct{}, error) {
	ret := _m.Called(laoID)

	if len(ret) == 0 {
		panic("no return value specified for GetLaoWitnesses")
	}

	var r0 map[string]struct{}
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (map[string]struct{}, error)); ok {
		return rf(laoID)
	}
	if rf, ok := ret.Get(0).(func(string) map[string]struct{}); ok {
		r0 = rf(laoID)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string]struct{})
		}
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(laoID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetMessageByID provides a mock function with given fields: ID
func (_m *MockRepository) GetMessageByID(ID string) (message.Message, error) {
	ret := _m.Called(ID)

	if len(ret) == 0 {
		panic("no return value specified for GetMessageByID")
	}

	var r0 message.Message
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (message.Message, error)); ok {
		return rf(ID)
	}
	if rf, ok := ret.Get(0).(func(string) message.Message); ok {
		r0 = rf(ID)
	} else {
		r0 = ret.Get(0).(message.Message)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(ID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetMessagesByID provides a mock function with given fields: IDs
func (_m *MockRepository) GetMessagesByID(IDs []string) (map[string]message.Message, error) {
	ret := _m.Called(IDs)

	if len(ret) == 0 {
		panic("no return value specified for GetMessagesByID")
	}

	var r0 map[string]message.Message
	var r1 error
	if rf, ok := ret.Get(0).(func([]string) (map[string]message.Message, error)); ok {
		return rf(IDs)
	}
	if rf, ok := ret.Get(0).(func([]string) map[string]message.Message); ok {
		r0 = rf(IDs)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string]message.Message)
		}
	}

	if rf, ok := ret.Get(1).(func([]string) error); ok {
		r1 = rf(IDs)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetOrganizerPubKey provides a mock function with given fields: laoID
func (_m *MockRepository) GetOrganizerPubKey(laoID string) (kyber.Point, error) {
	ret := _m.Called(laoID)

	if len(ret) == 0 {
		panic("no return value specified for GetOrganizerPubKey")
	}

	var r0 kyber.Point
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (kyber.Point, error)); ok {
		return rf(laoID)
	}
	if rf, ok := ret.Get(0).(func(string) kyber.Point); ok {
		r0 = rf(laoID)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(kyber.Point)
		}
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(laoID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetParamsForGetMessageByID provides a mock function with given fields: params
func (_m *MockRepository) GetParamsForGetMessageByID(params map[string][]string) (map[string][]string, error) {
	ret := _m.Called(params)

	if len(ret) == 0 {
		panic("no return value specified for GetParamsForGetMessageByID")
	}

	var r0 map[string][]string
	var r1 error
	if rf, ok := ret.Get(0).(func(map[string][]string) (map[string][]string, error)); ok {
		return rf(params)
	}
	if rf, ok := ret.Get(0).(func(map[string][]string) map[string][]string); ok {
		r0 = rf(params)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string][]string)
		}
	}

	if rf, ok := ret.Get(1).(func(map[string][]string) error); ok {
		r1 = rf(params)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetResultForGetMessagesByID provides a mock function with given fields: params
func (_m *MockRepository) GetResultForGetMessagesByID(params map[string][]string) (map[string][]message.Message, error) {
	ret := _m.Called(params)

	if len(ret) == 0 {
		panic("no return value specified for GetResultForGetMessagesByID")
	}

	var r0 map[string][]message.Message
	var r1 error
	if rf, ok := ret.Get(0).(func(map[string][]string) (map[string][]message.Message, error)); ok {
		return rf(params)
	}
	if rf, ok := ret.Get(0).(func(map[string][]string) map[string][]message.Message); ok {
		r0 = rf(params)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string][]message.Message)
		}
	}

	if rf, ok := ret.Get(1).(func(map[string][]string) error); ok {
		r1 = rf(params)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetRollCallState provides a mock function with given fields: channel
func (_m *MockRepository) GetRollCallState(channel string) (string, error) {
	ret := _m.Called(channel)

	if len(ret) == 0 {
		panic("no return value specified for GetRollCallState")
	}

	var r0 string
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (string, error)); ok {
		return rf(channel)
	}
	if rf, ok := ret.Get(0).(func(string) string); ok {
		r0 = rf(channel)
	} else {
		r0 = ret.Get(0).(string)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(channel)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// HasChannel provides a mock function with given fields: channel
func (_m *MockRepository) HasChannel(channel string) (bool, error) {
	ret := _m.Called(channel)

	if len(ret) == 0 {
		panic("no return value specified for HasChannel")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (bool, error)); ok {
		return rf(channel)
	}
	if rf, ok := ret.Get(0).(func(string) bool); ok {
		r0 = rf(channel)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(channel)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// HasMessage provides a mock function with given fields: messageID
func (_m *MockRepository) HasMessage(messageID string) (bool, error) {
	ret := _m.Called(messageID)

	if len(ret) == 0 {
		panic("no return value specified for HasMessage")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (bool, error)); ok {
		return rf(messageID)
	}
	if rf, ok := ret.Get(0).(func(string) bool); ok {
		r0 = rf(messageID)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(messageID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// IsElectionStarted provides a mock function with given fields: electionID
func (_m *MockRepository) IsElectionStarted(electionID string) (bool, error) {
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

// IsElectionStartedOrTerminated provides a mock function with given fields: electionID
func (_m *MockRepository) IsElectionStartedOrEnded(electionID string) (bool, error) {
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

// IsElectionTerminated provides a mock function with given fields: electionID
func (_m *MockRepository) IsElectionEnded(electionID string) (bool, error) {
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

// StoreChannelsAndMessage provides a mock function with given fields: channels, laoID, msg
func (_m *MockRepository) StoreChannelsAndMessage(channels []string, laoID string, msg message.Message) error {
	ret := _m.Called(channels, laoID, msg)

	if len(ret) == 0 {
		panic("no return value specified for StoreChannelsAndMessage")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func([]string, string, message.Message) error); ok {
		r0 = rf(channels, laoID, msg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreChannelsAndMessageWithLaoGreet provides a mock function with given fields: channels, laoID, organizerPubBuf, msg, laoGreetMsg
func (_m *MockRepository) StoreChannelsAndMessageWithLaoGreet(channels map[string]string, laoID string, organizerPubBuf []byte, msg message.Message, laoGreetMsg message.Message) error {
	ret := _m.Called(channels, laoID, organizerPubBuf, msg, laoGreetMsg)

	if len(ret) == 0 {
		panic("no return value specified for StoreChannelsAndMessageWithLaoGreet")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(map[string]string, string, []byte, message.Message, message.Message) error); ok {
		r0 = rf(channels, laoID, organizerPubBuf, msg, laoGreetMsg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreMessage provides a mock function with given fields: channelID, msg
func (_m *MockRepository) StoreMessage(channelID string, msg message.Message) error {
	ret := _m.Called(channelID, msg)

	if len(ret) == 0 {
		panic("no return value specified for StoreMessage")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, message.Message) error); ok {
		r0 = rf(channelID, msg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreMessageAndData provides a mock function with given fields: channelID, msg
func (_m *MockRepository) StoreMessageAndData(channelID string, msg message.Message) error {
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

// StoreMessageAndElectionResult provides a mock function with given fields: channelID, msg, electionResultMsg
func (_m *MockRepository) StoreMessageAndElectionResult(channelID string, msg message.Message, electionResultMsg message.Message) error {
	ret := _m.Called(channelID, msg, electionResultMsg)

	if len(ret) == 0 {
		panic("no return value specified for StoreMessageAndElectionResult")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, message.Message, message.Message) error); ok {
		r0 = rf(channelID, msg, electionResultMsg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreMessageWithElectionKey provides a mock function with given fields: laoID, electionID, electionPubKey, electionSecretKey, msg, electionKeyMsg
func (_m *MockRepository) StoreMessageWithElectionKey(laoID string, electionID string, electionPubKey kyber.Point, electionSecretKey kyber.Scalar, msg message.Message, electionKeyMsg message.Message) error {
	ret := _m.Called(laoID, electionID, electionPubKey, electionSecretKey, msg, electionKeyMsg)

	if len(ret) == 0 {
		panic("no return value specified for StoreMessageWithElectionKey")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, string, kyber.Point, kyber.Scalar, message.Message, message.Message) error); ok {
		r0 = rf(laoID, electionID, electionPubKey, electionSecretKey, msg, electionKeyMsg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// NewMockRepository creates a new instance of MockRepository. It also registers a testing interface on the mock and a cleanup function to assert the mocks expectations.
// The first argument is typically a *testing.T value.
func NewMockRepository(t interface {
	mock.TestingT
	Cleanup(func())
}) *MockRepository {
	mock := &MockRepository{}
	mock.Mock.Test(t)

	t.Cleanup(func() { mock.AssertExpectations(t) })

	return mock
}
