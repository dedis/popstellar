package answer

import (
	"encoding/json"
	"os"
	"path/filepath"
	message "popstellar/message"
	"popstellar/message/answer"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Answer_General(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "general_empty.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var msg message.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := message.GetType(buf)
	require.NoError(t, err)

	// > should be of type "answer"
	require.Equal(t, message.RPCTypeAnswer, rpctype)

	var answer answer.Answer

	err = json.Unmarshal(buf, &answer)
	require.NoError(t, err)

	// > result type should be empty
	require.True(t, answer.Result.IsEmpty())

	// > should contain the expected elements
	require.Equal(t, 999, *answer.ID)
	require.Equal(t, "2.0", answer.JSONRPC)
}

func Test_Error_functions(t *testing.T) {

	formatString := "check invalid error function"

	invalidAction := answer.NewInvalidActionError(formatString)
	require.Equal(t, -1, invalidAction.Code)
	require.Equal(t, "invalid action: "+formatString, invalidAction.Description)

	invalidObject := answer.NewInvalidObjectError(formatString)
	require.Equal(t, -1, invalidObject.Code)
	require.Equal(t, "invalid object: "+formatString, invalidObject.Description)

	invalidResource := answer.NewInvalidResourceError(formatString)
	require.Equal(t, -2, invalidResource.Code)
	require.Equal(t, "invalid resource: "+formatString, invalidResource.Description)

	duplicateResource := answer.NewDuplicateResourceError(formatString)
	require.Equal(t, -3, duplicateResource.Code)
	require.Equal(t, "duplicate resource: "+formatString, duplicateResource.Description)

	invalidField := answer.NewInvalidMessageFieldError(formatString)
	require.Equal(t, -4, invalidField.Code)
	require.Equal(t, "invalid message field: "+formatString, invalidField.Description)

	accessDenied := answer.NewAccessDeniedError(formatString)
	require.Equal(t, -5, accessDenied.Code)
	require.Equal(t, "access denied: "+formatString, accessDenied.Description)

	internalError := answer.NewInternalServerError(formatString)
	require.Equal(t, -6, internalError.Code)
	require.Equal(t, "internal server error: "+formatString, internalError.Description)

}
