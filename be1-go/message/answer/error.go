package answer

import "fmt"

// Error defines a JSON RPC error
type Error struct {
	Code        int    `json:"code"`
	Description string `json:"description"`
}

// Error returns the string representation of an Error message.
func (e *Error) Error() string {
	return e.Description
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
	return NewErrorf(-1, "invalid action: "+format, a...)
}

// NewInvalidObjectError returns an error with the code -1 for an invalid object.
func NewInvalidObjectError(format string, a ...interface{}) *Error {
	return NewErrorf(-1, "invalid object: "+format, a...)
}

// NewInvalidResourceError returns an error with -2 for an object with invalid resources
func NewInvalidResourceError(format string, a ...interface{}) *Error {
	return NewErrorf(-2, "invalid resource: "+format, a...)
}

// NewDuplicateResourceError returns an error with -3 for a resource that already exists
func NewDuplicateResourceError(format string, a ...interface{}) *Error {
	return NewErrorf(-3, "duplicate resource: "+format, a...)
}

// NewInvalidMessageFieldError returns an error with -4 when a message field is bogus
func NewInvalidMessageFieldError(format string, a ...interface{}) *Error {
	return NewErrorf(-4, "invalid message field: "+format, a...)
}

// NewAccessDeniedError returns an error with -5 when an access is denied for the sender
func NewAccessDeniedError(format string, a ...interface{}) *Error {
	return NewErrorf(-5, "access denied: "+format, a...)
}

// NewInternalServerError returns an error with -6 when there is an internal server error
func NewInternalServerError(format string, a ...interface{}) *Error {
	return NewErrorf(-6, "internal server error: "+format, a...)
}
