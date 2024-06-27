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