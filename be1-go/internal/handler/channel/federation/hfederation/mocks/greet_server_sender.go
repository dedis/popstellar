// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import (
	socket "popstellar/internal/network/socket"

	mock "github.com/stretchr/testify/mock"
)

// GreetServerSender is an autogenerated mock type for the GreetServerSender type
type GreetServerSender struct {
	mock.Mock
}

// SendGreetServer provides a mock function with given fields: _a0
func (_m *GreetServerSender) SendGreetServer(_a0 socket.Socket) error {
	ret := _m.Called(_a0)

	if len(ret) == 0 {
		panic("no return value specified for SendGreetServer")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(socket.Socket) error); ok {
		r0 = rf(_a0)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// NewGreetServerSender creates a new instance of GreetServerSender. It also registers a testing interface on the mock and a cleanup function to assert the mocks expectations.
// The first argument is typically a *testing.T value.
func NewGreetServerSender(t interface {
	mock.TestingT
	Cleanup(func())
}) *GreetServerSender {
	mock := &GreetServerSender{}
	mock.Mock.Test(t)

	t.Cleanup(func() { mock.AssertExpectations(t) })

	return mock
}
