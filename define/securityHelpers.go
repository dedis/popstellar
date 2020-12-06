/* Functions to verify signature correctness and fields correctness. */

package define

import (
	"bytes"
	ed "crypto/ed25519"
	"crypto/sha256"
	"fmt"
	"strconv"
	"time"
)

const MaxTimeBetweenLAOCreationAndPublish = 600

func LAOCreatedIsValid(data DataCreateLAO, message Message) bool {
	//the last modified timestamp is equal to the creation timestamp,
	if data.Creation != data.Last_modified {
		fmt.Printf(" last update and creation timestamp not equal : %v, %v", data, data.Last_modified)
		return false
	}
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation < time.Now().Unix()-MaxTimeBetweenLAOCreationAndPublish {
		fmt.Printf("timestamp invalid, either too old or in the future : %v", data.Creation)
		return false
	}
	//the ID is valid,
	str := []byte(data.Organizer)
	str = append(str, []byte(strconv.FormatInt(data.Creation, 10))...)
	str = append(str, []byte(data.Name)...)
	hash := sha256.Sum256(str)
	//hash64 := b64.StdEncoding.EncodeToString(hash[:])

	if !bytes.Equal([]byte(data.ID), hash[:]) {
		fmt.Printf("invalid ID : expecting %v but got %v", hash, data.ID)
		return false
	}

	return true
}

func LAOStateIsValid(data DataCreateLAO, message Message) bool {
	//the last modified timestamp is bigger than the creation timestamp,
	if data.Creation > data.Last_modified {
		fmt.Printf("creation time : %v, update time : %v . Creation cannot be bigger than update", data, data.Last_modified)
		return false
	}
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Last_modified > time.Now().Unix() || data.Last_modified < time.Now().Unix()-MaxTimeBetweenLAOCreationAndPublish {
		fmt.Printf("sec2")
		return false
	}

	//TODO any more checks to perform ?

	return true
}

func MeetingCreatedIsValid(data DataCreateMeeting, message Message) bool {
	//the last modified timestamp is equal to the creation timestamp,
	if data.Creation != data.Last_modified {
		return false
	}
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation-time.Now().Unix() > MaxTimeBetweenLAOCreationAndPublish {
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

func MessageIsValid(msg Message) (bool, error) {
	// the message_id is valid
	str := []byte(msg.Data)
	str = append(str, []byte(msg.Signature)...)
	hash := sha256.Sum256(str)

	if !bytes.Equal([]byte(msg.Message_id), hash[:]) {
		return false, nil
	}

	// the signature is valid
	valid, err := VerifySignature(msg.Sender, msg.Data, msg.Signature)
	if err != nil {
		return false, err
	}

	// the witness signatures are valid (check on every message??)
	return valid, nil //VerifyWitnessSignatures()
}

/*
	we check that Sign(sender||data) is the given signature
*/
func VerifySignature(publicKey string, data []byte, signature string) (bool, error) {
	//check the size of the key as it will panic if we plug it in Verify
	if len(publicKey) != ed.PublicKeySize {
		return false, nil
	}
	//check the validity of the signature
	//TODO method is defined supposing args are encrypted
	//the key is in base64 so we need to decrypt it before using it
	keyDecoded, err := Decode(publicKey)
	if err != nil {
		return false, ErrEncodingFault
	}
	//data is also in base64 so we need to decrypt it before using it
	dataDecoded, err := Decode(string(data))
	if err != nil {
		return false, ErrEncodingFault
	}
	if ed.Verify(keyDecoded, dataDecoded, []byte(signature)) {
		return true, nil
	}
	//invalid signature
	return false, nil
}

//TODO be careful about the size and the order !
/*Maybe have a fixed size byte ?
To handle checks while the slice is in construction, the slice must have full space
from the beginning. We should check how to create fixed length arrays in go. And
instead of appending in witness_message, put them in the slot which matches the slot
of the witness id in witness[]

	Witness[1,2,3...]
	witnessSignature[_,_,_./.]
	WitnessSignatures[3,6,2,1]
*/
func VerifyWitnessSignatures(publicKeys []byte, signatures []byte, data string, sender string) (bool, error) {
	senderDecoded, err := Decode(sender)
	if err != nil {
		return false, ErrEncodingFault
	}
	dataDecoded, err := Decode(data)
	if err != nil {
		return false, ErrEncodingFault
	}
	toCheck := append(senderDecoded, dataDecoded...)
	for i := 0; i < len(signatures); i++ {
		valid, err := VerifySignature(string(publicKeys[i]), toCheck, string(signatures[i]))
		if err != nil || !valid {
			return false, err
		}
	}
	return true, nil
}
