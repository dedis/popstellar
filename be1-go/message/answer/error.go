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

// NewInvalidResourceError returns an error with -2 for an object with invalid ressources
func NewInvalidResourceError(description string) *Error {
	return &Error{
		Code:        -2,
		Description: fmt.Sprintf("invalid resource: %s", description),
	}
}

// NewDuplicateResourceError returns an error with -3 for a resource that already exists
func NewDuplicateResourceError(description string) *Error {
	return &Error{
		Code:        -3,
		Description: fmt.Sprintf("duplicate resource: %s", description),
	}
}

// NewInvalidMessageFieldError returns an error with -4 when a message field is bogus
func NewInvalidMessageFieldError(description string) *Error {
	return &Error{
		Code:        -4,
		Description: fmt.Sprintf("invalid message field: %s", description),
	}
}

// NewAccessDeniedError returns an error with -5 when an access is denied for the sender
func NewAccessDeniedError(description string) *Error {
	return &Error{
		Code:        -5,
		Description: fmt.Sprintf("access denied: %s", description),
	}
}

// NewInternalServerError returns an error with -6 when there is an internal server error
func NewInternalServerError(description string) *Error {
	return &Error{
		Code:        -6,
		Description: fmt.Sprintf("internal server error: %s", description),
	}
}
