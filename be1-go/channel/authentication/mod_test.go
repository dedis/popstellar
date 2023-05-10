package authentication

import (
	"github.com/golang-jwt/jwt/v4"
	"github.com/rs/xid"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/stretchr/testify/require"
	"io"
	"popstellar/message/messagedata"
	"testing"
)

const (
	laoID                             = "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	authName                          = "/root/" + laoID + "/authentication"
	relativeMsgDataExamplePath string = "../../../protocol/examples/messageData"
	relativeQueryExamplePath   string = "../../../protocol/examples/query"
)

// TestJWTToken creates a JWT token with arbitrary parameters, and parse it to assert its correctness.
func TestJWTToken(t *testing.T) {
	sk, pk, err := loadRSAKeys()
	require.NoError(t, err)

	webAddr := "https://server.example.com"
	ppid := "ppid12564"
	cID := "cID122dw"
	nonce := "n0nc3"
	// creating the token
	idToken, err := createJWTString(webAddr, ppid, cID, nonce, sk)
	require.NoError(t, err)

	log.Info().Msg(idToken)
	// verifying the token
	token, err := jwt.Parse(idToken, func(jwtToken *jwt.Token) (interface{}, error) {
		_, ok := jwtToken.Method.(*jwt.SigningMethodRSA)
		require.True(t, ok)
		return pk, nil
	})

	claims, ok := token.Claims.(jwt.MapClaims)
	require.True(t, ok)
	//checking the parsing is correct
	require.NoError(t, err)
	require.True(t, token.Valid)
	require.Equal(t, claims["nonce"], nonce)
	require.NotEmptyf(t, claims["auth_time"], "Authentication time is nil")
	require.Equal(t, claims["aud"], cID)
	require.Equal(t, claims["iss"], webAddr)
	require.NotEmpty(t, claims["iat"], "Expiration time is nil")
	require.Equal(t, claims["sub"], ppid)

}

// TestURIParamSConstruction creates a fake authentication message, and assert
// whether the corresponding Redirect URI can be created without error.
func TestURIParamsConstruction(t *testing.T) {
	// creating a fake authorization message
	authMsg := &messagedata.AuthenticateUser{
		Object:          messagedata.AuthObject,
		Action:          messagedata.AuthAction,
		ClientID:        "cl1ent",
		Nonce:           "n0nce",
		Identifier:      xid.New().String(),
		IdentifierProof: "pr00f",
		State:           "123state",
		ResponseMode:    "query",
		PopchaAddress:   "https://server.example.com",
	}
	// creating a fake channel, we will not use it in this test
	c := NewChannel("", nil, zerolog.New(io.Discard))
	_, err := constructRedirectURIParams(c, authMsg)
	require.NoError(t, err)
}
