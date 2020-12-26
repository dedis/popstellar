// lib defines general useful functions
package lib

import (
	"bytes"
	b64 "encoding/base64"
	"strings"
)

// Find is a helper function to find an int in a []int. It returns the index of the element and a bool in slice.
func Find(slice []int, val int) (int, bool) {
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

// Decode is a function that decodes a base-64 encoded string into a []byte.
// DEPRECATED : This is now done automatically by json.Marshall, kept here just in case.
func Decode(data string) ([]byte, error) {
	d, err := b64.StdEncoding.DecodeString(strings.Trim(data, `"`))
	return d, err
}

//MessageAndChannel is a return structure used by the Handle functions of package actor. It contains a Message and the
// Channel it should be sent on.
type MessageAndChannel struct {
	Channel []byte
	Message []byte
}

// ArrayArrayByteToArrayString converts an array of array of bytes into an array of string
func ArrayArrayByteToArrayString(slice [][]byte) []string {
	var sliceString []string
	for _, item := range slice {
		sliceString = append(sliceString, string(item))
	}
	return sliceString
}
