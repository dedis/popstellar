// define/securityHelpers
package main

import (
	ed "crypto/ed25519"
	"crypto/rand"
	"crypto/sha256"
	b64 "encoding/base64"
	"encoding/json"
	"fmt"
	message2 "student20_pop/message"
	"student20_pop/parser"
	"student20_pop/security"
	"testing"
)

func CheckMessageIsValid( witnessSignatures []message2.ItemWitnessSignatures )error{
	//increase nb of tests
	for i := 0; i < 100; i++ {
		//randomize the key
		randomSeed := make([]byte, 32)
		rand.Read(randomSeed)
		privkey := ed.NewKeyFromSeed(randomSeed)
		var pubkey = string(privkey.Public().(ed.PublicKey))
		var creation int64 = 123
		name := "My LAO"
		if (len(pubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
			t.Error("wrong argument -> size don't respected ")
		}
		idData := sha256.Sum256([]byte(pubkey + fmt.Sprint(creation) + name))
		var data = message2.DataCreateLAO{
			Object:    "lao",
			Action:    "create",
			ID:        idData[:],
			Name:      name,
			Creation:  creation,
			Organizer: []byte(pubkey),
			Witnesses: []string{},
		}

		dataFlat, err := json.Marshal(data)
		if err != nil {
			t.Errorf("Error : %+v\n ,Impossible to marshal data", err)
		}
		signed := ed.Sign(privkey, dataFlat)
		id := sha256.Sum256(append(dataFlat, signed...))

		var message = MessageSend{
			//Marshall automatically encode data (Json.rawMessage) in Base64
			Data:              dataFlat,
			Sender:            b64.StdEncoding.EncodeToString([]byte(pubkey)),
			Signature:         b64.StdEncoding.EncodeToString(signed),
			Message_id:        b64.StdEncoding.EncodeToString(id[:]),
			WitnessSignatures: []string{},
		}
		messageFlat, err := json.Marshal(message)
		if err != nil {
			t.Errorf("Error : %+v\n ,Impossible to marshal message", err)
		}
		messProcessed, err := parser.ParseMessage(messageFlat)
		if err != nil {
			t.Errorf("Error : %+v\n encoutered, Analyse Message failed", err)
		}
		err = security.MessageIsValid(messProcessed)
		if err != nil {
			t.Errorf("Error, message %+v\n should be valid %v", message, err)
		}
	}
}

func TestMessageIsValid(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		witnessSignatures := []message2.ItemWitnessSignatures{}
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
}
}