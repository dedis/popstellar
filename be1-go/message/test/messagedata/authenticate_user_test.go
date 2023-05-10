package messagedata

import (
	"github.com/stretchr/testify/require"
	"popstellar/message/messagedata"
	"testing"
)

// TestAuthUserInterfaceFunctions tests the basic interface methods of messagedata
func TestAuthUserInterfaceFunctions(t *testing.T) {
	var authMsg messagedata.AuthenticateUser
	require.Equal(t, messagedata.AuthObject, authMsg.GetObject())
	require.Equal(t, messagedata.AuthAction, authMsg.GetAction())
	require.Empty(t, authMsg.NewEmpty())
}

// TestVerify runs multiple times the verification method on different jsons
func TestVerify(t *testing.T) {

}
