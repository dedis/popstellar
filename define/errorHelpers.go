package define

import (
	"errors"
)


var ErrInvalidAction = errors.New("invalid action")
var ErrInvalidResource = errors.New("invalid resource")
var ErrResourceAlreadyExists = errors.New("resource already exists")
var ErrRequestDataInvalid = errors.New("request data is invalid")
var ErrAccessDenied = errors.New("access denied")

