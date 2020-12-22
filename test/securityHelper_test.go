// define/securityHelpers
package main

import (
	ed "crypto/ed25519"
	"crypto/sha256"
	"encoding/json"
	"errors"
	"fmt"
	"math/rand"
	message2 "student20_pop/message"
	"student20_pop/parser"
	"student20_pop/security"
	"testing"
)

func CheckMessageIsValid(witnessSignatures []message2.ItemWitnessSignatures) error {
	//randomize the key
	randomSeed := make([]byte, 32)
	rand.Read(randomSeed)
	privkey := ed.NewKeyFromSeed(randomSeed)
	var pubkey = string(privkey.Public().(ed.PublicKey))
	var creation int64 = 123
	name := "My LAO"
	if (len(pubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return errors.New("wrong argument -> size of public key don't respected ")
	}

	idData := sha256.Sum256([]byte(pubkey + fmt.Sprint(creation) + name))
	var data = message2.DataCreateLAO{
		Object:    "lao",
		Action:    "create",
		ID:        idData[:],
		Name:      name,
		Creation:  creation,
		Organizer: []byte(pubkey),
		Witnesses: [][]byte{},
	}

	dataFlat, err := json.Marshal(data)
	if err != nil {
		return errors.New("Error : %+v\n ,Impossible to marshal data")
	}
	signed := ed.Sign(privkey, dataFlat)
	id := sha256.Sum256(append(dataFlat, signed...))
	//witness signatures
	ArrayOfWitnessSignatures := []json.RawMessage{}
	for i := 0; i < len(witnessSignatures); i++ {
		witnessSignatureI, err := json.Marshal(witnessSignatures[i])
		if err != nil {
			return errors.New("Problem when Marshaling witnessSignatures")
		}
		CoupleToAdd := witnessSignatureI[:]
		ArrayOfWitnessSignatures = append(ArrayOfWitnessSignatures, CoupleToAdd)
	}

	var message = message2.Message{
		Data:              dataFlat, // in base 64
		Sender:            []byte(pubkey),
		Signature:         signed,
		MessageId:         id[:],
		WitnessSignatures: ArrayOfWitnessSignatures,
	}
	messageFlat, err := json.Marshal(message)
	if err != nil {
		errors.New("Error : %+v\n ,Impossible to marshal message")
	}
	messProcessed, err := parser.ParseMessage(messageFlat)
	if err != nil {
		errors.New("Error : %+v\n encoutered, Analyse Message failed")
	}
	err = security.MessageIsValid(messProcessed)
	if err != nil {
		errors.New("Error, message %+v\n should be valid")
	}
	return nil
}

func TestMessageIsValidWithoutWitnesses(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		witnessSignatures := []message2.ItemWitnessSignatures{}
		/*for i:=0; i<len(witnessSignatures);i++{
			witness:= b64.StdEncoding.EncodeToString([]byte(witnessSignatures[i].Witness))
			signature:= b64.StdEncoding.EncodeToString([]byte(witnessSignatures[i].Signature))
			append(ArrayOfWitnessSignatures,(witness,signature) )
		}*/
		err := CheckMessageIsValid(witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}
