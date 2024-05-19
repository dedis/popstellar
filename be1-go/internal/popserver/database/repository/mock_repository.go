// Code generated by mockery v2.42.1. DO NOT EDIT.

package repository

import (
	method "popstellar/message/query/method"
	message "popstellar/message/query/method/message"

	kyber "go.dedis.ch/kyber/v3"

	mock "github.com/stretchr/testify/mock"

	types "popstellar/internal/popserver/types"
)

// MockRepository is an autogenerated mock type for the Repository type
type MockRepository struct {
	mock.Mock
}

// AddMessageToMyRumor provides a mock function with given fields: messageID
func (_m *MockRepository) AddMessageToMyRumor(messageID string) int {
	ret := _m.Called(messageID)

	if len(ret) == 0 {
		panic("no return value specified for AddMessageToMyRumor")
	}

	var r0 int
	if rf, ok := ret.Get(0).(func(string) int); ok {
		r0 = rf(messageID)
	} else {
		r0 = ret.Get(0).(int)
	}

	return r0
}

// CheckPrevCreateOrCloseID provides a mock function with given fields: channel, nextID
func (_m *MockRepository) CheckPrevCreateOrCloseID(channel string, nextID string) (bool, error) {
	ret := _m.Called(channel, nextID)

	if len(ret) == 0 {
		panic("no return value specified for CheckPrevCreateOrCloseID")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string, string) (bool, error)); ok {
		return rf(channel, nextID)
	}
	if rf, ok := ret.Get(0).(func(string, string) bool); ok {
		r0 = rf(channel, nextID)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string, string) error); ok {
		r1 = rf(channel, nextID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// CheckPrevOpenOrReopenID provides a mock function with given fields: channel, nextID
func (_m *MockRepository) CheckPrevOpenOrReopenID(channel string, nextID string) (bool, error) {
	ret := _m.Called(channel, nextID)

	if len(ret) == 0 {
		panic("no return value specified for CheckPrevOpenOrReopenID")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string, string) (bool, error)); ok {
		return rf(channel, nextID)
	}
	if rf, ok := ret.Get(0).(func(string, string) bool); ok {
		r0 = rf(channel, nextID)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string, string) error); ok {
		r1 = rf(channel, nextID)
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

// GetAndIncrementMyRumor provides a mock function with given fields:
func (_m *MockRepository) GetAndIncrementMyRumor() (bool, method.Rumor, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetAndIncrementMyRumor")
	}

	var r0 bool
	var r1 method.Rumor
	var r2 error
	if rf, ok := ret.Get(0).(func() (bool, method.Rumor, error)); ok {
		return rf()
	}
	if rf, ok := ret.Get(0).(func() bool); ok {
		r0 = rf()
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func() method.Rumor); ok {
		r1 = rf()
	} else {
		r1 = ret.Get(1).(method.Rumor)
	}

	if rf, ok := ret.Get(2).(func() error); ok {
		r2 = rf()
	} else {
		r2 = ret.Error(2)
	}

	return r0, r1, r2
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

// GetParamsHeartbeat provides a mock function with given fields:
func (_m *MockRepository) GetParamsHeartbeat() (map[string][]string, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetParamsHeartbeat")
	}

	var r0 map[string][]string
	var r1 error
	if rf, ok := ret.Get(0).(func() (map[string][]string, error)); ok {
		return rf()
	}
	if rf, ok := ret.Get(0).(func() map[string][]string); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string][]string)
		}
	}

	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetReactionSender provides a mock function with given fields: messageID
func (_m *MockRepository) GetReactionSender(messageID string) (string, error) {
	ret := _m.Called(messageID)

	if len(ret) == 0 {
		panic("no return value specified for GetReactionSender")
	}

	var r0 string
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (string, error)); ok {
		return rf(messageID)
	}
	if rf, ok := ret.Get(0).(func(string) string); ok {
		r0 = rf(messageID)
	} else {
		r0 = ret.Get(0).(string)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(messageID)
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

// GetServerKeys provides a mock function with given fields:
func (_m *MockRepository) GetServerKeys() (kyber.Point, kyber.Scalar, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetServerKeys")
	}

	var r0 kyber.Point
	var r1 kyber.Scalar
	var r2 error
	if rf, ok := ret.Get(0).(func() (kyber.Point, kyber.Scalar, error)); ok {
		return rf()
	}
	if rf, ok := ret.Get(0).(func() kyber.Point); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(kyber.Point)
		}
	}

	if rf, ok := ret.Get(1).(func() kyber.Scalar); ok {
		r1 = rf()
	} else {
		if ret.Get(1) != nil {
			r1 = ret.Get(1).(kyber.Scalar)
		}
	}

	if rf, ok := ret.Get(2).(func() error); ok {
		r2 = rf()
	} else {
		r2 = ret.Error(2)
	}

	return r0, r1, r2
}

// GetUnprocessedMessagesByChannel provides a mock function with given fields:
func (_m *MockRepository) GetUnprocessedMessagesByChannel() (map[string][]message.Message, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetUnprocessedMessagesByChannel")
	}

	var r0 map[string][]message.Message
	var r1 error
	if rf, ok := ret.Get(0).(func() (map[string][]message.Message, error)); ok {
		return rf()
	}
	if rf, ok := ret.Get(0).(func() map[string][]message.Message); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string][]message.Message)
		}
	}

	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
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

