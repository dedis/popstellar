package security

import (
	"bytes"
	"crypto/sha256"
	b64 "encoding/base64"
	"log"
	"strconv"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
	"time"
)

// LAOIsValid checks wether the infos given upon creation or update of a LAO are valid. That is it checks the timestamp
// and if it's a creation it verifies that the ID is the right one.
func LAOIsValid(data message.DataCreateLAO, create bool) bool {
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation < time.Now().Unix()-MaxClockDifference || data.Creation > time.Now().Unix()+MaxPropagationDelay {
		log.Printf("timestamp invalid, either too old or in the future : %v", data.Creation)
		return false
	}
	//the attestation is valid,
	str := data.Organizer
	str = append(str, []byte(strconv.FormatInt(data.Creation, 10))...)
	str = append(str, []byte(data.Name)...)
	hash := sha256.Sum256(str)

	if create && !bytes.Equal(data.ID, hash[:]) {
		log.Printf("expecting %v, got %v", hash, data.ID)
		return false
	}
	//check if id is correct  : SHA256(organizer||creation||name)
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, string(data.Organizer), strconv.FormatInt(data.Creation, 10), data.Name)
	hash = sha256.Sum256([]byte(lib.ComputeAsJsonArray(elementsToHashForDataId)))
	if !bytes.Equal(data.ID, hash[:]) {
		log.Printf("ID of createLAO invalid: %v should be: %v", string(data.ID), string(hash[:]))
	}

	return true
}

//MeetingCreatedIsValid checks wether a meeting is valid when it is created. It checks if the ID is correctly computed,
// and if the timestamps are coherent. (Start < End for example)
func MeetingCreatedIsValid(data message.DataCreateMeeting, laoId string) bool {
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation < time.Now().Unix()-MaxClockDifference || data.Creation > time.Now().Unix()+MaxPropagationDelay {
		log.Printf("timestamp unvalid : got %d but need to be between %d and %d",
			data.Creation, time.Now().Unix()-MaxClockDifference, time.Now().Unix()+MaxPropagationDelay)
		return false
	}

	//we start after the creation and we end after the start
	if data.Start < data.Creation || data.End < data.Start {
		log.Printf("timestamps not logic. Either end is before start, or start before creation.")
		return false
	}
	//need to meet some	where
	if data.Location == "" {
		log.Printf("location can not be empty")
		return false
	}
	//check if id is correct  : SHA256(lao_id||creation||name)
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, laoId, strconv.FormatInt(data.Creation, 10), data.Name)
	hash := sha256.Sum256([]byte(lib.ComputeAsJsonArray(elementsToHashForDataId)))
	if !bytes.Equal(data.ID, hash[:]) {
		log.Printf("ID od createRollCall invalid: %v should be: %v", string(data.ID), string(hash[:]))
	}
	return true
}

// not implemented yet
func PollCreatedIsValid(data message.DataCreatePoll, message message.Message) bool {
	return true
}

// not implemented yet
func RollCallCreatedIsValid(data message.DataCreateRollCallNow, laoId string) bool {
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation < time.Now().Unix()-MaxClockDifference || data.Creation > time.Now().Unix()+MaxPropagationDelay {
		log.Printf("timestamp unvalid : got %d but need to be between %d and %d",
			data.Creation, time.Now().Unix()-MaxClockDifference, time.Now().Unix()+MaxPropagationDelay)
		return false
	}
	//we start after the creation and we end after the start
	if data.Start < data.Creation {
		log.Printf("timestamps not logic.Start before creation.")
		return false
	}
	//check if id is correct  : SHA256('R'||lao_id||creation||name)
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", laoId, strconv.FormatInt(data.Creation, 10), data.Name)
	hash := sha256.Sum256([]byte(lib.ComputeAsJsonArray(elementsToHashForDataId)))
	if !bytes.Equal(data.ID, hash[:]) {
		log.Printf("ID od createRollCall invalid: %v should be: %v", string(data.ID), string(hash[:]))
	}
	return true
}

// MessageIsValid checks upon reception that the message data is valid, that is that the ID is correctly computed, and
// that the signature is correct as well
// IMPORTANT thing : 	For every message the signature is Sign(message_id)
//						EXCEPT for (the data) witnessMessage which is Sign(data)
func MessageIsValid(msg message.Message) error {
	// check message_id is valid
	var itemsToHashForMessageId []string
	itemsToHashForMessageId = append(itemsToHashForMessageId, b64.StdEncoding.EncodeToString(msg.Data), string(msg.Signature))
	hash := sha256.Sum256([]byte(lib.ComputeAsJsonArray(itemsToHashForMessageId)))

	if !bytes.Equal(msg.MessageId, hash[:]) {
		log.Printf("Id of message invalid: %v should be: %v", string(msg.MessageId), string(hash[:]))
		return lib.ErrInvalidResource
	}

	// the signature of data is valid (we are in the "MESSAGE layer")
	err := VerifySignature(msg.Sender, msg.Data, msg.Signature)
	if err != nil {
		log.Printf("invalid message signature")
		return err
	}

	// the witness signatures are valid (check on every message??)
	data, err := parser.ParseData(string(msg.Data))
	if err != nil {
		log.Printf("unable to parse the message data")
		return err
	}
	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "state":
			data, err := parser.ParseDataCreateLAO(msg.Data)
			if err != nil {
				log.Printf("test 3")
				return lib.ErrInvalidResource
			}
			// the signatures (of MESSAGEID) of witnesses are valid
			err = VerifyWitnessSignatures(data.Witnesses, msg.WitnessSignatures, msg.MessageId)
			if err != nil {
				log.Printf("invalid signatures in witness message")
				return err
			}
		}
	case "message":
		switch data["action"] {
		case "witness":
			data, err := parser.ParseDataWitnessMessage(msg.Data)
			if err != nil {
				log.Printf("test 3")
				return lib.ErrInvalidResource
			}
			// the signature of DATA is valid (we are in the "DATA layer")
			err = VerifySignature(msg.Sender, msg.Data, data.Signature)
			if err != nil {
				log.Printf("invalid message signature")
				return err
			}
		}
	}
	return nil
}
