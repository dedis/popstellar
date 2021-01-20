package security

import (
	"bytes"
	b64 "encoding/base64"
	"log"
	"strconv"
	"student20_pop/event"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
	"time"
)

// LAOIsValid checks whether the infos given upon creation or update of a LAO are valid. That is it checks the timestamp
// and if it's a creation it verifies that the ID is the right one.
func LAOIsValid(data message.DataCreateLAO, create bool) bool {
	//the timestamp is reasonably recent with respect to the server’s clock,
	creation := checkCreationTimeValidity(data.Creation)
	//name cannot be empty
	name := checkStringNotEmpty(data.Name)
	//ID is valid if creation
	id := !create || checkLaoId(data.Organizer, data.Creation, data.Name, data.ID)

	return creation && name && id
}

//MeetingCreatedIsValid checks whether a meeting is valid when it is created. It checks if the ID is correctly computed,
// and if the timestamps are coherent. (Start < End for example)
func MeetingCreatedIsValid(data message.DataCreateMeeting, laoId string) bool {
	//the timestamp is reasonably recent with respect to the server’s clock,
	creation := checkCreationTimeValidity(data.Creation)

	//we start after the creation and we end after the start
	if data.Start < data.Creation || (data.End != 0 && data.End < data.Start) {
		log.Printf("timestamps not logic. Either end is before start, or start before creation.")
		return false
	}
	//need to have a name not empty
	name := checkStringNotEmpty(data.Name)

	return creation && name && checkMeetingId(laoId, data.Creation, data.Name, data.ID)
}

// RollCallCreatedIsValid tell if a Roll call is valid on creation
func RollCallCreatedIsValid(data message.DataCreateRollCall, laoId string) bool {
	//the timestamp is reasonably recent with respect to the server’s clock,
	creation := checkCreationTimeValidity(data.Creation)

	//we receive either start either scheduled and the other is set to 0
	if data.Start != 0 && data.Scheduled != 0 {
		log.Printf("cannot have both start and scheduled set")
		return false
	} else if data.Start == 0 && data.Scheduled == 0 {
		log.Printf("at least one of Data and Schedule has to be set")
		return false
	}

	if data.Scheduled == 0 {
		//we start after the creation and we end after the start
		if data.Start < data.Creation {
			log.Printf("timestamps not logic. Start cannot be before creation.")
			return false
		}
	} else if data.Start == 0 {
		//we start after the creation and we end after the start
		if data.Scheduled < data.Creation {
			log.Printf("timestamps not logic. Scheduled cannot be before creation.")
			return false
		}
	}

	//need to meet some	where
	location := checkStringNotEmpty(data.Location)
	//name cannot be empty
	name := checkStringNotEmpty(data.Name)

	return name && creation && location && checkRollCallId(laoId, data.Creation, data.Name, data.ID)
}

//checkID compares SHA256(firstChar || laoId || creation || name) to id, returns true if they are the same
// for a LAO use the laoId as "organizer" field. uses HashItems to concatenate and hash the values
func checkID(firstChar string, laoId []byte, creation int64, name string, id []byte) bool {
	var elements []string
	if firstChar != "" {
		elements = append(elements, firstChar)
	}
	elements = append(elements, b64.StdEncoding.EncodeToString(laoId), strconv.FormatInt(creation, 10), name)
	hash := HashItems(elements)
	if !bytes.Equal(id, hash) {
		log.Printf("ID invalid: %v should be: %v", string(id), string(hash[:]))
		return false
	}
	return true
}

//checkRollCallId check if id is correct: SHA256('R'||lao_id||creation||name)
func checkRollCallId(laoId string, creation int64, name string, id []byte) bool {
	return checkID("R", []byte(laoId), creation, name, id)
}

//checkMeetingId check if id is correct: SHA256('M'||lao_id||creation||name)
func checkMeetingId(laoId string, creation int64, name string, id []byte) bool {
	return checkID("M", []byte(laoId), creation, name, id)
}

