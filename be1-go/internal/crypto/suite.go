// Package crypto defines the cryptographic suite
//
// The proof of personhood (PoP) project uses Ed25519 signatures which the
// servers need to verify in order to validate the incoming messages from
// the other parties.
//
// We use https://github.com/dedis/kyber for this purpose and use the Ed22519
// suite from it to perform all cryptographic operations.
package crypto

import "go.dedis.ch/kyber/v3/suites"

// Suite points to the `Ed25519` suite in Kyber.
var Suite = suites.MustFind("Ed25519")
