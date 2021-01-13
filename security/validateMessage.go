package security

import (
	"bytes"
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
	creation := checkCreationTimeValidity(data.Creation)

	//check if id is correct  : SHA256(organizer||creation||name)
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, b64.StdEncoding.EncodeToString(data.Organizer), strconv.FormatInt(data.Creation, 10), data.Name)
	hash := HashOfItems(elementsToHashForDataId)
	if create && !bytes.Equal(data.ID, hash) {
		log.Printf("ID of createLAO invalid: %v should be: %v", string(data.ID), string(hash[:]))
		return false
	}

	//no default name
	name := checkStringNotEmpty(data.Name)
	return creation && name
}

//MeetingCreatedIsValid checks wether a meeting is valid when it is created. It checks if the ID is correctly computed,
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

	//check if id is correct  : SHA256('M'||lao_id||creation||name)
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "M", b64.StdEncoding.EncodeToString([]byte(laoId)), strconv.FormatInt(data.Creation, 10), data.Name)
	hash := HashOfItems(elementsToHashForDataId)
	if !bytes.Equal(data.ID, hash) {
		log.Printf("ID of createRollCall invalid: %v should be: %v", string(data.ID), string(hash[:]))
	}
	return creation && name
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

//checkRollCallId check if id is correct  : SHA256('R'||lao_id||creation||name)
func checkRollCallId(laoId string, creation int64, name string, id []byte) bool {
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", b64.StdEncoding.EncodeToString([]byte(laoId)), strconv.FormatInt(creation, 10), name)
	hash := HashOfItems(elementsToHashForDataId)
	if !bytes.Equal(id, hash) {
		log.Printf("ID of RollCall invalid: %v should be: %v", string(id), string(hash[:]))
		return false
	}
	return true
}

//RollCallOpenedIsValid tell if a Roll call is valid on opening or reopening
func RollCallOpenedIsValid(data message.DataOpenRollCall, laoId string, rollCallCreation int64, rollCallName string) bool {
	//we start after the creation and we end after the start
	if data.Start < rollCallCreation {
		log.Printf("timestamps not logic.Start before creation.")
		return false
	}
	return checkRollCallId(laoId, rollCallCreation, rollCallName, data.ID)
}

//RollCallClosedIsValid tell if a rollCall timestamps make sense
func RollCallClosedIsValid(data message.DataCloseRollCall, laoId string, rollCallCreation int64, rollCallName string) bool {
	//we start after the creation and we end after the start
	if data.Start < rollCallCreation || data.End < data.Start {
		log.Printf("timestamps not logic.Start before creation.")
		return false
	}
	return checkRollCallId(laoId, rollCallCreation, rollCallName, data.ID)
}

// MessageIsValid checks upon reception that the message data is valid, that is that the ID is correctly computed, and
// that the signature is correct as well
// IMPORTANT thing : 	For every message the signature is Sign(message_id)
//						EXCEPT for (the data) witnessMessage which is Sign(data)
func MessageIsValid(msg message.Message) error {
	// check message_id is valid
	var itemsToHashForMessageId []string
	itemsToHashForMessageId = append(itemsToHashForMessageId, b64.StdEncoding.EncodeToString(msg.Data), b64.StdEncoding.EncodeToString(msg.Signature))
	hash := HashOfItems(itemsToHashForMessageId)

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
	/* TODO see comment above handleLAOState in organizer.go
			case "lao":
			switch data["action"] {
			case "state":
				data, err := parser.ParseDataCreateLAO(msg.Data)
				if err != nil {
					log.Printf("test 3")
					return lib.ErrInvalidResource
				}
				// the signatures (of MESSAGEID) of witnesses are valid
	TODO dans tous les cas, we don't have access to the message id of the lao, here we
				put the message_id of the message state lao (this makes no sens).

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
			// the signature of message_id of the message to witness is valid
			// this is the message_id of the data layer (!)
			err = VerifySignature(msg.Sender, data.MessageId, data.Signature)
			if err != nil {
				log.Printf("invalid message signature")
				return err
			}
		}
	}
	return nil
}

// checkCreationTimeValidity checks wether the int given as argument is between the server's current time
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
func checkStringNotEmpty(loc string) bool {
	if loc == "" {
		log.Printf("string argument cannot be empty")
		return false
	}
	return true
}
