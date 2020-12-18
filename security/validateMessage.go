package security

import (
	"bytes"
	"crypto/sha256"
	"log"
	"strconv"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
	"time"
)

//TODO check with be-2 that we have the same requirements

/* used for both creation and state update */
func LAOIsValid(data message.DataCreateLAO, create bool) bool {
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation < time.Now().Unix()-MaxClockDifference || data.Creation > time.Now().Unix()+MaxPropagationDelay {
		log.Printf("timestamp invalid, either too old or in the future : %v", data.Creation)
		return false
	}
	//the attestation is valid,
	str := []byte(data.Organizer)
	str = append(str, []byte(strconv.FormatInt(data.Creation, 10))...)
	str = append(str, []byte(data.Name)...)
	hash := sha256.Sum256(str)

	if create && !bytes.Equal([]byte(data.ID), hash[:]) {
		log.Printf("expecting %v, got %v", hash, data.ID)
		return false
	}

	return true
}

func MeetingCreatedIsValid(data message.DataCreateMeeting, message message.Message) bool {
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
	return true
}

func PollCreatedIsValid(data message.DataCreatePoll, message message.Message) bool {
	return true
}

func RollCallCreatedIsValid(data message.DataCreateRollCall, message message.Message) bool {
	return true
}

func MessageIsValid(msg message.Message) error {
	// the message_id is valid
	str := []byte(msg.Data)
	str = append(str, []byte(msg.Signature)...)
	hash := sha256.Sum256(str)

	if !bytes.Equal([]byte(msg.MessageId), hash[:]) {
		log.Printf("id of message invalid")
		return lib.ErrInvalidResource
	}

	// the signature is valid
	err := VerifySignature(msg.Sender, msg.Data, msg.Signature)
	if err != nil {
		log.Printf("invalid message signature")
		return err
	}

	// the witness signatures are valid (check on every message??)
	data, err := parser.ParseData(string(msg.Data))
	if data["object"] == "lao" && data["action"] == "create" {
		data, err := parser.ParseDataCreateLAO(msg.Data)
		if err != nil {
			log.Printf("test 3")
			return lib.ErrInvalidResource
		}
		// the signature of witnesses are valid
		err = VerifyWitnessSignatures(data.Witnesses, msg.WitnessSignatures, msg.Sender)
		if err != nil {
			log.Printf("invalid witness signatures")
			return err
		}
	}
	return nil
}
