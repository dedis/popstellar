package security

import (
	"encoding/json"
	ed "crypto/ed25519"
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
	//TODO verify witnesses are in event's witness list (@ouriel)
	for i := 0; i < len(witnessSignaturesEnc); i++ {
		witnessSignatures, err := parser.ParseWitnessSignature(witnessSignaturesEnc[i])
		if err != nil {
			return err
		}
		//We check that the signature belong to an assigned witness
		err = VerifySignature(witnessSignatures.Witness, message_id, witnessSignatures.Signature)
		if err != nil {
			return err
		}
	}
	return nil
}
