package messagedata

import (
	"encoding/base64"
	"golang.org/x/xerrors"
)

/* https://www.rfc-editor.org/rfc/rfc6749 */

// AuthenticationRequest defines the message data for an authentication
// request, according to the OpenID connect core specifications at
// https://openid.net/specs/openid-connect-core-1_0.html#ImplicitAuthRequest
type AuthenticationRequest struct {
	ResponseType string `json:"response_type"`
	ClientID     string `json:"client_id"`
	RedirectURI  string `json:"redirect_uri"`
	Scope        string `json:"scope"`
	State        string `json:"state,omitempty"`
	Nonce        string `json:"nonce"`
}

// Verify implements Verifiable. It verifies that the AuthenticationRequest message
// is correct
func (message AuthenticationRequest) Verify() error {

	// verify that the client id is based64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.ClientID)
	if err != nil {
		return xerrors.Errorf("client id is %s, should be base64URL encoded", message.ClientID)
	}
	// verify that the scope parameter contains openid scope value
	return nil
}

// GetObject implements MessageData
func (AuthenticationRequest) GetObject() string {
	return AuthenticationRequestObject
}

// GetAction implements MessageData
func (AuthenticationRequest) GetAction() string {
	return AuthRequestValidate
}

// NewEmpty implements MessageData
func (AuthenticationRequest) NewEmpty() MessageData {
	return &AuthenticationRequest{}
}
