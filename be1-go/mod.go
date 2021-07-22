// Package student20_pop defines the cryptographic suite
//
// The proof of personhood (PoP) project uses Ed25519 signatures
// which the organizer and witness servers need to verify in order to
// validate the incoming messages from the other parties.
//
// We use https://github.com/dedis/kyber for this purpose and use the Ed22519
// suite from it to perform all cryptographic operations.
package student20_pop
