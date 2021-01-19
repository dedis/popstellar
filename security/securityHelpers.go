// security contains all the function to perform checks on Data, from timestamp coherence to signature validation
package security

import (
	ed "crypto/ed25519"
	"encoding/json"
	"log"
	"student20_pop/lib"
	"student20_pop/parser"
)

const MaxPropagationDelay = 600
const MaxClockDifference = 100

// VerifySignature checks that Sign(itemToSigned) corresponds to the given signature
func VerifySignature(publicKey []byte, itemToVerify []byte, signature []byte) error {
	//check the size of the key as it will panic if we plug it in Verify
	if len(publicKey) != ed.PublicKeySize {
		return lib.ErrRequestDataInvalid
	}
	if ed.Verify(publicKey, itemToVerify, signature) {
		return nil
	}
	//invalid signature
	return lib.ErrRequestDataInvalid
}

// VerifyWitnessSignatures verify that for each WitnessSignatureEnc, the public key belongs to authorizedWitnesses and
// verifies that the signature is correct
func VerifyWitnessSignatures(authorizedWitnesses [][]byte, witnessSignaturesEnc []json.RawMessage, messageId []byte) error {
	for _, item := range witnessSignaturesEnc {
		witnessSignature, err := parser.ParseWitnessSignature(item)
		if err != nil {
			return err
		}
		//We check that the signature belong to an assigned witness
		_, isAssigned := lib.FindByteArray(authorizedWitnesses, witnessSignature.Signature)
		if !isAssigned {
			log.Printf("witness public key not recognized")
			return lib.ErrRequestDataInvalid
		}
		//then we check correctness of the signature
		err = VerifySignature(witnessSignature.WitnessKey, messageId, witnessSignature.Signature)
		if err != nil {
			return err
		}
	}
	return nil
}