//check if id is correct: SHA256(organizer||creation||name)
func checkLaoId(organizer []byte, creation int64, name string, id []byte) bool {
	return checkID("", organizer, creation, name, id)
}

//RollCallOpenedIsValid tell if a Roll call is valid on opening or reopening
func RollCallOpenedIsValid(data message.DataOpenRollCall, laoId string, rollCall event.RollCall) bool {
	//we start after the creation and we end after the start
	if data.Start < rollCall.Creation {
		log.Printf("timestamps not logic.Start before creation.")
		return false
	}
	return checkRollCallId(laoId, rollCall.Creation, rollCall.Name, data.ID)
}

//RollCallClosedIsValid tell if a rollCall timestamps makes sense and verifies its ID
func RollCallClosedIsValid(data message.DataCloseRollCall, laoId string, rollCall event.RollCall) bool {
	//we start after the creation and we end after the start
	if data.Start < rollCall.Creation || data.End < data.Start {
		log.Printf("timestamps not logic.Start before creation.")
		return false
	}

	if data.End > time.Now().Unix()+MaxClockDifference {
		log.Printf("timestamps not logic. End too far in the future. Roll call not closed")
		return false
	}
	return checkRollCallId(laoId, rollCall.Creation, rollCall.Name, data.ID)
}

// MessageIsValid checks upon reception that the message data is valid, that is that the ID is correctly computed, and
// that the signature is correct as well
// IMPORTANT :
// * For every message the signature is Sign(data)
// * but for the one in DATAWitnessMessage it is Sign(message_id)
func MessageIsValid(msg message.Message) error {
	// check message_id is valid
	var itemsToHashForMessageId []string
	itemsToHashForMessageId = append(itemsToHashForMessageId, b64.StdEncoding.EncodeToString(msg.Data), b64.StdEncoding.EncodeToString(msg.Signature))
	hash := HashItems(itemsToHashForMessageId)

	if !bytes.Equal(msg.MessageId, hash) {
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
	/*  see comment above handleLAOState in organizer.go
	case "lao":
	switch data["action"] {
	case "state":
		data, err := parser.ParseDataCreateLAO(msg.Data)
		if err != nil {
			log.Printf("could not parse dataCreateLAO")
			return lib.ErrInvalidResource
		}
		// the signatures (of message_id) of witnesses are valid


		err = VerifyWitnessSignatures(data.Witnesses, msg.WitnessSignatures, msg.MessageId)
		if err != nil {
			log.Printf("invalid signatures in witness message")
			return err
		}
	}

	*/
	case "message":
		switch data["action"] {
		case "witness":
			data, err := parser.ParseDataWitnessMessage(msg.Data)
			if err != nil {
				log.Printf("unable to parse the dataWitnessMessage correctlty ")
				return lib.ErrInvalidResource
			}
			// signature of message_id of the message to witness
			err = VerifySignature(msg.Sender, data.MessageId, data.Signature)
			// this is the message_id of the data layer (!)
			if err != nil {
				log.Printf("invalid message signature")
				return err
			}
		}
	}
	return nil
}

// checkCreationTimeValidity checks whether the int given as argument is between the server's current time
// + MaxPropagationDelay and the server's current time - MaxClockDifference. If it's not, it logs an error message
func checkCreationTimeValidity(ctime int64) bool {
	//the timestamp is reasonably recent with respect to the server’s clock,
	if ctime < time.Now().Unix()-MaxClockDifference || ctime > time.Now().Unix()+MaxPropagationDelay {
		log.Printf("timestamp unvalid : got %d but need to be between %d and %d",
			ctime, time.Now().Unix()-MaxClockDifference, time.Now().Unix()+MaxPropagationDelay)
		return false
	}
	return true
}

//checkStringNotEmpty returns false if the string given as param is empty, and logs an error message
func checkStringNotEmpty(str string) bool {
	if str == "" {
		log.Printf("string argument cannot be empty")
		return false
	}
	return true
}
