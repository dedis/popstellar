package define

import (
	"bytes"
	"crypto/sha256"
	ed "crypto/ed25519"
	"strconv"
	"time"
)

const MaxTimeBetweenLAOCreationAndPublish = 600

// TODO if we use the json Schema, don't need to check structure correctness
func LAOCreatedIsValid(data DataCreateLAO, message Message) error {
	//the last modified timestamp is equal to the creation timestamp,
	if data.Creation != data.LastModified {
		return ErrInvalidResource
	}
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation-time.Now().Unix() > MaxTimeBetweenLAOCreationAndPublish {
		return ErrInvalidResource
	}
	//the attestation is valid,
	str := []byte(data.OrganizerPKey)
	str = append(str, []byte(strconv.FormatInt(data.Creation, 10))...)
	str = append(str, []byte(data.Name)...)
	hash := sha256.Sum256(str)
	if !bytes.Equal([]byte(message.Message_id), hash[:]) {
		return ErrInvalidResource
	}

	return nil
}

func MeetingCreatedIsValid(data DataCreateMeeting, message Message) error {
	//the last modified timestamp is equal to the creation timestamp,
	if data.Creation != data.LastModified {
		return ErrInvalidResource
	}
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation-time.Now().Unix() > MaxTimeBetweenLAOCreationAndPublish {
		return ErrInvalidResource
	}

	//we start after the creation and we end after the start
	if data.Start < data.Creation || data.End < data.Start {
		return ErrInvalidResource
	}
	//need to meet some	where
	if data.Location == "" {
		return ErrInvalidResource
	}
	return nil
}
func PollCreatedIsValid(data DataCreatePoll, message Message) error {
	return nil
}
func RollCallCreatedIsValid(data DataCreateRollCall, message Message) error {
	return nil
}

func MessageIsValid(msg Message) error {
	return nil
}

func VerifySignature(publicKey string, data string,signature string ) error{
	//check the size of the key as it will panic if we plug it in Verify
	if len(publicKey) != ed.PublicKeySize{
		return ErrRequestDataInvalid
	}
	//check the validity of the signature
	//TODO prone to modification depending on base64 encoding
	if ed.Verify([]byte(publicKey), []byte(data), []byte(signature)){
		return nil
	}
	//invalid signature
	return ErrRequestDataInvalid
}

//TODO be careful about the size and the order !
/*Maybe have a fixed size byte ?
To handle checks while the slice is in construction, the slice must have full space
from the beginning. We should check how to create fixed length arrays in go. And
instead of appending in witness_message, put them in the slot which matches the slot
of the witness id in witness[]

	Witness[1,2,3...]
	witnessSignature[_,_,_./.]
*/
func VerifyWitnessSignatures(publicKeys []byte, signatures []byte,data string,sender string,signature string ) error {
	toCheck := sender + data
	for i := 0; i < len(signatures); i++ {
		err := VerifySignature(string (publicKeys[i]), toCheck ,string (signatures[i]))
		if err!= nil{
			return err
		}
	}
	return nil
}