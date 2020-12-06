// define/securityHelpers
package main

import (
	b64 "encoding/base64"
	"encoding/json"
	ed "crypto/ed25519"
	"crypto/sha256"
	"student20_pop/define"
	"testing"
	"fmt"
)

func TestMessageIsValid(t *testing.T) {
	var emptyTabString []string
	pubkey := "12345678901234567890123456789012"
	privkey := ed.NewKeyFromSeed([]byte(pubkey))
	var creation int64 = 123
	var	lastMod int64 = 123
	name :="My LAO"
	if((len(pubkey)!=ed.PublicKeySize) || len(privkey)!=ed.PrivateKeySize){
		t.Error("wrong argument -> size don't respected ")
	}
	idData := sha256.Sum256( []byte(pubkey + fmt.Sprint(creation) +name))
	var  data =  define.DataCreateLAO{
		Object:        "lao",
		Action:        "create",
		ID:            b64.StdEncoding.EncodeToString(idData[:]),
		Name:          name,
		Creation:      creation,
		Last_modified: lastMod,
		Organizer:     (b64.StdEncoding.EncodeToString([]byte(pubkey))),
		Witnesses:     emptyTabString,
		}

	dataFlat ,err := json.Marshal(data)
	if err != nil{
		t.Errorf("Error : %+v\n ,Impossible to marshal data",err)
	}
	signed := ed.Sign(privkey,dataFlat)
	id := sha256.Sum256(append(dataFlat, signed...))

	var message = define.Message {
		Data  :            []byte(b64.StdEncoding.EncodeToString(dataFlat)),// in base 64
		Sender :           (b64.StdEncoding.EncodeToString([]byte(pubkey))),
		Signature:         (b64.StdEncoding.EncodeToString(signed)),
		Message_id:        (b64.StdEncoding.EncodeToString(id[:])),
		WitnessSignatures: emptyTabString,
	}
	messageFlat ,err := json.Marshal(data)
	if err != nil{
		t.Errorf("Error : %+v\n ,Impossible to marshal message",err)
	}
	messProcessed,err :=define.AnalyseMessage(messageFlat)
	if err != nil{
		t.Errorf("Error : %+v\n encoutered, Analyse Message failed",err)
	}
	err = define.MessageIsValid(messProcessed)
	if err != nil{
		t.Errorf("Error, message %+v\n should be valid",message)
	}

}
