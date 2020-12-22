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
type keys struct{
	private ed.PrivateKey
	public []byte
}

func CheckMessageIsValid(pubkey []byte,privkey ed.PrivateKey,data message2.DataCreateLAO,witnessKeysAndSignatures []message2.ItemWitnessSignatures,WitnesseKeys [][]byte) error {
	dataFlat, err := json.Marshal(data)
	if err != nil {
		return errors.New("Error : %+v\n ,Impossible to marshal data")
	}
	signed := ed.Sign(privkey, dataFlat)
	id := sha256.Sum256(append(dataFlat, signed...))

	//witness signatures
	ArrayOfWitnessSignatures,err := plugWitnessesInArray(witnessKeysAndSignatures)
	if err != nil{
		return err
	}
	var message = message2.Message{
		Data:              dataFlat, // in base 64
		Sender:            pubkey,
		Signature:         signed,
		MessageId:         id[:],
		WitnessSignatures: ArrayOfWitnessSignatures,
	}
	messageFlat, err := json.Marshal(message)
	if err != nil {
		return err
	}
	messProcessed, err := parser.ParseMessage(messageFlat)
	if err != nil {
		return err
	}
	err = security.MessageIsValid(messProcessed)
	if err != nil {
		return err
	}
	return nil
}

func TestMessageIsValidWithoutWitnesses(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey,privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		witnessKeys := [][]byte{}
		data, err:= createDataLao(pubkey,privkey,witnessKeys)
		if err != nil {
			t.Error(err)
		}
		err = CheckMessageIsValid(pubkey,privkey,data,witnessSignatures,witnessKeys)
		if err != nil {
			t.Error(err)
		}
	}
}
func TestMessageIsValidWithAssessedWitnesses(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey,privkey := createKeyPair()
		keyz := createArrayOfkeys()
		witnessKeys:= onlyPublicKeys(keyz)
		data, err:= createDataLao(pubkey,privkey,witnessKeys)
		if err != nil {
			t.Error(err)
		}
		id,err:= getIdofMessage(data,privkey)
		if err != nil {
			t.Error(err)
		}
		witnessSignatures := arrayOfWitnessSignatures(keyz,id)
		err = CheckMessageIsValid(pubkey,privkey,data,witnessSignatures,witnessKeys)
		if err != nil {
			t.Error(err)
		}
	}
}
func plugWitnessesInArray(witnessKeysAndSignatures []message2.ItemWitnessSignatures)([]json.RawMessage,error){
	ArrayOfWitnessSignatures := []json.RawMessage{}
	for i := 0; i < len(witnessKeysAndSignatures); i++ {
		witnessSignatureI, err := json.Marshal(witnessKeysAndSignatures[i])
		if err != nil {
			return nil ,errors.New("Problem when Marshaling witnessKeysAndSignatures")
		}
		CoupleToAdd := witnessSignatureI[:]
		ArrayOfWitnessSignatures = append(ArrayOfWitnessSignatures, CoupleToAdd)
	}
	return ArrayOfWitnessSignatures, nil
}
func createKeyPair()([]byte,ed.PrivateKey){
	//randomize the key
	randomSeed := make([]byte, 32)
	rand.Read(randomSeed)
	privkey := ed.NewKeyFromSeed(randomSeed)
	return privkey.Public().(ed.PublicKey), privkey
}

func createDataLao(pubkey []byte,privkey ed.PrivateKey,WitnesseKeys [][]byte) (message2.DataCreateLAO,error){
	var creation int64 = 123
	name := "My LAO"
	if (len(pubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return message2.DataCreateLAO{} ,errors.New("wrong argument -> size of public key don't respected ")
	}

	idData := sha256.Sum256([]byte(string(pubkey) + fmt.Sprint(creation) + name))
	var data = message2.DataCreateLAO{
		Object:    "lao",
		Action:    "create",
		ID:        idData[:],
		Name:      name,
		Creation:  creation,
		Organizer: []byte(pubkey),
		Witnesses: WitnesseKeys,
	}
	return data,nil
}
/*10 pair of keys*/
func createArrayOfkeys() []keys{
	keyz := []keys{}
	for i:=0; i<10;i++{
		publicW, privW :=	createKeyPair()
		keyz = append(keyz,keys{private: privW,public: []byte(publicW)})
	}
	return keyz
}
func onlyPublicKeys(ks []keys) [][]byte {
	var acc [][]byte
	for _, k := range ks {
		acc = append(acc, k.public)
	}
	return acc
}
func arrayOfWitnessSignatures(ks []keys,id []byte) []message2.ItemWitnessSignatures {
	var acc []message2.ItemWitnessSignatures
	for _, k := range ks {
		acc = append(acc, message2.ItemWitnessSignatures{k.public,ed.Sign(k.private, id)})
	}
	return acc
}
func getIdofMessage(data message2.DataCreateLAO,privkey ed.PrivateKey) ([]byte,error){
	dataFlat, err := json.Marshal(data)
	if err != nil {
		return nil,errors.New("Error : Impossible to marshal data")
	}
	signed := ed.Sign(privkey, dataFlat)
	id:= sha256.Sum256(append(dataFlat, signed...))
	return id[:],nil
}