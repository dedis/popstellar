package errors

import (
	"fmt"
	"runtime"
)

const (
	depthStack = 32

	InvalidActionErrorCode       = -1
	InvalidResourceErrorCode     = -2
	DuplicateResourceErrorCode   = -3
	InvalidMessageFieldErrorCode = -4
	AccessDeniedErrorCode        = -5
	InternalServerErrorCode      = -6

	InvalidActionErrorMsg       = "invalid action: "
	InvalidResourceErrorMsg     = "invalid resource: "
	DuplicateResourceErrorMsg   = "duplicate resource: "
	InvalidMessageFieldErrorMsg = "invalid message field: "
	JsonUnmarshalErrorMsg       = InvalidMessageFieldErrorMsg + "failed to unmarshal JSON: "
	AccessDeniedErrorMsg        = "access denied: "
	InternalServerErrorMsg      = "internal server error: "
	JsonMarshalErrorMsg         = InternalServerErrorMsg + "failed to marshal JSON: "
	QueryDatabaseErrorMsg       = InternalServerErrorMsg + "failed to query from database: "
	StoreDatabaseErrorMsg       = InternalServerErrorMsg + "failed to store inside database: "
)

// PopError defines a custom error type that includes a stack trace.
type PopError struct {
	code       int
	message    string
	stackTrace []uintptr
}

// Error implements the error interface.
func (p *PopError) Error() string {
	return fmt.Sprintf("%d: %s", p.code, p.message)
}

func NewPopError(code int, format string, a ...interface{}) *PopError {

	var pcs [depthStack]uintptr
	n := runtime.Callers(2, pcs[:])
	stack := pcs[0:n]

	return &PopError{
		code:       code,
		message:    fmt.Sprintf(format, a...),
		stackTrace: stack,
	}
}

func (p *PopError) Code() int {
	return p.code
}

func (p *PopError) StackTraceString() string {
	frames := runtime.CallersFrames(p.stackTrace)
	stackTrace := ""

	for {
		frame, ok := frames.Next()
		stackTrace += fmt.Sprintf("%s\n\t%s:%d\n", frame.Function, frame.File, frame.Line)
		if !ok {
			break
		}
	}
	return stackTrace
}

// NewInvalidActionError returns an error with the code -1 for an invalid action
func NewInvalidActionError(format string, a ...interface{}) error {
	return NewPopError(InvalidActionErrorCode, InvalidActionErrorMsg+format, a)
}

// NewInvalidResourceError returns an error with the code -2 for an object with invalid resources
func NewInvalidResourceError(format string, a ...interface{}) error {
	return NewPopError(InvalidResourceErrorCode, InvalidResourceErrorMsg+format, a)
}

// NewDuplicateResourceError returns an error with the code -3 for a resource that already exists
func NewDuplicateResourceError(format string, a ...interface{}) error {
	return NewPopError(DuplicateResourceErrorCode, DuplicateResourceErrorMsg+format, a)
}

// NewInvalidMessageFieldError returns an error with the code -4 when a message field is bogus
func NewInvalidMessageFieldError(format string, a ...interface{}) error {
	return NewPopError(InvalidMessageFieldErrorCode, InvalidMessageFieldErrorMsg+format, a)
}

// NewJsonUnmarshalError returns an error with -4 when it is impossible to unmarshal a json message
func NewJsonUnmarshalError(format string, a ...interface{}) error {
	return NewPopError(InvalidMessageFieldErrorCode, JsonUnmarshalErrorMsg+format, a)
}

// NewAccessDeniedError returns an error with the code -5 when an access is denied for the sender
func NewAccessDeniedError(format string, a ...interface{}) error {
	return NewPopError(AccessDeniedErrorCode, AccessDeniedErrorMsg+format, a)
}

// NewInternalServerError returns an error with the code -6 when there is an internal server error
func NewInternalServerError(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, InternalServerErrorMsg+format, a)
}

// NewJsonMarshalError returns an error with -6 when it is impossible to marshal a json message
func NewJsonMarshalError(format string, a ...interface{}) error {
	return NewPopError(InvalidMessageFieldErrorCode, JsonMarshalErrorMsg+format, a)
}

// NewQueryDatabaseError returns an error with the code -6 when there is an error with a database query
func NewQueryDatabaseError(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, QueryDatabaseErrorMsg+format, a)
}

// NewStoreDatabaseError returns an error with the code -6 when there is an error with a database store
func NewStoreDatabaseError(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, StoreDatabaseErrorMsg+format, a)
}
