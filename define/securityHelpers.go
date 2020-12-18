package define

import (
	"bytes"
	ed "crypto/ed25519"
	"crypto/sha256"
	"fmt"
	"strconv"
	"time"
)

const MaxPropagationDelay = 600

// TODO if we use the json Schema, don't need to check structure correctness
func LAOCreatedIsValid(data DataCreateLAO, message Message) bool {
	//the last modified timestamp is equal to the creation timestamp,
	/*/if data.Creation != data.Last_modified {
		fmt.Printf("%v, %v", data, data.Last_modified)
		fmt.Printf("sec1")
		return false
	}*/
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation < time.Now().Unix()-MaxPropagationDelay {
		fmt.Printf("timestamp invalid, either too old or in the future : %v", data.Creation)
		return false
	}
	//the attestation is valid,
	str := []byte(data.Organizer)
	str = append(str, []byte(strconv.FormatInt(data.Creation, 10))...)
	str = append(str, []byte(data.Name)...)
	hash := sha256.Sum256(str)
	//hash64 := b64.StdEncoding.EncodeToString(hash[:])

	if !bytes.Equal([]byte(data.ID), hash[:]) {
		//if(hash64 != data.ID) {
		fmt.Printf("sec3 \n")
		fmt.Printf("%v, %v", hash, data.ID)
		return false
	}
	/*the timestamp is reasonably recent with respect to the server’s clock,
	if data.Last_modified > time.Now().Unix() || data.Last_modified < time.Now().Unix()-MaxPropagationDelay {
		fmt.Printf("sec2")
		return false
	}*/

	//TODO any more checks to perform ?

	return true
}

func MeetingCreatedIsValid(data DataCreateMeeting, message Message) bool {
	//the last modified timestamp is equal to the creation timestamp,
	/*if data.Creation != data.Last_modified {
		return false
	}*/
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation-time.Now().Unix() > MaxPropagationDelay {
		return false
	}

	//we start after the creation and we end after the start
	if data.Start < data.Creation || data.End < data.Start {
		return false
	}
	//need to meet some	where
	if data.Location == "" {
		return false
	}
	return true
}

func PollCreatedIsValid(data DataCreatePoll, message Message) bool {
	return true
}

func RollCallCreatedIsValid(data DataCreateRollCall, message Message) bool {
	return true
}

func MessageIsValid(msg Message) error {
	// the message_id is valid
	str := []byte(msg.Data)
	str = append(str, []byte(msg.Signature)...)
	hash := sha256.Sum256(str)

	if !bytes.Equal([]byte(msg.Message_id), hash[:]) {
		return ErrInvalidResource
	}

	// the signature is valid
	err := VerifySignature(msg.Sender, (msg.Data), msg.Signature)
	if err != nil {
		return err //err
	}

	// the witness signatures are valid (check on every message??)
	publicKeys, err := AnalyseData(string(msg.Data))
	if publicKeys["object"] == "lao" && publicKeys["action"] == "create" {
		data, err := AnalyseDataCreateLAO(msg.Data)
		if err != nil {
			return ErrInvalidResource
		}
		print("Hello in public")
		// the signature of witnesses are valid
		err = VerifyWitnessSignatures(data.Witnesses, msg.WitnessSignatures, msg.Sender)
		if err != nil {
			return err //err
		}
	}
	return nil
}

/*
	we check that Sign(sender||data) is the given signature
*/
func VerifySignature(publicKey string, data []byte, signature string) error {
	//check the size of the key as it will panic if we plug it in Verify
	if len(publicKey) != ed.PublicKeySize {
		return ErrRequestDataInvalid
	}

	//data is in base64 so we need to decrypt it before using it
	//d :=  strings.Replace(data, "\n", "", -1)
	//dataDecoded,err := Decode(string(data))
	//if err!=nil{
	//		return ErrEncodingFault
	//}
	//hash := sha256.Sum256(data)

	if ed.Verify([]byte(publicKey), data, []byte(signature)) {
		return nil
	}
	//invalid signature
	return ErrRequestDataInvalid
}

/*
	we check that Sign(sender||data) is the given signature
*/
func VerifyWitnessSignature(publicKey string, data []byte, signature string) error {
	//check the size of the key as it will panic if we plug it in Verify
	if len(publicKey) != ed.PublicKeySize {
		return ErrRequestDataInvalid
	}

	if ed.Verify([]byte(publicKey), data, []byte(signature)) {
		return nil
	}
	//invalid signature
	return ErrRequestDataInvalid
}

/*
	handling of dynamic updates with object as item and not just string
	*publicKeys is already decoded
    *sender and signature are not already decoded
*/
func VerifyWitnessSignatures(publicKeys []string, witnessSignaturesEnc []string, sender string) error {
	senderDecoded, err := Decode(sender)
	if err != nil {
		return ErrEncodingFault
	}
	//TODO do we only check the pairs in witnessSignaturesEnc (1) or do need to verify that the publicKey
	// of the pair is in the publicKeys before (2)?
	for i := 0; i < len(witnessSignaturesEnc); i++ {
		witnessSignatures, err := AnalyseWitnessSignatures(witnessSignaturesEnc[i])
		if err != nil {
			return err
		}
		//right now we apply the first option and publickeys is then usless here
		err = VerifyWitnessSignature(witnessSignatures.Witness, senderDecoded, witnessSignatures.Signature)
		if err != nil {
			return err
		}
	}
	return nil
}

func LAOStateIsValid(data DataCreateLAO, message Message) bool {
	//the last modified timestamp is bigger than the creation timestamp,
	/*if data.Creation > data.Last_modified {
		fmt.Printf("creation time : %v, update time : %v . Creation cannot be bigger than update", data, data.Last_modified)
		return false
	}
	the timestamp is reasonably recent with respect to the server’s clock,
	if data.Last_modified > time.Now().Unix() || data.Last_modified < time.Now().Unix()-MaxPropagationDelay {
		fmt.Printf("sec2")
		return false
	}*/

	//TODO any more checks to perform ?

	return true
}
