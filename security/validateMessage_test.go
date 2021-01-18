// define/securityHelpers
package security

import (
	ed "crypto/ed25519"
	b64 "encoding/base64"
	"encoding/json"
	"errors"
	"io/ioutil"
	"log"
	"strconv"
	"student20_pop/event"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
	"testing"
	"time"
)

// we don't check the switch in messageIsValid

//TestMessageIsValidWithoutWitnesses checks that a message containing a createLao is valid at both message and data layer
func TestMessageIsValidWithoutWitnesses(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	//increase nb of tests
	for i := 0; i < 100; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		var witnessSignatures []message.ItemWitnessSignatures
		var witnessKeys [][]byte
		var creation = time.Now().Unix()
		name := "My LAO"
		data, err := createLAO(publicKey, privateKey, witnessKeys, creation, name)
		if err != nil {
			t.Error(err)
		}
		valid := LAOIsValid(data, true)
		if valid != true {
			t.Errorf("Created Lao Should be valid %#v", data)
		}
		err = checkMessageIsValid(publicKey, privateKey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestDataWitnessMessageIsValid checks that a witness message is valid
func TestDataWitnessMessageIsValid(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	//increase nb of tests
	for i := 0; i < 100; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		var witnessSignatures []message.ItemWitnessSignatures
		messageIdToWitness := []byte("finally something to sign")
		data, err := createWitnessMessage(privateKey, messageIdToWitness)
		if err != nil {
			t.Error(err)
		}
		err = checkMessageIsValid(publicKey, privateKey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestRollCallOpenedIsValid checks that a message containing a openRollCall is valid at both message and data layer
func TestRollCallOpenedIsValid(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	//increase nb of tests
	for i := 0; i < 100; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		var witnessSignatures []message.ItemWitnessSignatures
		rollCallCreation := time.Now().Unix()
		start := rollCallCreation + 1
		rollCallName := "encore un roll call"
		laoId := []byte("12345")

		data, err := createOpenRollCall(publicKey, privateKey, rollCallCreation, start, rollCallName, laoId)
		if err != nil {
			t.Error(err)
		}
		rollCall, err := createRollCallEvent(publicKey, privateKey, rollCallCreation, start, rollCallName, laoId)
		if err != nil {
			t.Error(err)
		}
		valid := RollCallOpenedIsValid(data, string(laoId), rollCall)
		if valid != true {
			t.Errorf("Created rollcall Should be valid %#v", data)
		}
		err = checkMessageIsValid(publicKey, privateKey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestRollCallClosedIsValid checks that a message containing a createRollCall is valid at both message and data layer
func TestRollCallClosedIsValid(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	//increase nb of tests
	for i := 0; i < 100; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		var witnessSignatures []message.ItemWitnessSignatures
		var attendeesPks [][]byte

		rollCallCreation := time.Now().Unix()
		rollCallName := "encore un roll call"
		start := rollCallCreation + 1
		end := start + 1
		laoId := []byte("12345")
		data, err := createCloseRollCallNow(publicKey, privateKey, rollCallCreation, start, end, rollCallName, laoId, attendeesPks)
		if err != nil {
			t.Error(err)
		}
		rollCall, err := createRollCallEvent(publicKey, privateKey, rollCallCreation, start, rollCallName, laoId)
		if err != nil {
			t.Error(err)
		}
		valid := RollCallClosedIsValid(data, string(laoId), rollCall)
		if valid != true {
			t.Errorf("closed rollcall Should be valid %#v", data)
		}
		err = checkMessageIsValid(publicKey, privateKey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestRollCallCreatedIsValid checks that a message containing a createRollCall is valid at both message and data layer
func TestRollCallCreatedIsValid(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	//increase nb of tests
	for i := 0; i < 100; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		var witnessSignatures []message.ItemWitnessSignatures
		var creation = time.Now().Unix()
		start := creation + 1
		name := "RollCallNow"
		laoId := []byte("12345")
		data, err := createRollCallNow(publicKey, privateKey, creation, start, name, laoId)
		if err != nil {
			t.Error(err)
		}
		valid := RollCallCreatedIsValid(data, string(laoId))
		if valid != true {
			t.Errorf("Created rollcall Should be valid %#v", data)
		}
		err = checkMessageIsValid(publicKey, privateKey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestMeetingCreatedIsValid checks that a message containing a createMeeting is valid at both message and data layer
func TestMeetingCreatedIsValid(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	//increase nb of tests
	for i := 0; i < 100; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		var witnessSignatures []message.ItemWitnessSignatures
		var creation = time.Now().Unix()
		start := creation + 1
		end := creation + 1
		name := "someMeetintintinitin"
		laoId := []byte("12345")

		data, err := createMeeting(publicKey, privateKey, creation, start, end, name, laoId)
		if err != nil {
			t.Error(err)
		}
		valid := MeetingCreatedIsValid(data, string(laoId))
		if valid != true {
			t.Errorf("Created Meeting Should be valid %#v", data)
		}
		err = checkMessageIsValid(publicKey, privateKey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//================== invalid Tests ========================//

//TestBadDataWitnessMessage checks that a bad witness message is invalid
func TestBadDataWitnessMessage(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	publicKey, privateKey := lib.GenerateTestKeyPair()
	var witnessSignatures []message.ItemWitnessSignatures
	messageIdToWitness := []byte("enfin un truc à signer")
	data, err := createWitnessMessage(privateKey, messageIdToWitness)
	if err != nil {
		t.Error(err)
	}
	data.Signature = []byte("oulala la mauvaise signature")
	err = checkMessageIsValid(publicKey, privateKey, data, witnessSignatures)
	if err == nil {
		t.Errorf("Didn't detect Witness message with invalid signaure %#v", data)
	}
	//==================================================================//
	//Here we test when the message signature is invalid
	msg, err := makeMessage(publicKey, privateKey, data, witnessSignatures)
	if err != nil {
		t.Error(err)
	}

	msg.Signature = []byte("oulala la mauvaise signature")

	messageFlat, err := json.Marshal(msg)
	if err != nil {
		t.Error(err)
	}
	messProcessed, err := parser.ParseMessage(messageFlat)
	if err != nil {
		t.Error(err)
	}

	err = MessageIsValid(messProcessed)
	if err == nil {
		t.Errorf("didn't detect that message's signature is invalid %#v", data)
	}
	//==================================================================//
	//Here we test when the message id is invalid
	msg, err = makeMessage(publicKey, privateKey, data, witnessSignatures)
	if err != nil {
		t.Error(err)
	}

	msg.MessageId = []byte("bad identititittity")

	messageFlat, err = json.Marshal(msg)
	if err != nil {
		t.Error(err)
	}
	messProcessed, err = parser.ParseMessage(messageFlat)
	if err != nil {
		t.Error(err)
	}

	err = MessageIsValid(messProcessed)
	if err == nil {
		t.Errorf("didn't detect that message's message_id is invalid %#v", data)
	}
	//==================================================================//
	//Here we test when the message id is invalid
	msg, err = makeMessage(publicKey, privateKey, data, witnessSignatures)
	if err != nil {
		t.Error(err)
	}

	msg.Data = []byte("problem: \", \\} bad data, not parsable")

	messageFlat, err = json.Marshal(msg)
	if err != nil {
		t.Error(err)
	}
	messProcessed, err = parser.ParseMessage(messageFlat)
	if err != nil {
		t.Error(err)
	}

	err = MessageIsValid(messProcessed)
	if err == nil {
		t.Errorf("didn't detect that message's data is imparsable %#v", data)
	}
}

//TestRollCallClosedInvalid checks that a message containing a closeRollCall with bad timestamps is invalid
func TestRollCallClosedInvalid(t *testing.T) {
	publicKey, privateKey := lib.GenerateTestKeyPair()
	var attendeesPks [][]byte

	rollCallCreation := time.Now().Unix()
	rollCallName := "encore un roll call"
	start := rollCallCreation + 4
	end := start - 2
	laoId := []byte("12345")
	data, err := createCloseRollCallNow(publicKey, privateKey, rollCallCreation, start, end, rollCallName, laoId, attendeesPks)
	if err != nil {
		t.Error(err)
	}
	rollCall, err := createRollCallEvent(publicKey, privateKey, rollCallCreation, start, rollCallName, laoId)
	if err != nil {
		t.Error(err)
	}
	valid := RollCallClosedIsValid(data, string(laoId), rollCall)
	if valid {
		t.Errorf("didn't detect that closed rollcall is invalid %#v", data)
	}
}

//TestOpenRollCallBadFields checks that a message containing a openRollCall with invalid start is invalid at data layer
func TestOpenRollCallBadFields(t *testing.T) {
	for i := 0; i < 2; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		rollCallCreation := time.Now().Unix()
		start := rollCallCreation - 2
		rollCallName := "encore un roll call"
		laoId := []byte("12345")

		data, err := createOpenRollCall(publicKey, privateKey, rollCallCreation, start, rollCallName, laoId)
		if err != nil {
			t.Error(err)
		}
		rollCall, err := createRollCallEvent(publicKey, privateKey, rollCallCreation, start, rollCallName, laoId)
		if err != nil {
			t.Error(err)
		}
		valid := RollCallOpenedIsValid(data, string(laoId), rollCall)
		if valid {
			t.Errorf("Opened rollcall Should be invalid, start before creation %#v", data)
		}
	}
}

//TestRollCallCreatedBadFields verifies that roll call is invalidated due to incorrect id,timestamps,
//and that the message is invalidated due to incorrect private key
func TestRollCallCreatedBadFields(t *testing.T) {
	for i := 0; i < 2; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		var witnessSignatures []message.ItemWitnessSignatures
		var creation = time.Now().Unix()
		start := creation + 1
		name := "RollCallNow"
		laoId := []byte("12345")
		data, err := createRollCallNow(publicKey, privateKey, creation, start, name, laoId)
		if err != nil {
			t.Error(err)
		}
		notLaoId := []byte("6789")
		valid := RollCallCreatedIsValid(data, string(notLaoId))
		if valid {
			t.Errorf("Created Rollcall Should be invalid beacause of invalid id %#v", data)
		}
		//===========================================================//
		creationBis := time.Now().Unix() + time.Now().Unix() + MaxPropagationDelay + 1
		if i%2 == 0 {
			creationBis = time.Now().Unix() - MaxClockDifference - 1
		}
		data.Creation = creationBis
		valid = RollCallCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to bad creation %#v", data)
		}
		//===========================================================//
		data.Start = 2
		data.Scheduled = 3
		valid = RollCallCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have start "+
				"& scheduled strictly positive at the same time) %#v", data)
		}
		//===========================================================//
		data.Start = 0
		data.Scheduled = 0
		valid = RollCallCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have start "+
				"& scheduled equal 0 at the same time) %#v", data)
		}
		//===========================================================//
		data.Start = data.Creation - 5
		data.Scheduled = 0
		valid = RollCallCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have start "+
				"less than creation & scheduled equal 0 ) %#v", data)
		}
		//===========================================================//
		data.Scheduled = data.Creation - 5
		data.Start = 0
		valid = RollCallCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have scheduled "+
				"less than creation & start equal 0 ) %#v", data)
		}
		//===========================================================//
		data.Scheduled = data.Creation - 5
		data.Start = 0
		valid = RollCallCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have scheduled "+
				"less than creation & start equal 0 ) %#v", data)
		}
		//verify that the message is invalidated due to incorrect private key
		_, badPrivateKey := lib.GenerateTestKeyPair()
		err = checkMessageIsValid(publicKey, badPrivateKey, data, witnessSignatures)
		if err == nil {
			t.Errorf("The Message Should be invalid beacause of incorrect Key %#v", data)
		}
	}
}

