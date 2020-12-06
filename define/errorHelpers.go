/* INTERNAL ERRORS */
package define

import (
	"errors"
)

var ErrInvalidAction = errors.New("invalid action")
var ErrInvalidResource = errors.New("invalid resource")
var ErrResourceAlreadyExists = errors.New("resource already exists")
var ErrRequestDataInvalid = errors.New("request data is invalid")
var ErrAccessDenied = errors.New("access denied")

var ErrDBFault = errors.New("error in the DB")
var ErrIdNotDecoded = errors.New("could not unmarshall message's ID")
var ErrEncodingFault = errors.New("encoding error")