// HasRumor provides a mock function with given fields: senderID, rumorID
func (_m *MockRepository) HasRumor(senderID string, rumorID int) (bool, error) {
	ret := _m.Called(senderID, rumorID)

	if len(ret) == 0 {
		panic("no return value specified for HasRumor")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string, int) (bool, error)); ok {
		return rf(senderID, rumorID)
	}
	if rf, ok := ret.Get(0).(func(string, int) bool); ok {
		r0 = rf(senderID, rumorID)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string, int) error); ok {
		r1 = rf(senderID, rumorID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// IsAttendee provides a mock function with given fields: laoPath, poptoken
func (_m *MockRepository) IsAttendee(laoPath string, poptoken string) (bool, error) {
	ret := _m.Called(laoPath, poptoken)

	if len(ret) == 0 {
		panic("no return value specified for IsAttendee")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string, string) (bool, error)); ok {
		return rf(laoPath, poptoken)
	}
	if rf, ok := ret.Get(0).(func(string, string) bool); ok {
		r0 = rf(laoPath, poptoken)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string, string) error); ok {
		r1 = rf(laoPath, poptoken)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// IsElectionEnded provides a mock function with given fields: electionID
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

// IsElectionStartedOrEnded provides a mock function with given fields: electionID
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

// StoreChirpMessages provides a mock function with given fields: channel, generalChannel, msg, generalMsg
func (_m *MockRepository) StoreChirpMessages(channel string, generalChannel string, msg message.Message, generalMsg message.Message) error {
	ret := _m.Called(channel, generalChannel, msg, generalMsg)

	if len(ret) == 0 {
		panic("no return value specified for StoreChirpMessages")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, string, message.Message, message.Message) error); ok {
		r0 = rf(channel, generalChannel, msg, generalMsg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreElection provides a mock function with given fields: laoPath, electionPath, electionPubKey, electionSecretKey, msg
func (_m *MockRepository) StoreElection(laoPath string, electionPath string, electionPubKey kyber.Point, electionSecretKey kyber.Scalar, msg message.Message) error {
	ret := _m.Called(laoPath, electionPath, electionPubKey, electionSecretKey, msg)

	if len(ret) == 0 {
		panic("no return value specified for StoreElection")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, string, kyber.Point, kyber.Scalar, message.Message) error); ok {
		r0 = rf(laoPath, electionPath, electionPubKey, electionSecretKey, msg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreElectionEndWithResult provides a mock function with given fields: channelID, msg, electionResultMsg
func (_m *MockRepository) StoreElectionEndWithResult(channelID string, msg message.Message, electionResultMsg message.Message) error {
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

// StoreElectionWithElectionKey provides a mock function with given fields: laoPath, electionPath, electionPubKey, electionSecretKey, msg, electionKeyMsg
func (_m *MockRepository) StoreElectionWithElectionKey(laoPath string, electionPath string, electionPubKey kyber.Point, electionSecretKey kyber.Scalar, msg message.Message, electionKeyMsg message.Message) error {
	ret := _m.Called(laoPath, electionPath, electionPubKey, electionSecretKey, msg, electionKeyMsg)

	if len(ret) == 0 {
		panic("no return value specified for StoreElectionWithElectionKey")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, string, kyber.Point, kyber.Scalar, message.Message, message.Message) error); ok {
		r0 = rf(laoPath, electionPath, electionPubKey, electionSecretKey, msg, electionKeyMsg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreLaoWithLaoGreet provides a mock function with given fields: channels, laoID, organizerPubBuf, msg, laoGreetMsg
func (_m *MockRepository) StoreLaoWithLaoGreet(channels map[string]string, laoID string, organizerPubBuf []byte, msg message.Message, laoGreetMsg message.Message) error {
	ret := _m.Called(channels, laoID, organizerPubBuf, msg, laoGreetMsg)

	if len(ret) == 0 {
		panic("no return value specified for StoreLaoWithLaoGreet")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(map[string]string, string, []byte, message.Message, message.Message) error); ok {
		r0 = rf(channels, laoID, organizerPubBuf, msg, laoGreetMsg)
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

// StoreNewRumor provides a mock function with given fields: senderID, rumorID, processedMessages, unprocessedMessages
func (_m *MockRepository) StoreNewRumor(senderID string, rumorID int, processedMessages []string, unprocessedMessages map[string][]message.Message) error {
	ret := _m.Called(senderID, rumorID, processedMessages, unprocessedMessages)

	if len(ret) == 0 {
		panic("no return value specified for StoreNewRumor")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, int, []string, map[string][]message.Message) error); ok {
		r0 = rf(senderID, rumorID, processedMessages, unprocessedMessages)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreRollCallClose provides a mock function with given fields: channels, laoID, msg
func (_m *MockRepository) StoreRollCallClose(channels []string, laoID string, msg message.Message) error {
	ret := _m.Called(channels, laoID, msg)

	if len(ret) == 0 {
		panic("no return value specified for StoreRollCallClose")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func([]string, string, message.Message) error); ok {
		r0 = rf(channels, laoID, msg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreServerKeys provides a mock function with given fields: electionPubKey, electionSecretKey
func (_m *MockRepository) StoreServerKeys(electionPubKey kyber.Point, electionSecretKey kyber.Scalar) error {
	ret := _m.Called(electionPubKey, electionSecretKey)

	if len(ret) == 0 {
		panic("no return value specified for StoreServerKeys")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(kyber.Point, kyber.Scalar) error); ok {
		r0 = rf(electionPubKey, electionSecretKey)
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
