package define

import (
	"errors"
)

/*TODO:
errorHandling
responseToJson in marshall
handle errors within wholemessage
*/

var ErrInvalidAction = errors.New("invalid action")


func ErrToInt(err error) int {
	//TODO
	return 0
}