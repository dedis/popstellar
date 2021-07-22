// Package message is used to define all message types used in the PoP
// application. Please refer to the protocol specification in the protocol
// directory of the repository for the JSON-RPC schemas.
//
// Since we use JSON for serialisation, message types implement MarshalJSON
// and UnmarshalJSON methods for custom marshaling/unmarshaling logic.
package message
