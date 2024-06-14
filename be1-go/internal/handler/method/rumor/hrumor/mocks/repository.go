// Code generated by mockery v2.42.1. DO NOT EDIT.

package mocks

import (
	mmessage "popstellar/internal/handler/message/mmessage"

	mock "github.com/stretchr/testify/mock"
)

// Repository is an autogenerated mock type for the Repository type
type Repository struct {
	mock.Mock
}

// CheckRumor provides a mock function with given fields: senderID, rumorID, timestamp
func (_m *Repository) CheckRumor(senderID string, rumorID int, timestamp map[string]int) (bool, bool, error) {
	ret := _m.Called(senderID, rumorID, timestamp)

	if len(ret) == 0 {
		panic("no return value specified for CheckRumor")
	}

	var r0 bool
	var r1 bool
	var r2 error
	if rf, ok := ret.Get(0).(func(string, int, map[string]int) (bool, bool, error)); ok {
		return rf(senderID, rumorID, timestamp)
	}
	if rf, ok := ret.Get(0).(func(string, int, map[string]int) bool); ok {
		r0 = rf(senderID, rumorID, timestamp)
	} else {
		r0 = ret.Get(0).(bool)
	}

	if rf, ok := ret.Get(1).(func(string, int, map[string]int) bool); ok {
		r1 = rf(senderID, rumorID, timestamp)
	} else {
		r1 = ret.Get(1).(bool)
	}

	if rf, ok := ret.Get(2).(func(string, int, map[string]int) error); ok {
		r2 = rf(senderID, rumorID, timestamp)
	} else {
		r2 = ret.Error(2)
	}

	return r0, r1, r2
}

// GetUnprocessedMessagesByChannel provides a mock function with given fields:
func (_m *Repository) GetUnprocessedMessagesByChannel() (map[string][]mmessage.Message, error) {
	ret := _m.Called()

	if len(ret) == 0 {
		panic("no return value specified for GetUnprocessedMessagesByChannel")
	}

	var r0 map[string][]mmessage.Message
	var r1 error
	if rf, ok := ret.Get(0).(func() (map[string][]mmessage.Message, error)); ok {
		return rf()
	}
	if rf, ok := ret.Get(0).(func() map[string][]mmessage.Message); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(map[string][]mmessage.Message)
		}
	}

	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// StoreRumor provides a mock function with given fields: rumorID, sender, unprocessed, processed
func (_m *Repository) StoreRumor(rumorID int, sender string, unprocessed map[string][]mmessage.Message, processed []string) error {
	ret := _m.Called(rumorID, sender, unprocessed, processed)

	if len(ret) == 0 {
		panic("no return value specified for StoreRumor")
	}

	var r0 error
	if rf, ok := ret.Get(0).(func(int, string, map[string][]mmessage.Message, []string) error); ok {
		r0 = rf(rumorID, sender, unprocessed, processed)
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
