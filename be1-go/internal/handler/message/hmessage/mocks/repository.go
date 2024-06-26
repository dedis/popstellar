// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import mock "github.com/stretchr/testify/mock"

// Repository is an autogenerated mock type for the Repository type
type Repository struct {
	mock.Mock
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