//TestMeetingBadFields verifies that the meeting is invalid due to invalid timestamps,name and id
func TestMeetingBadFields(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 2; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		var witnessSignatures []message.ItemWitnessSignatures
		creation := time.Now().Unix() + time.Now().Unix() + MaxPropagationDelay + 1
		if i%2 == 0 {
			creation = time.Now().Unix() - MaxClockDifference - 1
		}
		start := creation + 1
		end := creation + 1
		name := "someMeeting"
		laoId := []byte("12345")

		data, err := createMeeting(publicKey, privateKey, creation, start, end, name, laoId)
		if err != nil {
			t.Error(err)
		}
		valid := MeetingCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to bad creation %#v", data)
		}
		//==================================================================//
		creation = time.Now().Unix()
		data.Start = creation + 2*MaxPropagationDelay
		valid = MeetingCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to bad start %#v", data)
		}
		//==================================================================//
		creation = time.Now().Unix()
		data.Start = creation - 2*MaxPropagationDelay
		valid = MeetingCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to bad timestamp: "+
				"Either end is before start, or start before creation. %#v", data)
		}
		//data.Start < data.Creation || (data.End != 0 && data.End < data.Start) {
		//==================================================================//
		data.End = data.Start - 10
		valid = MeetingCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to bad end %#v", data)
		}
		//==================================================================//
		data.Creation = time.Now().Unix()
		data.Start = creation + 2
		data.End = creation + 2
		data.Name = ""
		valid = MeetingCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to empty name %#v", data)
		}
		//==================================================================//
		data.ID = []byte("wo shi fa guo ren")
		valid = MeetingCreatedIsValid(data, string(laoId))
		if valid {
			t.Errorf("Created Meeting Should be invalid due to bad id %#v", data)
		}
		err = checkMessageIsValid(publicKey, privateKey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestLAOInvalidName verifies that the Lao is invalid due to empty name
func TestLAOInvalidName(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	publicKey, privateKey := lib.GenerateTestKeyPair()
	var witnessKeys [][]byte
	var creation = time.Now().Unix()
	// should be not empty but this is not specified in the protocol specifications ?
	name := ""
	data, err := createLAO(publicKey, privateKey, witnessKeys, creation, name)
	if err != nil {
		t.Error(err)
	}
	valid := LAOIsValid(data, true)
	if valid {
		t.Errorf("Created Lao Should be invalid due to empty location %#v", data)
	}
}

//TestLAOInvalidId verifies that the Lao is invalid due to invalid id
func TestLAOInvalidId(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	publicKey, privateKey := lib.GenerateTestKeyPair()
	var witnessKeys [][]byte
	var creation = time.Now().Unix()
	name := "hello"
	data, err := createLAO(publicKey, privateKey, witnessKeys, creation, name)
	if err != nil {
		t.Error(err)
	}
	data.ID = []byte("123456")
	valid := LAOIsValid(data, true)
	if valid {
		t.Errorf("Created Lao Should be invalid due to incorrect id %#v", data)
	}
}

//TestLAOIInvalidCreationTime verifies that the Lao is invalid due to invalid creation time
func TestLAOIInvalidCreationTime(t *testing.T) {
	// turn off logging for the tests
	log.SetFlags(0)
	log.SetOutput(ioutil.Discard)

	//increase nb of tests
	for i := 0; i < 100; i++ {
		publicKey, privateKey := lib.GenerateTestKeyPair()
		var witnessKeys [][]byte
		name := "ok"
		creation := time.Now().Unix() + time.Now().Unix() + MaxPropagationDelay + 1
		if i%2 == 0 {
			creation = time.Now().Unix() - MaxClockDifference - 1
		}
		data, err := createLAO(publicKey, privateKey, witnessKeys, creation, name)
		if err != nil {
			t.Error(err)
		}
		valid := LAOIsValid(data, true)
		if valid != false {
			t.Errorf("Created Lao Should be invalid due to wrong creation time %#v", data)
		}
	}
}

//================================ helper functions for the tests ===================================================//

func checkMessageIsValid(publicKey []byte, privateKey ed.PrivateKey, data interface{}, KeysAndSignatures []message.ItemWitnessSignatures) error {

	msg, err := makeMessage(publicKey, privateKey, data, KeysAndSignatures)
	if err != nil {
		return err
	}
	messageFlat, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	messProcessed, err := parser.ParseMessage(messageFlat)
	if err != nil {
		return err
	}
	err = MessageIsValid(messProcessed)
	if err != nil {
		return err
	}
	return nil
}

// makeMessage returns a valid message.Message with fields passed as arguments, and signs it using the privateKey
func makeMessage(publicKey []byte, privateKey ed.PrivateKey, data interface{}, keysAndSignatures []message.ItemWitnessSignatures) (msg message.Message, err error) {
	var dataFlat, signed, id []byte
	dataFlat, signed, id, err = computeMessageId(data, privateKey)
	if err != nil {
		return message.Message{}, err
	}

	//witness signatures
	ArrayOfWitnessSignatures, err := marshalSignatureArray(keysAndSignatures)
	if err != nil {
		return message.Message{}, err
	}
	msg = message.Message{
		Data:              dataFlat, // in base 64
		Sender:            publicKey,
		Signature:         signed,
		MessageId:         id,
		WitnessSignatures: ArrayOfWitnessSignatures,
	}
	return msg, nil
}

// marshalSignatureArray returns a []json.RawMessage where each element is a marshalled message.ItemWitnessSignatures of
// the passed argument
func marshalSignatureArray(keySignaturePairs []message.ItemWitnessSignatures) ([]json.RawMessage, error) {
	var signatures []json.RawMessage
	for i := 0; i < len(keySignaturePairs); i++ {
		pairString, err := json.Marshal(keySignaturePairs[i])
		if err != nil {
			return nil, errors.New("could not marshal key-Signature pair")
		}
		signatures = append(signatures, pairString)
	}
	return signatures, nil
}

// createMeeting returns a valid message.DataCreateMeeting with the parameters passed as arguments.
func createMeeting(publicKey []byte, privateKey ed.PrivateKey, creation int64, start int64, end int64, name string, laoId []byte) (message.DataCreateMeeting, error) {
	if (len(publicKey) != ed.PublicKeySize) || len(privateKey) != ed.PrivateKeySize {
		return message.DataCreateMeeting{}, errors.New("wrong argument -> size of public key don't respected ")
	}

	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "M", b64.StdEncoding.EncodeToString(laoId), strconv.FormatInt(creation, 10), name)
	idData := HashItems(elementsToHashForDataId)
	var data = message.DataCreateMeeting{
		Object:   "lao",
		Action:   "create",
		ID:       idData,
		Name:     name,
		Creation: creation,
		Location: "some location",
		Start:    start,
		End:      end,
		Extra:    "no no no",
	}
	return data, nil
}

