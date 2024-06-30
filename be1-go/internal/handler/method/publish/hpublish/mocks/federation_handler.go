// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import (
	mmessage "popstellar/internal/handler/message/mmessage"

	mock "github.com/stretchr/testify/mock"

	socket "popstellar/internal/network/socket"
)

// FederationHandler is an autogenerated mock type for the FederationHandler type
type FederationHandler struct {
	mock.Mock
}

// HandleWithSocket provides a mock function with given fields: channelPath, msg, _a2
func (_m *FederationHandler) HandleWithSocket(channelPath string, msg mmessage.Message, _a2 socket.Socket) error {
	ret := _m.Called(channelPath, msg, _a2)

	if len(ret) == 0 {
		panic("no return value specified for HandleWithSocket")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, mmessage.Message, socket.Socket) error); ok {
		r0 = rf(channelPath, msg, _a2)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// NewFederationHandler creates a new instance of FederationHandler. It also registers a testing interface on the mock and a cleanup function to assert the mocks expectations.
// The first argument is typically a *testing.T value.
func NewFederationHandler(t interface {
	mock.TestingT
	Cleanup(func())
}) *FederationHandler {
	mock := &FederationHandler{}
	mock.Mock.Test(t)

	t.Cleanup(func() { mock.AssertExpectations(t) })

	return mock
}
