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
func NewInvalidActionError(action string, values ...interface{}) *Error {
	return NewErrorf(
	-1,
	"invalid action: "+ action,
	values,
	)
}

// NewInvalidObjectError returns an error with the code -1 for an invalid object.
func NewInvalidObjectError(action string, values ... interface{}) *Error {
	return NewErrorf(
		-1,
		"invalid object: "+ action,
		values,
	)
}

// NewInvalidResourceError returns an error with -2 for an object with invalid resources
func NewInvalidResourceError(description string, values ... interface{}) *Error {
	return NewErrorf(
		-2,
		"invalid resource: "+ description,
		values,
	)
}

// NewDuplicateResourceError returns an error with -3 for a resource that already exists
func NewDuplicateResourceError(description string, values ... interface{}) *Error {
	return NewErrorf(
		-3,
		"duplicate resource: "+ description,
		values,
	)
}

// NewInvalidMessageFieldError returns an error with -4 when a message field is bogus
func NewInvalidMessageFieldError(description string, values ... interface{}) *Error {
	return NewErrorf(
		-4,
		"invalid message field: "+ description,
		values,
	)
}

// NewAccessDeniedError returns an error with -5 when an access is denied for the sender
func NewAccessDeniedError(description string, values ... interface{}) *Error {
	return NewErrorf(
		-5,
		"access denied: "+ description,
		values,
	)
}

// NewInternalServerError returns an error with -6 when there is an internal server error
func NewInternalServerError(description string, values ... interface{}) *Error {
	return NewErrorf(
		-6,
		"internal server error: "+ description,
		values,
	)
}
