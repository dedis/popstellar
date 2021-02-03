// lib defines general useful functions
package lib

import (
	ed "crypto/ed25519"

	// crypto/rand
	"math/rand"
	"strings"
)

// MessageAndChannel is a return structure used by the Handle functions of package actor. It contains a Message and the
// Channel it should be sent on.
type MessageAndChannel struct {
	Channel []byte
	Message []byte
}

// NestedByteArrayToStringArray converts an array of array of bytes into an array of string
func NestedByteArrayToStringArray(slice [][]byte) []string {
	var sliceString []string
	for _, item := range slice {
		sliceString = append(sliceString, string(item))
	}
	return sliceString
}

// StringArrayToNestedByteArray converts an array of strings to a [][]byte
func StringArrayToNestedByteArray(slice []string) [][]byte {
	var byteString [][]byte
	for _, item := range slice {
		byteString = append(byteString, []byte(item))
	}
	return byteString
}

// EscapeAndQuote escapes the following characters the following way:
// `"` and `\` characters must be escaped by adding a `\` characters before them.
// `"` becomes `\"` and `\` becomes `\\`.
func EscapeAndQuote(s string) string {
	str := strings.ReplaceAll(strings.ReplaceAll(s, "\\", "\\\\"), "\"", "\\\"")
	return `"` + str + `"`
}

// ArrayRepresentation returns a json Array with the strings given as arguments. It will escape them with the EscapeAndQuote
// function first.
// Typically used in hashes to prevent security troubles due to bad concatenation
func ArrayRepresentation(elements []string) string {
	str := "["
	if len(elements) > 0 {
		str = "[" + EscapeAndQuote(elements[0])
		for i := 1; i < len(elements); i++ {
			str += "," + EscapeAndQuote(elements[i])
		}
	}
	str += "]"
	return str
}

// GenerateTestKeyPair returns a pair of public and private key. Only used for tests. Should we create a package test and
// put it there ?
func GenerateTestKeyPair() ([]byte, ed.PrivateKey) {
	//randomize the key
	randomSeed := make([]byte, ed.PublicKeySize)
	rand.Read(randomSeed)
	privateKey := ed.NewKeyFromSeed(randomSeed)
	return privateKey.Public().(ed.PublicKey), privateKey
}