// createWitnessMessage returns a valid message.DataWitnessMessage witnessing message with id messageId
func createWitnessMessage(privateKey ed.PrivateKey, messageId []byte) (message.DataWitnessMessage, error) {
	if len(privateKey) != ed.PrivateKeySize {
		return message.DataWitnessMessage{}, errors.New("wrong argument -> size of private key don't respected ")
	}
	signed := ed.Sign(privateKey, messageId)
	var data = message.DataWitnessMessage{
		Object:    "message",
		Action:    "witness",
		MessageId: messageId,
		Signature: signed,
	}
	return data, nil
}

// createLAO returns a valid message.DataCreateLAO with the given parameters
func createLAO(publicKey []byte, privateKey ed.PrivateKey, WitnessKeys [][]byte, creation int64, name string) (message.DataCreateLAO, error) {
	if (len(publicKey) != ed.PublicKeySize) || len(privateKey) != ed.PrivateKeySize {
		return message.DataCreateLAO{}, errors.New("bad public key size")
	}
	var itemsToHashForId []string
	itemsToHashForId = append(itemsToHashForId, b64.StdEncoding.EncodeToString(publicKey), strconv.FormatInt(creation, 10), name)
	idData := HashItems(itemsToHashForId)
	var data = message.DataCreateLAO{
		Object:    "lao",
		Action:    "create",
		ID:        idData,
		Name:      name,
		Creation:  creation,
		Organizer: publicKey,
		Witnesses: WitnessKeys,
	}
	return data, nil
}

