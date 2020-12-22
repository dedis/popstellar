/* GENERAL USEFUL FUNCTIONS
Currently only find elem in array */
package lib

import (
	"bytes"
	b64 "encoding/base64"
	"strings"
)

/* helper function to find an elem. in a slice.  returns index of elem + bool in slice*/
func Find(slice []int, val int) (int, bool) {
	for i, item := range slice {
		if item == val {
			return i, true
		}
	}
	return -1, false
}

func FindStr(slice []string, val string) (int, bool) {
	for i, item := range slice {
		if item == val {
			return i, true
		}
	}
	return -1, false
}
func FindBytarr(slice [][]byte, val []byte) (int, bool) {
	for i, item := range slice {
		if bytes.Equal(item, val) {
			return i, true
		}
	}
	return -1, false
}

func Decode(data string) ([]byte, error) {
	d, err := b64.StdEncoding.DecodeString(strings.Trim(data, `"`))
	return d, err
}

type MessageAndChannel struct {
	Channel []byte
	Message []byte
}

func ConvertSliceSliceByteToSliceString(slice [][]byte) []string {
	sliceString := []string{}
	for _, item := range slice {
		sliceString = append(sliceString, string(item))
	}
	return sliceString
}
/*
`"` and `\` characters must be escaped by adding a `\` characters before them.
`"` becomes `\"` and `\` becomes `\\`.
*/
func Escape(s string) string{
	return strings.ReplaceAll(strings.ReplaceAll(s, "\\", "\\\\"), "\"", "\\\"")
}