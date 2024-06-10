package mauthentification

import (
	"embed"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/messagedata"
	"testing"
)

//go:embed testdata/*.json
var testData embed.FS

// TestAuthUserInterfaceFunctions tests the basic interface methods of messagedata
func TestAuthUserInterfaceFunctions(t *testing.T) {
	var authMsg AuthenticateUser
	require.Equal(t, messagedata.AuthObject, authMsg.GetObject())
	require.Equal(t, messagedata.AuthAction, authMsg.GetAction())
	require.Empty(t, authMsg.NewEmpty())
}

// TestVerify runs multiple times the verification method on different jsons
func TestVerify(t *testing.T) {

	var authUser AuthenticateUser

	// action and object are constant
	object, action := "popcha", "authenticate"

	// generic method, takes a boolean argument, to assert whether the given json should be valid or not
	getTestValid := func(file string, shouldBeValid bool) func(*testing.T) {
		return func(t *testing.T) {
			// read the example file
			buf, err := testData.ReadFile("testdata/" + file)
			require.NoError(t, err)

			// check on the object and action
			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &authUser)
			require.NoError(t, err)

			// verify method (signature verification, base64 encoding verification)
			err = authUser.Verify()
			if shouldBeValid {
				require.NoError(t, err)
			} else {
				require.Error(t, err)
			}

		}
	}

	t.Run("valid authentication message", getTestValid("popcha_authenticate.json", true))
	t.Run("valid authentication message", getTestValid("popcha_authenticate_wrong_proof.json", false))
	t.Run("valid authentication message", getTestValid("popcha_authenticate_wrong_identifier.json", false))
	t.Run("valid authentication message", getTestValid("popcha_authenticate_wrong_nonce.json", false))

}
