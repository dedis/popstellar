package security

import (
	ed "crypto/ed25519"
	"crypto/rand"
	"encoding/json"
	"errors"
	mRand "math/rand"
	"student20_pop/lib"
	message2 "student20_pop/message"
	"testing"
)

type keys struct {
	private ed.PrivateKey
	public  []byte
}

// we don't check  that the key's length is 32 in the verification
// we don't check the utility function HashItems which basically just hash his arguments

// TestCorrectSignaturesAndCorrectWitnesses tests if VerifyWitnessSignatures works with correct arguments
func TestCorrectSignaturesAndCorrectWitnesses(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		AuthorisedWitnessKeys, jsonArrayOfWitnessSigntures, id, err := witnessesAndSignatures(true, true)
		if err != nil {
			t.Errorf("Problem when Marshaling witnessKeysAndSignatures")
		}
		err = VerifyWitnessSignatures(AuthorisedWitnessKeys, jsonArrayOfWitnessSigntures, id)
		if err != nil {
			t.Errorf("At least one of the signatures is invalid")
		}
	}
}

// TestCorrectSignaturesAndCorrectWitnesses tests if VerifyWitnessSignatures raises an error if the witnesses are incorrect
func TestCorrectSignaturesAndBadWitnesses(t *testing.T) {
	AuthorisedWitnessKeys, jsonArrayOfWitnessSignatures, id, err := witnessesAndSignatures(false, true)
	if err != nil {
		t.Errorf("Problem when Marshaling witnessKeysAndSignatures")
	}
	err = VerifyWitnessSignatures(AuthorisedWitnessKeys, jsonArrayOfWitnessSignatures, id)
	if err != lib.ErrRequestDataInvalid {
		t.Errorf("The verifier  didn't notice unauthenticated witness")
	}
}

// TestCorrectSignaturesAndCorrectWitnesses tests if VerifyWitnessSignatures raises an error if the signatures and the witnesses are incorrect
func TestBadSignaturesAndBadWitnesses(t *testing.T) {
	AuthorisedWitnessKeys, jsonArrayOfWitnessSignatures, id, err := witnessesAndSignatures(false, false)
	if err != nil {
		t.Errorf("Problem when Marshaling witnessKeysAndSignatures")
	}
	err = VerifyWitnessSignatures(AuthorisedWitnessKeys, jsonArrayOfWitnessSignatures, id)
	if err != lib.ErrRequestDataInvalid {
		t.Errorf("The verifier  didn't notice wrong signature(s) and unauthenticated witness")
	}
}

// TestCorrectSignaturesAndCorrectWitnesses tests if VerifyWitnessSignatures raises an error if the signatures are incorrect
func TestBadSignaturesAndCorrectWitnesses(t *testing.T) {
	AuthorisedWitnessKeys, jsonArrayOfWitnessSigantures, id, err := witnessesAndSignatures(false, false)
	if err != nil {
		t.Errorf("Problem when Marshaling witnessKeysAndSignatures")
	}
	err = VerifyWitnessSignatures(AuthorisedWitnessKeys, jsonArrayOfWitnessSigantures, id)
	if err != lib.ErrRequestDataInvalid {
		t.Errorf("The verifier  didn't notice wrong signature(s)")
	}
}

//
func witnessesAndSignatures(correctWitnesses bool, correctSignatures bool) (authorisedKeys [][]byte, witnessSignatures []json.RawMessage, id []byte, err error) {
	id = make([]byte, 32)
	_, _ = rand.Read(id)

	keys := generateKeyPairs()
	publicKeys := extractPublicKeys(keys)
	signatures := arraySign(keys, id)
	authorisedKeys = publicKeys

	if !correctWitnesses {
		_, _ = rand.Read(authorisedKeys[0])
	}
	if !correctSignatures {
		_, _ = rand.Read(signatures[mRand.Intn(len(signatures))].Signature)
	}
	if !correctSignatures && !correctWitnesses {
		keys := generateKeyPairs()
		signatures = arraySign(keys, id)
	}

	witnessSignatures, err = PlugWitnessesInArray(signatures)
	if err != nil {
		return nil, nil, nil, errors.New("Problem when Marshaling witnessKeysAndSignatures")
	}
	return authorisedKeys, witnessSignatures, id, nil
}

// generateKeyPairs returns an array of 10 pairs of public and private keys
func generateKeyPairs() []keys {
	var keyList []keys
	for i := 0; i < 10; i++ {
		publicKey, privateKey := createKeyPair()
		keyList = append(keyList, keys{private: privateKey, public: publicKey})
	}
	return keyList
}

//extractPublicKeys returns an array containing the public Keys of keys
func extractPublicKeys(ks []keys) [][]byte {
	var acc [][]byte
	for _, k := range ks {
		acc = append(acc, k.public)
	}
	return acc
}

// arraySign returns an array of message2.ItemWitnessSignatures, where to each element in ks corresponds a pair
// k.public, Sign(k.private, id)
func arraySign(ks []keys, id []byte) []message2.ItemWitnessSignatures {
	var acc []message2.ItemWitnessSignatures
	for _, k := range ks {
		acc = append(acc, message2.ItemWitnessSignatures{WitnessKey: k.public, Signature: ed.Sign(k.private, id)})
	}
	return acc
}
