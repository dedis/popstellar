// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import (
	mrumor "popstellar/internal/handler/method/rumor/mrumor"
	socket "popstellar/internal/network/socket"

	mock "github.com/stretchr/testify/mock"
)

// RumorHandler is an autogenerated mock type for the RumorHandler type
type RumorHandler struct {
	mock.Mock
}

// HandleRumorStateAnswer provides a mock function with given fields: _a0, rumor
func (_m *RumorHandler) HandleRumorStateAnswer(_a0 socket.Socket, rumor mrumor.ParamsRumor) error {
	ret := _m.Called(_a0, rumor)

	if len(ret) == 0 {
		panic("no return value specified for HandleRumorStateAnswer")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(socket.Socket, mrumor.ParamsRumor) error); ok {
		r0 = rf(_a0, rumor)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// SendRumor provides a mock function with given fields: _a0, rumor
func (_m *RumorHandler) SendRumor(_a0 socket.Socket, rumor mrumor.ParamsRumor) {
	_m.Called(_a0, rumor)
}

// NewRumorHandler creates a new instance of RumorHandler. It also registers a testing interface on the mock and a cleanup function to assert the mocks expectations.
// The first argument is typically a *testing.T value.
func NewRumorHandler(t interface {
	mock.TestingT
	Cleanup(func())
}) *RumorHandler {
	mock := &RumorHandler{}
	mock.Mock.Test(t)

	t.Cleanup(func() { mock.AssertExpectations(t) })

	return mock
}
