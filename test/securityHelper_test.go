// define/securityHelpers
package main

import (
	ed "crypto/ed25519"
	"crypto/sha256"
	b64 "encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"student20_pop/define"
	"testing"
	"math/rand"
)

type MessageSend struct {
	Data              []byte // in base 64
	Sender            string
	Signature         string
	Message_id        string
	WitnessSignatures []string
}

func CheckMessageIsValid( witnessSignatures []define.ItemWitnessSignatures )error{
	//randomize the key
	randomSeed := make([]byte, 32)
	rand.Read(randomSeed)
	privkey := ed.NewKeyFromSeed(randomSeed)
	var pubkey = string(privkey.Public().(ed.PublicKey))
	var creation int64 = 123
	name := "My LAO"
	if ((len(pubkey)!=ed.PublicKeySize) || len(privkey)!=ed.PrivateKeySize){
		return errors.New("wrong argument -> size of public key don't respected ")
	}

	idData := sha256.Sum256( []byte(pubkey + fmt.Sprint(creation) +name))
	var data = define.DataCreateLAO{
		Object:        "lao",
		Action:        "create",
		ID:            b64.StdEncoding.EncodeToString(idData[:]),
		Name:          name,
		Creation:      creation,
		Organizer:     (b64.StdEncoding.EncodeToString([]byte(pubkey))),
		Witnesses:   	 []string{},
	}

	dataFlat, err := json.Marshal(data)
	if err != nil{
		return errors.New("Error : %+v\n ,Impossible to marshal data")
	}
	signed := ed.Sign(privkey, dataFlat)
	id := sha256.Sum256(append(dataFlat, signed...))
	//witness signatures
	ArrayOfWitnessSignatures := []string{}
	for i:=0; i<len(witnessSignatures);i++{
		witnessSignatureI,err := json.Marshal(witnessSignatures[i])
		if err!=nil{
			return errors.New("Problem when Marshaling witnessSignatures")
		}
		CoupleToAdd := string(witnessSignatureI[:])
		ArrayOfWitnessSignatures = append(ArrayOfWitnessSignatures, CoupleToAdd)
	}

	var message = MessageSend{
		Data:              []byte(b64.StdEncoding.EncodeToString(dataFlat)), // in base 64
		Sender:            (b64.StdEncoding.EncodeToString([]byte(pubkey))),
		Signature:         (b64.StdEncoding.EncodeToString(signed)),
		Message_id:        (b64.StdEncoding.EncodeToString(id[:])),
		WitnessSignatures: ArrayOfWitnessSignatures,
	}
	messageFlat, err := json.Marshal(message)
	if err != nil{
		errors.New("Error : %+v\n ,Impossible to marshal message")
	}
	messProcessed, err := define.AnalyseMessage(messageFlat)
	if err != nil{
		errors.New("Error : %+v\n encoutered, Analyse Message failed")
	}
	err = define.MessageIsValid(messProcessed)
	if err != nil{
		errors.New("Error, message %+v\n should be valid")
	}
	return nil
}


func TestMessageIsValidWithoutWitnesses(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		witnessSignatures := []define.ItemWitnessSignatures {}
		/*for i:=0; i<len(witnessSignatures);i++{
			witness:= b64.StdEncoding.EncodeToString([]byte(witnessSignatures[i].Witness))
			signature:= b64.StdEncoding.EncodeToString([]byte(witnessSignatures[i].Signature))
			append(ArrayOfWitnessSignatures,(witness,signature) )
		}*/
		err := CheckMessageIsValid(witnessSignatures)
		if err!= nil{
			t.Error(err)
		}
	}
}
