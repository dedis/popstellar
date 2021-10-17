package validation

import (
	"encoding/base64"
	"github.com/rs/zerolog"
	"io"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestSchemaValidator_New(t *testing.T) {
	validator, err := NewSchemaValidator(nolog)
	require.NoError(t, err)
	require.NotNil(t, validator.genericMessageSchema)
	require.NotNil(t, validator.dataSchema)
}

func TestSchemaValidator_ValidateResponse(t *testing.T) {
	response := `{"jsonrpc":"2.0","result":[{"message_id":"8ADlKroQD5VNOPGJ5aYfSQXapwfRp1gxvU5oK85jPUs=","data":"eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiV2ViIFZvdGluZyBUZXN0IiwiY3JlYXRpb24iOjE2MjMzNDUwNzEsIm9yZ2FuaXplciI6IkhoQVhDQ190TlBOQnk3WUgxWjRkRjl5Qk42TmExd01KQUNtYlZhMngzZGM9Iiwid2l0bmVzc2VzIjpbXSwiaWQiOiI0ZWlzc1M0Vk5fQm1JeXZLUW9TSkFQZjF0a2hTcFF1U2dCdnU3Tzc2QWFBPSJ9","sender":"HhAXCC_tNPNBy7YH1Z4dF9yBN6Na1wMJACmbVa2x3dc=","signature":"j-bpWF-4eB0WGSbxUgQSFVcP4BRXG2AvfndjY4RbCN7DWPlCEfunVraPAg_4qpOWJs8FODZZQOai-w_YPMHWBg==","witness_signatures":[]}],"id":40}`

	validator, err := NewSchemaValidator(nolog)
	require.NoError(t, err)

	err = validator.VerifyJSON([]byte(response), GenericMessage)
	require.NoError(t, err)
}

func TestSchemaValidator_ValidateDataLAOCreate(t *testing.T) {
	dataEncoded := "eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiV2ViIFZvdGluZyBUZXN0IiwiY3JlYXRpb24iOjE2MjMzNDUwNzEsIm9yZ2FuaXplciI6IkhoQVhDQ190TlBOQnk3WUgxWjRkRjl5Qk42TmExd01KQUNtYlZhMngzZGM9Iiwid2l0bmVzc2VzIjpbXSwiaWQiOiI0ZWlzc1M0Vk5fQm1JeXZLUW9TSkFQZjF0a2hTcFF1U2dCdnU3Tzc2QWFBPSJ9"
	dataBuf, err := base64.URLEncoding.DecodeString(dataEncoded)
	require.NoError(t, err)

	validator, err := NewSchemaValidator(nolog)
	require.NoError(t, err)

	err = validator.VerifyJSON(dataBuf, Data)
	require.NoError(t, err)
}

func TestSchemaValidator_ValidateCatchupRequest(t *testing.T) {
	request := `{"jsonrpc":"2.0","method":"catchup","params":{"channel":"/root/kHQbFsB2Q_JxH55pJMfKNV3Mje5hPHjI7AZ4HIOlp40="},"id":39}`

	validator, err := NewSchemaValidator(nolog)
	require.NoError(t, err)

	err = validator.VerifyJSON([]byte(request), GenericMessage)
	require.NoError(t, err)
}

func TestSchemaValidator_ValidateCatchupRequestBadChannel(t *testing.T) {
	request := `{"jsonrpc":"2.0","method":"catchup","params":{"channel":"kHQbFsB2Q_JxH55pJMfKNV3Mje5hPHjI7AZ4HIOlp40="},"id":39}`

	validator, err := NewSchemaValidator(nolog)
	require.NoError(t, err)

	err = validator.VerifyJSON([]byte(request), GenericMessage)
	require.Error(t, err)
}

func TestSchemaValidator_ValidateAnswer(t *testing.T) {
	response := `{"jsonrpc":"2.0","result":0,"id":38}`

	validator, err := NewSchemaValidator(nolog)
	require.NoError(t, err)

	err = validator.VerifyJSON([]byte(response), GenericMessage)
	require.NoError(t, err)
}

func TestSchemaValidator_ValidatePublish(t *testing.T) {
	request := `{"method":"publish","id":11,"params":{"channel":"/root/4eissS4VN_BmIyvKQoSJAPf1tkhSpQuSgBvu7O76AaA=/cqAJNbhYsUcgqbqQKiDyCnlAcWKgeG1z-pz1acLr134=","message":{"data":"eyJvYmplY3QiOiJlbGVjdGlvbiIsImFjdGlvbiI6ImNhc3Rfdm90ZSIsImxhbyI6IjRlaXNzUzRWTl9CbUl5dktRb1NKQVBmMXRraFNwUXVTZ0J2dTdPNzZBYUE9IiwiY3JlYXRlZF9hdCI6MTYyMzM0NTY2MCwidm90ZXMiOlt7ImlkIjoieUIybEtHR1lSY0Robm4wWVdhOGJ0MlhrU0FHazBXNVNZQ3dBWHdWeVJIdz0iLCJxdWVzdGlvbiI6IlhPSmVDdXRsNzlJU1RFRkhWSVhTdUJmRWh6czF2V3lJYlZ3NFJ0U3FYSlk9Iiwidm90ZSI6WzFdfV0sImVsZWN0aW9uIjoiY3FBSk5iaFlzVWNncWJxUUtpRHlDbmxBY1dLZ2VHMXotcHoxYWNMcjEzND0ifQ==","sender":"Wto5aKBnfU0fIX2x1c_KB_-fVaW5COfOu-jLWkOIaWE=","signature":"Dy2EfE55nj9z4-d7xTqZV31pYRpwf2m4Rnleq7wTNvddWp1BbDEJnpg5uYfMt7qqHkSw3cZJKHxAnTD0quk2DQ==","message_id":"fc9ZXkNDjfhAc51PaLyBaBN-LOMA5nNx9sr9Xbvl2Ng=","witness_signatures":[]}},"jsonrpc":"2.0"}`

	validator, err := NewSchemaValidator(nolog)
	require.NoError(t, err)

	err = validator.VerifyJSON([]byte(request), GenericMessage)
	require.NoError(t, err)
}

func TestSchemaValidator_ValidatePublishBadMethod(t *testing.T) {
	request := `{"method":"publish_foo","id":11,"params":{"channel":"/root/4eissS4VN_BmIyvKQoSJAPf1tkhSpQuSgBvu7O76AaA=/cqAJNbhYsUcgqbqQKiDyCnlAcWKgeG1z-pz1acLr134=","message":{"data":"eyJvYmplY3QiOiJlbGVjdGlvbiIsImFjdGlvbiI6ImNhc3Rfdm90ZSIsImxhbyI6IjRlaXNzUzRWTl9CbUl5dktRb1NKQVBmMXRraFNwUXVTZ0J2dTdPNzZBYUE9IiwiY3JlYXRlZF9hdCI6MTYyMzM0NTY2MCwidm90ZXMiOlt7ImlkIjoieUIybEtHR1lSY0Robm4wWVdhOGJ0MlhrU0FHazBXNVNZQ3dBWHdWeVJIdz0iLCJxdWVzdGlvbiI6IlhPSmVDdXRsNzlJU1RFRkhWSVhTdUJmRWh6czF2V3lJYlZ3NFJ0U3FYSlk9Iiwidm90ZSI6WzFdfV0sImVsZWN0aW9uIjoiY3FBSk5iaFlzVWNncWJxUUtpRHlDbmxBY1dLZ2VHMXotcHoxYWNMcjEzND0ifQ==","sender":"Wto5aKBnfU0fIX2x1c_KB_-fVaW5COfOu-jLWkOIaWE=","signature":"Dy2EfE55nj9z4-d7xTqZV31pYRpwf2m4Rnleq7wTNvddWp1BbDEJnpg5uYfMt7qqHkSw3cZJKHxAnTD0quk2DQ==","message_id":"fc9ZXkNDjfhAc51PaLyBaBN-LOMA5nNx9sr9Xbvl2Ng=","witness_signatures":[]}},"jsonrpc":"2.0"}`

	validator, err := NewSchemaValidator(nolog)
	require.NoError(t, err)

	err = validator.VerifyJSON([]byte(request), GenericMessage)
	require.Error(t, err)
}

func TestSchemaValidator_ValidateCastVoteData(t *testing.T) {
	request := "eyJvYmplY3QiOiJlbGVjdGlvbiIsImFjdGlvbiI6ImNhc3Rfdm90ZSIsImxhbyI6IjRlaXNzUzRWTl9CbUl5dktRb1NKQVBmMXRraFNwUXVTZ0J2dTdPNzZBYUE9IiwiY3JlYXRlZF9hdCI6MTYyMzM0NTY2MCwidm90ZXMiOlt7ImlkIjoieUIybEtHR1lSY0Robm4wWVdhOGJ0MlhrU0FHazBXNVNZQ3dBWHdWeVJIdz0iLCJxdWVzdGlvbiI6IlhPSmVDdXRsNzlJU1RFRkhWSVhTdUJmRWh6czF2V3lJYlZ3NFJ0U3FYSlk9Iiwidm90ZSI6WzFdfV0sImVsZWN0aW9uIjoiY3FBSk5iaFlzVWNncWJxUUtpRHlDbmxBY1dLZ2VHMXotcHoxYWNMcjEzND0ifQ=="
	dataBuf, err := base64.URLEncoding.DecodeString(request)
	require.NoError(t, err)

	validator, err := NewSchemaValidator(nolog)
	require.NoError(t, err)

	err = validator.VerifyJSON(dataBuf, Data)
	require.NoError(t, err)
}

// -----------------------------------------------------------------------------
// Utility functions

var nolog = zerolog.New(io.Discard)