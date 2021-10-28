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
func NewInvalidActionError(action string) *Error {
	return &Error{
		Code:        -1,
		Description: fmt.Sprintf("invalid action: %s", action),
	}
}

// NewInvalidObjectError returns an error with the code -1 for an invalid object.
func NewInvalidObjectError(action string) *Error {
	return &Error{
		Code:        -1,
		Description: fmt.Sprintf("invalid object: %s", action),
	}
}
