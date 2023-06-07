package messagedata

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/crypto"
)

// AuthenticateUser is a message containing the user authentication information
// necessary to generate the ID and Access Tokens in the OpenID Connect Protocol.
type AuthenticateUser struct {
	Object          string `json:"object"`
	Action          string `json:"action"`
	ClientID        string `json:"client_id"`
	Nonce           string `json:"nonce"`
	Identifier      string `json:"identifier"`
	IdentifierProof string `json:"identifier_proof"`
	State           string `json:"state"`
	ResponseMode    string `json:"response_mode"`
	PopchaAddress   string `json:"popcha_address"`
}

// NewEmpty creates an empty auth user message
func (msg AuthenticateUser) NewEmpty() MessageData {
	return &AuthenticateUser{}
}

// Verify checks core aspects of the message, including base64 encoding and signatures
func (msg AuthenticateUser) Verify() error {
	id, err := base64.URLEncoding.DecodeString(msg.Identifier)
	if err != nil {
		return xerrors.Errorf("Identifier is %s, should be base64URL encoded", msg.Identifier)
	}
	idProof, err := base64.URLEncoding.DecodeString(msg.IdentifierProof)
	if err != nil {
		return xerrors.Errorf("Identifier Proof is %s, should be base64URL encoded", msg.IdentifierProof)
	}
	nonce := []byte(msg.Nonce)

	// check that the identifier proof is valid
	err = schnorr.VerifyWithChecks(crypto.Suite, id, nonce, idProof)
	if err != nil {
		return xerrors.Errorf("Identifier proof has invalid signature: %v", err)
	}

	return nil
}

// GetObject implements MessageData
func (AuthenticateUser) GetObject() string {
	return AuthObject
}

// GetAction implements MessageData
func (AuthenticateUser) GetAction() string {
	return AuthAction
}
