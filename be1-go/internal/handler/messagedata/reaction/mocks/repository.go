// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import (
	message "popstellar/internal/message/query/method/message"

	mock "github.com/stretchr/testify/mock"
)

// Repository is an autogenerated mock type for the Repository type
type Repository struct {
	mock.Mock
}

// GetReactionSender provides a mock function with given fields: messageID
func (_m *Repository) GetReactionSender(messageID string) (string, error) {
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

// IsAttendee provides a mock function with given fields: laoPath, poptoken
func (_m *Repository) IsAttendee(laoPath string, poptoken string) (bool, error) {
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
