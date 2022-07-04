// Package uint53 provides operations on 53-bits non-negative integers.
//
// These values can be handled without loss of precision in IEEE754 64-bit
// floating-point numbers, and thus in JSON.

package uint53

import (
	"errors"
)

// Uint53 defines the internal representation in this implementation
type Uint53 = uint64

// MaxUint53 is the highest allowed value for Uint53
const MaxUint53 Uint53 = 0x1F_FF_FF_FF_FF_FF_FF

// InRange check whether the value is in range for Uint53
func InRange(a Uint53) bool {
	return a <= MaxUint53
}

// SafePlus compute the sum of two Uint53 values, or indicate an overflow if
// the sum is too large.
func SafePlus(a, b Uint53) (Uint53, error) {
	if !InRange(a) || !InRange(b) {
		return 0, errors.New("Uint53.SafePlus: argument out of range")
	}
	r := a + b
	if !InRange(r) {
		return 0, errors.New("uint53 addition overflow")
	}

	return r, nil
}
