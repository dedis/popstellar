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

//=====================================================================================/
func witnessesAndSignatures(correctWitnesses bool, correctSignatures bool) (AuthorisedWitnessKeys [][]byte, jsonArrayOfWitnessSigntures []json.RawMessage, id []byte, err error) {
	id = make([]byte, 32)
	rand.Read(id)

	keys := createArrayOfkeys()
	pubkeys := onlyPublicKeys(keys)
	witnessSignatures := arrayOfWitnessSignatures(keys, id)
	AuthorisedWitnessKeys = pubkeys

	if !correctWitnesses {
		rand.Read(AuthorisedWitnessKeys[0])
		//rand.Read(AuthorisedWitnessKeys[mRand.Intn(len(AuthorisedWitnessKeys))])
	}
	if !correctSignatures {
		rand.Read(witnessSignatures[mRand.Intn(len(witnessSignatures))].Signature)
	}
	if !correctSignatures && !correctWitnesses {
		keys := createArrayOfkeys()
		pubkeys = onlyPublicKeys(keys)
		witnessSignatures = arrayOfWitnessSignatures(keys, id)
	}

	jsonArrayOfWitnessSigntures, err = PlugWitnessesInArray(witnessSignatures)
	if err != nil {
		return nil, nil, nil, errors.New("Problem when Marshaling witnessKeysAndSignatures")
	}
	return AuthorisedWitnessKeys, jsonArrayOfWitnessSigntures, id, nil
}

/*10 pair of keys*/
func createArrayOfkeys() []keys {
	keyz := []keys{}
	for i := 0; i < 10; i++ {
		publicW, privW := createKeyPair()
		keyz = append(keyz, keys{private: privW, public: []byte(publicW)})
	}
	return keyz
}
//onlyPublicKeys returns an array containing the public Keys of keys
func onlyPublicKeys(ks []keys) [][]byte {
	var acc [][]byte
	for _, k := range ks {
		acc = append(acc, k.public)
	}
	return acc
}
func arrayOfWitnessSignatures(ks []keys, id []byte) []message2.ItemWitnessSignatures {
	var acc []message2.ItemWitnessSignatures
	for _, k := range ks {
		acc = append(acc, message2.ItemWitnessSignatures{k.public, ed.Sign(k.private, id)})
	}
	return acc
}