// createRollCallEvent returns an event.RollCall with the given parameters
func createRollCallEvent(publicKey []byte, privateKey ed.PrivateKey, creation int64, start int64, name string, laoId []byte) (event.RollCall, error) {
	if (len(publicKey) != ed.PublicKeySize) || len(privateKey) != ed.PrivateKeySize {
		return event.RollCall{}, errors.New("bad public key size")
	}
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", b64.StdEncoding.EncodeToString(laoId), strconv.FormatInt(creation, 10), name)
	idData := HashItems(elementsToHashForDataId)
	var data = event.RollCall{
		ID:          string(idData),
		Name:        name,
		Creation:    creation,
		Location:    "pas loin",
		Start:       start,
		Description: "un roll call",
	}
	return data, nil
}

// createCloseRollCallNow returns a valid message.DataCloseRollCall with the given arguments.
func createRollCallNow(publicKey []byte, privateKey ed.PrivateKey, creation int64, start int64, name string, laoId []byte) (message.DataCreateRollCall, error) {
	if (len(publicKey) != ed.PublicKeySize) || len(privateKey) != ed.PrivateKeySize {
		return message.DataCreateRollCall{}, errors.New("bad public key size")
	}
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", b64.StdEncoding.EncodeToString(laoId), strconv.FormatInt(creation, 10), name)
	idData := HashItems(elementsToHashForDataId)
	var data = message.DataCreateRollCall{
		Object:      "roll_call",
		Action:      "create",
		ID:          idData,
		Name:        name,
		Creation:    creation,
		Location:    "pas loin",
		Start:       start,
		Description: "un roll call",
	}
	return data, nil
}

