// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import (
	messagedata "popstellar/message/messagedata"
	message "popstellar/message/query/method/message"

	kyber "go.dedis.ch/kyber/v3"

	mock "github.com/stretchr/testify/mock"
)

// Repository is an autogenerated mock type for the Repository type
type Repository struct {
	mock.Mock
}

// AddNewBlackList provides a mock function with given fields: msgs
func (_m *Repository) AddNewBlackList(msgs map[string]map[string]message.Message) error {
	ret := _m.Called(msgs)

	if len(ret) == 0 {
		panic("no return value specified for AddNewBlackList")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(map[string]map[string]message.Message) error); ok {
		r0 = rf(msgs)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// ChannelExists provides a mock function with given fields: laoChannelPath
func (_m *Repository) ChannelExists(laoChannelPath string) (bool, error) {
	ret := _m.Called(laoChannelPath)

	if len(ret) == 0 {
		panic("no return value specified for ChannelExists")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (bool, error)); ok {
		return rf(laoChannelPath)
	}
	if rf, ok := ret.Get(0).(func(string) bool); ok {
		r0 = rf(laoChannelPath)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(laoChannelPath)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// CheckPrevID provides a mock function with given fields: channel, nextID
func (_m *Repository) CheckPrevID(channel string, nextID string) (bool, error) {
	ret := _m.Called(channel, nextID)

	if len(ret) == 0 {
		panic("no return value specified for CheckPrevID")
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
func (_m *Repository) GetAllMessagesFromChannel(channelID string) ([]message.Message, error) {
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
func (_m *Repository) GetChannelType(channel string) (string, error) {
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

// GetClientServerAddress provides a mock function with given fields:
func (_m *Repository) GetClientServerAddress() (string, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetClientServerAddress")
	}

	var r0 string
	var r1 error
	if rf, ok := ret.Get(0).(func() (string, error)); ok {
		return rf()
	}
	if rf, ok := ret.Get(0).(func() string); ok {
		r0 = rf()
	} else {
		r0 = ret.Get(0).(string)
	}

	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetIDsTable provides a mock function with given fields:
func (_m *Repository) GetIDsTable() (map[string][]string, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetIDsTable")
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

// GetLaoWitnesses provides a mock function with given fields: laoPath
func (_m *Repository) GetLaoWitnesses(laoPath string) (map[string]struct{}, error) {
	ret := _m.Called(laoPath)

	if len(ret) == 0 {
		panic("no return value specified for GetLaoWitnesses")
	}

	var r0 map[string]struct{}
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (map[string]struct{}, error)); ok {
		return rf(laoPath)
	}
	if rf, ok := ret.Get(0).(func(string) map[string]struct{}); ok {
		r0 = rf(laoPath)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string]struct{})
		}
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(laoPath)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetLastVote provides a mock function with given fields: sender, electionID
func (_m *Repository) GetLastVote(sender string, electionID string) (messagedata.VoteCastVote, error) {
	ret := _m.Called(sender, electionID)

	if len(ret) == 0 {
		panic("no return value specified for GetLastVote")
	}

	var r0 messagedata.VoteCastVote
	var r1 error
	if rf, ok := ret.Get(0).(func(string, string) (messagedata.VoteCastVote, error)); ok {
		return rf(sender, electionID)
	}
	if rf, ok := ret.Get(0).(func(string, string) messagedata.VoteCastVote); ok {
		r0 = rf(sender, electionID)
	} else {
		r0 = ret.Get(0).(messagedata.VoteCastVote)
	}

	if rf, ok := ret.Get(1).(func(string, string) error); ok {
		r1 = rf(sender, electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetMessageByID provides a mock function with given fields: ID
func (_m *Repository) GetMessageByID(ID string) (message.Message, error) {
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
func (_m *Repository) GetMessagesByID(IDs []string) (map[string]message.Message, error) {
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

// GetOwnerPubKey provides a mock function with given fields:
func (_m *Repository) GetOwnerPubKey() (kyber.Point, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetOwnerPubKey")
	}

	var r0 kyber.Point
	var r1 error
	if rf, ok := ret.Get(0).(func() (kyber.Point, error)); ok {
		return rf()
	}
	if rf, ok := ret.Get(0).(func() kyber.Point); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(kyber.Point)
		}
	}

	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetParamsForGetMessageByID provides a mock function with given fields: params
func (_m *Repository) GetParamsForGetMessageByID(params map[string][]string) (map[string][]string, error) {
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

// GetResult provides a mock function with given fields: electionID
func (_m *Repository) GetResult(electionID string) (messagedata.ElectionResult, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for GetResult")
	}

	var r0 messagedata.ElectionResult
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (messagedata.ElectionResult, error)); ok {
		return rf(electionID)
	}
	if rf, ok := ret.Get(0).(func(string) messagedata.ElectionResult); ok {
		r0 = rf(electionID)
	} else {
		r0 = ret.Get(0).(messagedata.ElectionResult)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(electionID)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetResultForGetMessagesByID provides a mock function with given fields: params
func (_m *Repository) GetResultForGetMessagesByID(params map[string][]string) (map[string][]message.Message, error) {
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
func (_m *Repository) GetRollCallState(channel string) (string, error) {
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

// GetServerPubKey provides a mock function with given fields:
func (_m *Repository) GetServerPubKey() ([]byte, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetServerPubKey")
	}

	var r0 []byte
	var r1 error
	if rf, ok := ret.Get(0).(func() ([]byte, error)); ok {
		return rf()
	}
	if rf, ok := ret.Get(0).(func() []byte); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).([]byte)
		}
	}

	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// GetServerSecretKey provides a mock function with given fields:
func (_m *Repository) GetServerSecretKey() ([]byte, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetServerSecretKey")
	}

	var r0 []byte
	var r1 error
	if rf, ok := ret.Get(0).(func() ([]byte, error)); ok {
		return rf()
	}
	if rf, ok := ret.Get(0).(func() []byte); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).([]byte)
		}
	}

	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// HasChannel provides a mock function with given fields: laoChannelPath
func (_m *Repository) HasChannel(laoChannelPath string) (bool, error) {
	ret := _m.Called(laoChannelPath)

	if len(ret) == 0 {
		panic("no return value specified for HasChannel")
	}

	var r0 bool
	var r1 error
	if rf, ok := ret.Get(0).(func(string) (bool, error)); ok {
		return rf(laoChannelPath)
	}
	if rf, ok := ret.Get(0).(func(string) bool); ok {
		r0 = rf(laoChannelPath)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(laoChannelPath)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// HasMessage provides a mock function with given fields: messageID
func (_m *Repository) HasMessage(messageID string) (bool, error) {
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

// IsElectionTerminated provides a mock function with given fields: electionID
func (_m *Repository) IsElectionTerminated(electionID string) (bool, error) {
	ret := _m.Called(electionID)

	if len(ret) == 0 {
		panic("no return value specified for IsElectionTerminated")
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

// StoreChannel provides a mock function with given fields: channel, organizerPubKey
func (_m *Repository) StoreChannel(channel string, organizerPubKey []byte) error {
	ret := _m.Called(channel, organizerPubKey)

	if len(ret) == 0 {
		panic("no return value specified for StoreChannel")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, []byte) error); ok {
		r0 = rf(channel, organizerPubKey)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreChannelsAndMessage provides a mock function with given fields: channels, baseChannel, organizerPubKey, msg
func (_m *Repository) StoreChannelsAndMessage(channels []string, baseChannel string, organizerPubKey []byte, msg message.Message) error {
	ret := _m.Called(channels, baseChannel, organizerPubKey, msg)

	if len(ret) == 0 {
		panic("no return value specified for StoreChannelsAndMessage")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func([]string, string, []byte, message.Message) error); ok {
		r0 = rf(channels, baseChannel, organizerPubKey, msg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreChannelsAndMessageWithLaoGreet provides a mock function with given fields: channels, baseChannel, channelRelation, messageIDRelation, organizerPubBuf, msg, laoGreetMsg
func (_m *Repository) StoreChannelsAndMessageWithLaoGreet(channels []string, baseChannel string, channelRelation string, messageIDRelation string, organizerPubBuf []byte, msg message.Message, laoGreetMsg message.Message) error {
	ret := _m.Called(channels, baseChannel, channelRelation, messageIDRelation, organizerPubBuf, msg, laoGreetMsg)

	if len(ret) == 0 {
		panic("no return value specified for StoreChannelsAndMessageWithLaoGreet")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func([]string, string, string, string, []byte, message.Message, message.Message) error); ok {
		r0 = rf(channels, baseChannel, channelRelation, messageIDRelation, organizerPubBuf, msg, laoGreetMsg)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// StoreMessage provides a mock function with given fields: channelID, msg
func (_m *Repository) StoreMessage(channelID string, msg message.Message) error {
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

// StoreMessageWithElectionKey provides a mock function with given fields: baseChannel, channelRelation, messageIDRelation, electionPubKey, electionSecretKey, msg, electionKey
func (_m *Repository) StoreMessageWithElectionKey(baseChannel string, channelRelation string, messageIDRelation string, electionPubKey kyber.Point, electionSecretKey kyber.Scalar, msg message.Message, electionKey message.Message) error {
	ret := _m.Called(baseChannel, channelRelation, messageIDRelation, electionPubKey, electionSecretKey, msg, electionKey)

	if len(ret) == 0 {
		panic("no return value specified for StoreMessageWithElectionKey")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, string, string, kyber.Point, kyber.Scalar, message.Message, message.Message) error); ok {
		r0 = rf(baseChannel, channelRelation, messageIDRelation, electionPubKey, electionSecretKey, msg, electionKey)
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
