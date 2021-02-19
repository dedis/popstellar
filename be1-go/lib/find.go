package lib

import "bytes"

// FindInt is a helper function to find an int in a []int. It returns the index of the element and a bool in slice.
func FindInt(slice []int, val int) (int, bool) {
	for i, item := range slice {
		if item == val {
			return i, true
		}
	}
	return -1, false
}

// FindStr is a helper function to find a string in a []string. It returns the index of the element and a bool in slice.
func FindStr(slice []string, val string) (int, bool) {
	for i, item := range slice {
		if item == val {
			return i, true
		}
	}
	return -1, false
}

// FindByteArray is a helper function to find a []byte in a [][]byte. It returns the index of the element and a bool
// in slice.
func FindByteArray(slice [][]byte, val []byte) (int, bool) {
	for i, item := range slice {
		if bytes.Equal(item, val) {
			return i, true
		}
	}
	return -1, false
}
