// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import (
	message "popstellar/internal/handler/message/mmessage"

	mock "github.com/stretchr/testify/mock"
)

// Repository is an autogenerated mock type for the Repository type
type Repository struct {
	mock.Mock
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