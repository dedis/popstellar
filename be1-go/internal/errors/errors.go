package errors

import (
	"encoding/json"
	"fmt"
	"path/filepath"
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
	JsonMarshalErrorMsg         = InvalidMessageFieldErrorMsg + "failed to marshal JSON: "
	DecodeStringErrorMsg        = InvalidMessageFieldErrorMsg + "failed to decode string: "
	AccessDeniedErrorMsg        = "access denied: "
	InternalServerErrorMsg      = "internal server error: "

	DatabaseInsertErrorMsg            = InternalServerErrorMsg + "failed to insert into database: "
	DatabaseSelectErrorMsg            = InternalServerErrorMsg + "failed to select from database: "
	DatabaseDeleteErrorMsg            = InternalServerErrorMsg + "failed to delete from database: "
	DatabaseTransactionBeginErrorMsg  = InternalServerErrorMsg + "failed to start database transaction: "
	DatabaseTransactionCommitErrorMsg = InternalServerErrorMsg + "failed to commit database transaction: "
	DatabaseScanErrorMsg              = InternalServerErrorMsg + "failed to scan database row: "
	DatabaseIteratorErrorMsg          = InternalServerErrorMsg + "failed to iterate over database rows: "
	DatabaseRowsAffectedErrorMsg      = InternalServerErrorMsg + "failed to get number of rows affected: "
	DatabaseCreateTableErrorMsg       = InternalServerErrorMsg + "failed to create table: "
	DatabaseInternalErrorMsg          = InternalServerErrorMsg + "internal database error: "

	KeyMarshalErrorMsg   = InternalServerErrorMsg + "failed to marshal key: "
	KeyUnmarshalErrorMsg = InternalServerErrorMsg + "failed to unmarshal key: "
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
	n := runtime.Callers(3, pcs[:])
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
		if !ok {
			break
		}
		stackTrace += fmt.Sprintf("%s\n\t%s:%d\n", filepath.Base(frame.Function), filepath.Base(frame.File), frame.Line)
	}

	return stackTrace
}

type Frame struct {
	Function string `json:"function"`
	File     string `json:"file"`
	Line     int    `json:"line"`
}

func (p *PopError) GetStackTraceJSON() ([]byte, error) {
	frames := runtime.CallersFrames(p.stackTrace)
	var stackTraceArray []Frame

	for {
		frame, ok := frames.Next()
		if !ok {
			break
		}
		stackTraceArray = append(stackTraceArray, Frame{
			Function: filepath.Base(frame.Function),
			File:     filepath.Base(frame.File),
			Line:     frame.Line,
		})
	}

	return json.Marshal(stackTraceArray)
}

// NewInvalidActionError returns an error with the code -1 for an invalid action
func NewInvalidActionError(format string, a ...interface{}) error {
	return NewPopError(InvalidActionErrorCode, InvalidActionErrorMsg+format, a...)
}

// NewInvalidResourceError returns an error with the code -2 for an object with invalid resources
func NewInvalidResourceError(format string, a ...interface{}) error {
	return NewPopError(InvalidResourceErrorCode, InvalidResourceErrorMsg+format, a...)
}

// NewDuplicateResourceError returns an error with the code -3 for a resource that already exists
func NewDuplicateResourceError(format string, a ...interface{}) error {
	return NewPopError(DuplicateResourceErrorCode, DuplicateResourceErrorMsg+format, a...)
}

// NewInvalidMessageFieldError returns an error with the code -4 when a message field is bogus
func NewInvalidMessageFieldError(format string, a ...interface{}) error {
	return NewPopError(InvalidMessageFieldErrorCode, InvalidMessageFieldErrorMsg+format, a...)
}

// NewJsonUnmarshalError returns an error with -4 when it is impossible to unmarshal a json message
func NewJsonUnmarshalError(format string, a ...interface{}) error {
	return NewPopError(InvalidMessageFieldErrorCode, JsonUnmarshalErrorMsg+format, a...)
}

// NewJsonMarshalError returns an error with -4 when it is impossible to marshal a json message
func NewJsonMarshalError(format string, a ...interface{}) error {
	return NewPopError(InvalidMessageFieldErrorCode, JsonMarshalErrorMsg+format, a...)
}

// NewDecodeStringError returns an error with the code -4 when it is impossible to decode a string
func NewDecodeStringError(format string, a ...interface{}) error {
	return NewPopError(InvalidMessageFieldErrorCode, DecodeStringErrorMsg+format, a...)
}

// NewAccessDeniedError returns an error with the code -5 when an access is denied for the sender
func NewAccessDeniedError(format string, a ...interface{}) error {
	return NewPopError(AccessDeniedErrorCode, AccessDeniedErrorMsg+format, a...)
}

// NewInternalServerError returns an error with the code -6 when there is an internal server error
func NewInternalServerError(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, InternalServerErrorMsg+format, a)
}

// NewDatabaseInsertErrorMsg returns an error with the code -6 when there is an error with a database insert
func NewDatabaseInsertErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseInsertErrorMsg+format, a...)
}

// NewDatabaseSelectErrorMsg returns an error with the code -6 when there is an error with a database select
func NewDatabaseSelectErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseSelectErrorMsg+format, a...)
}

// NewDatabaseDeleteErrorMsg returns an error with the code -6 when there is an error with a database delete
func NewDatabaseDeleteErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseDeleteErrorMsg+format, a...)
}

// NewDatabaseTransactionBeginErrorMsg returns an error with the code -6 when there is an error with a database transaction
func NewDatabaseTransactionBeginErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseTransactionBeginErrorMsg+format, a...)
}

// NewDatabaseTransactionCommitErrorMsg returns an error with the code -6 when there is an error with a database transaction
func NewDatabaseTransactionCommitErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseTransactionCommitErrorMsg+format, a...)
}

// NewDatabaseScanErrorMsg returns an error with the code -6 when there is an error with a database scan
func NewDatabaseScanErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseScanErrorMsg+format, a...)
}

// NewDatabaseIteratorErrorMsg returns an error with the code -6 when there is an error with a database iterator
func NewDatabaseIteratorErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseIteratorErrorMsg+format, a...)
}

// NewDatabaseRowsAffectedErrorMsg returns an error with the code -6 when there is an error with a database rows affected
func NewDatabaseRowsAffectedErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseRowsAffectedErrorMsg+format, a...)
}

// NewKeyMarshalError returns an error with the code -6 when it is impossible to marshal a key
func NewKeyMarshalError(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, KeyMarshalErrorMsg+format, a...)
}

// NewKeyUnmarshalError returns an error with the code -6 when it is impossible to unmarshal a key
func NewKeyUnmarshalError(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, KeyUnmarshalErrorMsg+format, a...)
}

// NewDatabaseCreateTableErrorMsg returns an error with the code -6 when it is impossible to create a table
func NewDatabaseCreateTableErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseCreateTableErrorMsg+format, a...)
}

// NewDatabaseInternalErrorMsg returns an error with the code -6 when there is an internal database error
func NewDatabaseInternalErrorMsg(format string, a ...interface{}) error {
	return NewPopError(InternalServerErrorCode, DatabaseInternalErrorMsg+format, a...)
}
