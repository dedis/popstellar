package manswer

import "fmt"

const (
	InvalidActionErrorCode       = -1
	InvalidResourceErrorCode     = -2
	DuplicateResourceErrorCode   = -3
	InvalidMessageFieldErrorCode = -4
	AccessDeniedErrorCode        = -5
	InternalServerErrorCode      = -6
)

// Error defines a JSON RPC error
type Error struct {
	Code        int    `json:"code"`
	Description string `json:"description"`
}

// Error returns the string representation of an Error message.
func (e *Error) Error() string {
	return e.Description
}

func (e *Error) Wrap(description string) *Error {
	return &Error{
		Code:        e.Code,
		Description: fmt.Sprintf(description+": %v", e.Description),
	}
}

// NewError returns a *message.Error
func NewError(code int, description string) *Error {
	return &Error{
		Code:        code,
		Description: description,
	}
}

// NewErrorf returns a formatted *message.Error
func NewErrorf(code int, format string, values ...interface{}) *Error {
	return &Error{
		Code:        code,
		Description: fmt.Sprintf(format, values...),
	}
}

// NewInvalidActionError returns an error with the code -1 for an invalid action.
func NewInvalidActionError(format string, a ...interface{}) *Error {
	return NewErrorf(InvalidActionErrorCode, "invalid action: "+format, a...)
}

// NewInvalidResourceError returns an error with -2 for an object with invalid resources
func NewInvalidResourceError(format string, a ...interface{}) *Error {
	return NewErrorf(InvalidResourceErrorCode, "invalid resource: "+format, a...)
}

// NewDuplicateResourceError returns an error with -3 for a resource that already exists
func NewDuplicateResourceError(format string, a ...interface{}) *Error {
	return NewErrorf(DuplicateResourceErrorCode, "duplicate resource: "+format, a...)
}

// NewInvalidMessageFieldError returns an error with -4 when a message field is bogus
func NewInvalidMessageFieldError(format string, a ...interface{}) *Error {
	return NewErrorf(InvalidMessageFieldErrorCode, "invalid message field: "+format, a...)
}

// NewJsonUnmarshalError returns an error with -4 when it is impossible to unmarshal a json message
func NewJsonUnmarshalError(format string, a ...interface{}) *Error {
	return NewErrorf(InvalidMessageFieldErrorCode, "failed to unmarshal JSON: "+format, a...)
}

// NewAccessDeniedError returns an error with -5 when an access is denied for the sender
func NewAccessDeniedError(format string, a ...interface{}) *Error {
	return NewErrorf(AccessDeniedErrorCode, "access denied: "+format, a...)
}

// NewInternalServerError returns an error with -6 when there is an internal server error
func NewInternalServerError(format string, a ...interface{}) *Error {
	return NewErrorf(InternalServerErrorCode, "internal server error: "+format, a...)
}

// NewQueryDatabaseError returns an error with -6 when there is an error with a database query
func NewQueryDatabaseError(format string, a ...interface{}) *Error {
	return NewErrorf(InternalServerErrorCode, "failed to query from database: "+format, a...)
}

// NewStoreDatabaseError returns an error with -6 when there is an error with a database store
func NewStoreDatabaseError(format string, a ...interface{}) *Error {
	return NewErrorf(InternalServerErrorCode, "failed to store inside database: "+format, a...)
}
