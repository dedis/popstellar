package security

import (
	ed "crypto/ed25519"
	"encoding/json"
	"student20_pop/lib"
	"student20_pop/parser"
)

const MaxPropagationDelay = 600
const MaxClockDifference = 100

/*
	we check that Sign(sender||data) is the given signature
*/
func VerifySignature(publicKey []byte, data []byte, signature []byte) error {
	//check the size of the key as it will panic if we plug it in Verify
	if len(publicKey) != ed.PublicKeySize {
		return lib.ErrRequestDataInvalid
	}
	if ed.Verify(publicKey, data, signature) {
		return nil
	}
	//invalid signature
	return lib.ErrRequestDataInvalid
}

/*
	handling of dynamic updates with object as item and not just string
	*publicKeys is already decoded
    *sender and signature are not already decoded
*/
func VerifyWitnessSignatures(authorizedWitnesses [][]byte, witnessSignaturesEnc []json.RawMessage, message_id []byte) error {
	for _, item := range witnessSignaturesEnc {
		witnessSignature, err := parser.ParseWitnessSignature(item)
		if err != nil {
			return err
		}
		//We check that the signature belong to an assigned witness
		_, isAssigned := lib.FindByteArray(authorizedWitnesses, witnessSignature.Signature)
		//TODO do we stop if one signature came from a witness which is not in the list ?
		if !isAssigned {
			return lib.ErrRequestDataInvalid
		}
		//then we check correctness of the signature
		err = VerifySignature(witnessSignature.WitnessKey, message_id, witnessSignature.Signature)
		if err != nil {
			return err
		}
	}
	return nil
}
