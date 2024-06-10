// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import (
	mock "github.com/stretchr/testify/mock"
	"popstellar/internal/message/method"
)

// Peers is an autogenerated mock type for the Peers type
type Peers struct {
	mock.Mock
}

// AddPeerGreeted provides a mock function with given fields: socketID
func (_m *Peers) AddPeerGreeted(socketID string) {
	_m.Called(socketID)
}

// AddPeerInfo provides a mock function with given fields: socketID, info
func (_m *Peers) AddPeerInfo(socketID string, info method.GreetServerParams) error {
	ret := _m.Called(socketID, info)

	if len(ret) == 0 {
		panic("no return value specified for AddPeerInfo")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(string, method.GreetServerParams) error); ok {
		r0 = rf(socketID, info)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// IsPeerGreeted provides a mock function with given fields: socketID
func (_m *Peers) IsPeerGreeted(socketID string) bool {
	ret := _m.Called(socketID)

	if len(ret) == 0 {
		panic("no return value specified for IsPeerGreeted")
	}

	var r0 bool
	if rf, ok := ret.Get(0).(func(string) bool); ok {
		r0 = rf(socketID)
	} else {
		r0 = ret.Get(0).(bool)
	}

	return r0
}

// NewPeers creates a new instance of Peers. It also registers a testing interface on the mock and a cleanup function to assert the mocks expectations.
// The first argument is typically a *testing.T value.
func NewPeers(t interface {
	mock.TestingT
	Cleanup(func())
}) *Peers {
	mock := &Peers{}
	mock.Mock.Test(t)

	t.Cleanup(func() { mock.AssertExpectations(t) })

	return mock
}