// createCloseRollCallNow returns a valid message.DataCloseRollCall with the given arguments.
func createCloseRollCallNow(publicKey []byte, privateKey ed.PrivateKey, creation int64, start int64, end int64, name string, laoId []byte, attendeePublicKeys [][]byte) (message.DataCloseRollCall, error) {
	if (len(publicKey) != ed.PublicKeySize) || len(privateKey) != ed.PrivateKeySize {
		return message.DataCloseRollCall{}, errors.New("bad public key size")
	}
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", b64.StdEncoding.EncodeToString(laoId), strconv.FormatInt(creation, 10), name)
	idData := HashItems(elementsToHashForDataId)
	var data = message.DataCloseRollCall{
		Object:    "roll_call",
		Action:    "close",
		ID:        idData,
		Name:      name,
		Start:     start,
		End:       end,
		Attendees: attendeePublicKeys,
	}
	return data, nil
}

// createOpenRollCall returns a valid message.DataOpenRollCall as should be received.
func createOpenRollCall(publicKey []byte, privateKey ed.PrivateKey, creation int64, start int64, name string, laoId []byte) (message.DataOpenRollCall, error) {
	if len(publicKey) != ed.PublicKeySize || len(privateKey) != ed.PrivateKeySize {
		return message.DataOpenRollCall{}, errors.New("bad public key size")
	}
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", b64.StdEncoding.EncodeToString(laoId), strconv.FormatInt(creation, 10), name)
	idData := HashItems(elementsToHashForDataId)
	var data = message.DataOpenRollCall{
		Object: "roll_call",
		Action: "open",
		ID:     idData,
		Start:  start,
	}
	return data, nil
}

// computeMessageId computes the ID of a message, by hashing the data field concatenated with itself signed with the
// privateKey given as argument
func computeMessageId(data interface{}, privateKey ed.PrivateKey) (dataFlat, signed, id []byte, err error) {
	dataFlat, err = json.Marshal(data)
	if err != nil {
		return nil, nil, nil, errors.New("error, could not marshal data")
	}
	signed = ed.Sign(privateKey, dataFlat)

	var itemsToHashForMessageId []string
	itemsToHashForMessageId = append(itemsToHashForMessageId, b64.StdEncoding.EncodeToString(dataFlat), b64.StdEncoding.EncodeToString(signed))
	hash := HashItems(itemsToHashForMessageId)
	return dataFlat, signed, hash, nil

}
