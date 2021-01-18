// define/securityHelpers
package security

import (
	ed "crypto/ed25519"
	b64 "encoding/base64"
	"encoding/json"
	"errors"
	"math/rand"
	"strconv"
	"student20_pop/event"
	"testing"
	"time"

	message2 "student20_pop/message"
	"student20_pop/parser"
)

// we don't check the switch in messageIsValid

//TestMessageIsValidWithoutWitnesses checks that a message containing a createLao is valid at both message and data layer
func TestMessageIsValidWithoutWitnesses(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		witnessKeys := [][]byte{}
		var creation = time.Now().Unix()
		name := "My LAO"
		data, err := createDataLao(pubkey, privkey, witnessKeys, creation, name)
		if err != nil {
			t.Error(err)
		}
		valid := LAOIsValid(data, true)
		if valid != true {
			t.Errorf("Created Lao Should be valid %#v", data)
		}
		err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestDataWitnessMessageIsValid checks that a witness message is valid
func TestDataWitnessMessageIsValid(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		messageIdToWitness := []byte("enfin un truc à signer")
		data, err := createDataWitnessMessage(privkey, messageIdToWitness)
		if err != nil {
			t.Error(err)
		}
		err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

/*
// Basically following the last meeting (22/12/20) we are not supposed to have this case
func TestMessageIsValidWithAssessedWitnesses(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		authorisedWitnessKeys, jsonArrayOfWitnessSigantures, id, err := witnessesAndSignatures(true, true)
		var creation = time.Now().Unix()
		name := "My LAO"
		data, err := createDataLao(pubkey, privkey, authorisedWitnessKeys, creation, name)
		if err != nil {
			t.Error(err)
		}
		valid := LAOIsValid(data,true )
		if valid != true {
			t.Errorf("Created Lao Should be valid %#v", data)
		}

		data, signed, id,err:= getIdofMessage(data,privkey)
		if err != nil {
			t.Error(err)
		}
		witnessSignatures := arraySign(keyz,id)
		err = CheckMessageIsValid(pubkey,privkey,data,witnessSignatures,witnessKeys)
		if err != nil {
			t.Error(err)
		}
	}
}
*/
//TestRollCallOpenedIsValid checks that a message containing a openRollCall is valid at both message and data layer
func TestRollCallOpenedIsValid(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		rollCallCreation := time.Now().Unix()
		start := rollCallCreation + (MaxPropagationDelay / 2)
		rollCallName := "encore un roll call"
		lao_id := []byte("12345")

		data, err := createOpenRollCall(pubkey, privkey, rollCallCreation, start, rollCallName, lao_id)
		if err != nil {
			t.Error(err)
		}
		rollCall, err := createEventRollCall(pubkey, privkey, rollCallCreation, start, rollCallName, lao_id)
		if err != nil {
			t.Error(err)
		}
		valid := RollCallOpenedIsValid(data, string(lao_id), rollCall)
		if valid != true {
			t.Errorf("Created rollcall Should be valid %#v", data)
		}
		err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestRollCallClosedIsValid checks that a message containing a createRollCall is valid at both message and data layer
func TestRollCallClosedIsValid(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		attendeesPks := [][]byte{}

		rollCallCreation := time.Now().Unix()
		rollCallName := "encore un roll call"
		start := rollCallCreation + (MaxPropagationDelay / 4)
		end := start + (MaxPropagationDelay / 4)
		lao_id := []byte("12345")
		data, err := createCloseRollCallNow(pubkey, privkey, rollCallCreation, start, end, rollCallName, lao_id, attendeesPks)
		if err != nil {
			t.Error(err)
		}
		rollCall, err := createEventRollCall(pubkey, privkey, rollCallCreation, start, rollCallName, lao_id)
		if err != nil {
			t.Error(err)
		}
		valid := RollCallClosedIsValid(data, string(lao_id), rollCall)
		if valid != true {
			t.Errorf("closed rollcall Should be valid %#v", data)
		}
		err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestRollCallCreatedIsValid checks that a message containing a createRollCall is valid at both message and data layer
func TestRollCallCreatedIsValid(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		var creation = time.Now().Unix()
		start := creation + (MaxPropagationDelay / 2)
		name := "RollCallNow"
		lao_id := []byte("12345")
		data, err := createRollCallNow(pubkey, privkey, creation, start, name, lao_id)
		if err != nil {
			t.Error(err)
		}
		valid := RollCallCreatedIsValid(data, string(lao_id))
		if valid != true {
			t.Errorf("Created rollcall Should be valid %#v", data)
		}
		err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestMeetingCreatedIsValid checks that a message containing a createMeeting is valid at both message and data layer
func TestMeetingCreatedIsValid(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		var creation = time.Now().Unix()
		start := creation + (MaxPropagationDelay / 2)
		end := creation + (MaxPropagationDelay / 2)
		name := "someMeetintintinitin"
		lao_id := []byte("12345")

		data, err := createMeeting(pubkey, privkey, creation, start, end, name, lao_id)
		if err != nil {
			t.Error(err)
		}
		valid := MeetingCreatedIsValid(data, string(lao_id))
		if valid != true {
			t.Errorf("Created Meeting Should be valid %#v", data)
		}
		err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//==================invalid Tests========================//

//TestBadDataWitnessMessage checks that a bad witness message is invalid
func TestBadDataWitnessMessage(t *testing.T) {
	pubkey, privkey := createKeyPair()
	witnessSignatures := []message2.ItemWitnessSignatures{}
	messageIdToWitness := []byte("enfin un truc à signer")
	data, err := createDataWitnessMessage(privkey, messageIdToWitness)
	if err != nil {
		t.Error(err)
	}
	data.Signature = []byte("oulala la mauvaise signature")
	err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
	if err == nil {
		t.Errorf("Didn't detect Witness message with invalid signaure %#v", data)
	}
	//==================================================================//
	//Here we test when the message signature is invalid
	message, err := makeMessage(pubkey, privkey, data, witnessSignatures)
	if err != nil {
		t.Error(err)
	}

	message.Signature = []byte("oulala la mauvaise signature")

	messageFlat, err := json.Marshal(message)
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
	message, err = makeMessage(pubkey, privkey, data, witnessSignatures)
	if err != nil {
		t.Error(err)
	}

	message.MessageId = []byte("oulala la mauvaise identititittity")

	messageFlat, err = json.Marshal(message)
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
	message, err = makeMessage(pubkey, privkey, data, witnessSignatures)
	if err != nil {
		t.Error(err)
	}

	message.Data = []byte("oulala la\", \\} mauvaise data imparsable")

	messageFlat, err = json.Marshal(message)
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
	pubkey, privkey := createKeyPair()
	attendeesPks := [][]byte{}

	rollCallCreation := time.Now().Unix()
	rollCallName := "encore un roll call"
	start := rollCallCreation + (MaxPropagationDelay / 4)
	end := start - (MaxPropagationDelay / 4)
	lao_id := []byte("12345")
	data, err := createCloseRollCallNow(pubkey, privkey, rollCallCreation, start, end, rollCallName, lao_id, attendeesPks)
	if err != nil {
		t.Error(err)
	}
	rollCall, err := createEventRollCall(pubkey, privkey, rollCallCreation, start, rollCallName, lao_id)
	if err != nil {
		t.Error(err)
	}
	valid := RollCallClosedIsValid(data, string(lao_id), rollCall)
	if valid == true {
		t.Errorf("didn't detect that closed rollcall is invalid %#v", data)
	}
}

//TestOpenRollCallBadFields checks that a message containing a openRollCall with invalid start is invalid at data layer
func TestOpenRollCallBadFields(t *testing.T) {
	for i := 0; i < 2; i++ {
		pubkey, privkey := createKeyPair()
		rollCallCreation := time.Now().Unix()
		start := rollCallCreation - (MaxPropagationDelay / 2)
		rollCallName := "encore un roll call"
		lao_id := []byte("12345")

		data, err := createOpenRollCall(pubkey, privkey, rollCallCreation, start, rollCallName, lao_id)
		if err != nil {
			t.Error(err)
		}
		rollCall, err := createEventRollCall(pubkey, privkey, rollCallCreation, start, rollCallName, lao_id)
		if err != nil {
			t.Error(err)
		}
		valid := RollCallOpenedIsValid(data, string(lao_id), rollCall)
		if valid == true {
			t.Errorf("Opened rollcall Should be invalid, start before creation %#v", data)
		}
	}
}

//TestRollCallCreatedBadFields verifies that roll call is invalidated due to incorrect id,timestamps,
//and that the message is invalidated due to incorrect private key
func TestRollCallCreatedBadFields(t *testing.T) {
	for i := 0; i < 2; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		var creation = time.Now().Unix()
		start := creation + (MaxPropagationDelay / 2)
		name := "RollCallNow"
		lao_id := []byte("12345")
		data, err := createRollCallNow(pubkey, privkey, creation, start, name, lao_id)
		if err != nil {
			t.Error(err)
		}
		notLao_id := []byte("6789")
		valid := RollCallCreatedIsValid(data, string(notLao_id))
		if valid == true {
			t.Errorf("Created Rollcall Should be invalid beacause of invalid id %#v", data)
		}
		//===========================================================//
		creationBis := time.Now().Unix() + time.Now().Unix() + MaxPropagationDelay + 1
		if i%2 == 0 {
			creationBis = time.Now().Unix() - MaxClockDifference - 1
		}
		data.Creation = creationBis
		valid = RollCallCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to bad creation %#v", data)
		}
		//===========================================================//
		data.Start = 2
		data.Scheduled = 3
		valid = RollCallCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have start "+
				"& scheduled strictly positive at the same time) %#v", data)
		}
		//===========================================================//
		data.Start = 0
		data.Scheduled = 0
		valid = RollCallCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have start "+
				"& scheduled equal 0 at the same time) %#v", data)
		}
		//===========================================================//
		data.Start = data.Creation - 5
		data.Scheduled = 0
		valid = RollCallCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have start "+
				"less than creation & scheduled equal 0 ) %#v", data)
		}
		//===========================================================//
		data.Scheduled = data.Creation - 5
		data.Start = 0
		valid = RollCallCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have scheduled "+
				"less than creation & start equal 0 ) %#v", data)
		}
		//===========================================================//
		data.Scheduled = data.Creation - 5
		data.Start = 0
		valid = RollCallCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to incoherent setup (cannot have scheduled "+
				"less than creation & start equal 0 ) %#v", data)
		}
		//verify that the message is invalidated due to incorrect private key
		_, falsePrivkey := createKeyPair()
		err = CheckMessageIsValid(pubkey, falsePrivkey, data, witnessSignatures)
		if err == nil {
			t.Errorf("The Message Should be invalid beacause of incorrect Key %#v", data)
		}
	}
}

//TestMeetingBadFields verifies that the meeting is invalid due to invalid timestamps,name and id
func TestMeetingBadFields(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 2; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		creation := time.Now().Unix() + time.Now().Unix() + MaxPropagationDelay + 1
		if i%2 == 0 {
			creation = time.Now().Unix() - MaxClockDifference - 1
		}
		start := creation + (MaxPropagationDelay / 2)
		end := creation + (MaxPropagationDelay / 2)
		name := "someMeeting"
		lao_id := []byte("12345")

		data, err := createMeeting(pubkey, privkey, creation, start, end, name, lao_id)
		if err != nil {
			t.Error(err)
		}
		valid := MeetingCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to bad creation %#v", data)
		}
		//==================================================================//
		creation = time.Now().Unix()
		data.Start = creation + 2*MaxPropagationDelay
		valid = MeetingCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to bad start %#v", data)
		}
		//==================================================================//
		creation = time.Now().Unix()
		data.Start = creation - 2*MaxPropagationDelay
		valid = MeetingCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to bad timestamp: "+
				"Either end is before start, or start before creation. %#v", data)
		}
		//data.Start < data.Creation || (data.End != 0 && data.End < data.Start) {
		//==================================================================//
		data.End = data.Start - 10
		valid = MeetingCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to bad end %#v", data)
		}
		//==================================================================//
		data.Creation = time.Now().Unix()
		data.Start = creation + (MaxPropagationDelay / 2)
		data.End = creation + (MaxPropagationDelay / 2)
		data.Name = ""
		valid = MeetingCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to empty name %#v", data)
		}
		//==================================================================//
		data.ID = []byte("wo shi fa guo ren")
		valid = MeetingCreatedIsValid(data, string(lao_id))
		if valid == true {
			t.Errorf("Created Meeting Should be invalid due to bad id %#v", data)
		}
		err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

//TestLAOInvalidName verifies that the Lao is invalid due to empty name
func TestLAOInvalidName(t *testing.T) {
	pubkey, privkey := createKeyPair()
	witnessKeys := [][]byte{}
	var creation = time.Now().Unix()
	// should be not empty but) not in the protospecs ?
	name := ""
	data, err := createDataLao(pubkey, privkey, witnessKeys, creation, name)
	if err != nil {
		t.Error(err)
	}
	valid := LAOIsValid(data, true)
	if valid == true {
		t.Errorf("Created Lao Should be invalid due to empty location %#v", data)
	}
}

//TestLAOInvalidId verifies that the Lao is invalid due to invlaid id
func TestLAOInvalidId(t *testing.T) {
	pubkey, privkey := createKeyPair()
	witnessKeys := [][]byte{}
	var creation = time.Now().Unix()
	name := "helloo"
	data, err := createDataLao(pubkey, privkey, witnessKeys, creation, name)
	if err != nil {
		t.Error(err)
	}
	data.ID = []byte("123456")
	valid := LAOIsValid(data, true)
	if valid == true {
		t.Errorf("Created Lao Should be invalid due to incorrect id %#v", data)
	}
}

//TestLAOIInvalidCreationTime verifies that the Lao is invalid due to invlaid creation time
func TestLAOIInvalidCreationTime(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessKeys := [][]byte{}
		name := "ok"
		creation := time.Now().Unix() + time.Now().Unix() + MaxPropagationDelay + 1
		if i%2 == 0 {
			creation = time.Now().Unix() - MaxClockDifference - 1
		}
		data, err := createDataLao(pubkey, privkey, witnessKeys, creation, name)
		if err != nil {
			t.Error(err)
		}
		valid := LAOIsValid(data, true)
		if valid != false {
			t.Errorf("Created Lao Should be invalid due to wrong creation time %#v", data)
		}
	}
}

//===================================================================================//
func CheckMessageIsValid(pubkey []byte, privkey ed.PrivateKey, data interface{}, witnessKeysAndSignatures []message2.ItemWitnessSignatures) error {

	message, err := makeMessage(pubkey, privkey, data, witnessKeysAndSignatures)
	if err != nil {
		return err
	}
	messageFlat, err := json.Marshal(message)
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
func makeMessage(pubkey []byte, privkey ed.PrivateKey, data interface{}, witnessKeysAndSignatures []message2.ItemWitnessSignatures) (message2.Message, error) {
	var dataFlat, signed, id []byte
	var err error
	dataFlat, signed, id, err = getIdofMessage(data, privkey)
	if err != nil {
		return message2.Message{}, err
	}

	//witness signatures
	ArrayOfWitnessSignatures, err := PlugWitnessesInArray(witnessKeysAndSignatures)
	if err != nil {
		return message2.Message{}, err
	}
	var message = message2.Message{
		Data:              dataFlat, // in base 64
		Sender:            pubkey,
		Signature:         signed,
		MessageId:         id[:],
		WitnessSignatures: ArrayOfWitnessSignatures,
	}
	return message, nil
}
func PlugWitnessesInArray(witnessKeysAndSignatures []message2.ItemWitnessSignatures) ([]json.RawMessage, error) {
	ArrayOfWitnessSignatures := []json.RawMessage{}
	for i := 0; i < len(witnessKeysAndSignatures); i++ {
		witnessSignatureI, err := json.Marshal(witnessKeysAndSignatures[i])
		if err != nil {
			return nil, errors.New("Problem when Marshaling witnessKeysAndSignatures")
		}
		CoupleToAdd := witnessSignatureI[:]
		ArrayOfWitnessSignatures = append(ArrayOfWitnessSignatures, CoupleToAdd)
	}
	return ArrayOfWitnessSignatures, nil
}
func createKeyPair() ([]byte, ed.PrivateKey) {
	//randomize the key
	randomSeed := make([]byte, 32)
	rand.Read(randomSeed)
	privkey := ed.NewKeyFromSeed(randomSeed)
	return privkey.Public().(ed.PublicKey), privkey
}

func createMeeting(orgPubkey []byte, privkey ed.PrivateKey, creation int64, start int64, end int64, name string, lao_id []byte) (message2.DataCreateMeeting, error) {
	if (len(orgPubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return message2.DataCreateMeeting{}, errors.New("wrong argument -> size of public key don't respected ")
	}
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "M", b64.StdEncoding.EncodeToString(lao_id), strconv.FormatInt(creation, 10), name)
	idData := HashItems(elementsToHashForDataId)
	var data = message2.DataCreateMeeting{
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

func createDataWitnessMessage(privkey ed.PrivateKey, mesageIdToWitness []byte) (message2.DataWitnessMessage, error) {
	if len(privkey) != ed.PrivateKeySize {
		return message2.DataWitnessMessage{}, errors.New("wrong argument -> size of private key don't respected ")
	}
	signed := ed.Sign(privkey, mesageIdToWitness)
	var data = message2.DataWitnessMessage{
		Object:    "message",
		Action:    "witness",
		MessageId: mesageIdToWitness,
		Signature: signed,
	}
	return data, nil
}

func createDataLao(orgPubkey []byte, privkey ed.PrivateKey, WitnesseKeys [][]byte, creation int64, name string) (message2.DataCreateLAO, error) {
	if (len(orgPubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return message2.DataCreateLAO{}, errors.New("wrong argument -> size of public key don't respected ")
	}
	var itemsToHashForId []string
	itemsToHashForId = append(itemsToHashForId, b64.StdEncoding.EncodeToString(orgPubkey), strconv.FormatInt(creation, 10), name)
	idData := HashItems(itemsToHashForId)
	var data = message2.DataCreateLAO{
		Object:    "lao",
		Action:    "create",
		ID:        idData[:],
		Name:      name,
		Creation:  creation,
		Organizer: orgPubkey,
		Witnesses: WitnesseKeys,
	}
	return data, nil
}
func createEventRollCall(pubkey []byte, privkey ed.PrivateKey, creation int64, start int64, name string, lao_id []byte) (event.RollCall, error) {
	if (len(pubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return event.RollCall{}, errors.New("wrong argument -> size of public key don't respected ")
	}
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", b64.StdEncoding.EncodeToString(lao_id), strconv.FormatInt(creation, 10), name)
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
func createRollCallNow(pubkey []byte, privkey ed.PrivateKey, creation int64, start int64, name string, lao_id []byte) (message2.DataCreateRollCall, error) {
	if (len(pubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return message2.DataCreateRollCall{}, errors.New("wrong argument -> size of public key don't respected ")
	}
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", b64.StdEncoding.EncodeToString(lao_id), strconv.FormatInt(creation, 10), name)
	idData := HashItems(elementsToHashForDataId)
	var data = message2.DataCreateRollCall{
		Object:      "roll_call",
		Action:      "create",
		ID:          idData[:],
		Name:        name,
		Creation:    creation,
		Location:    "pas loin",
		Start:       start,
		Description: "un roll call",
	}
	return data, nil
}
func createCloseRollCallNow(pubkey []byte, privkey ed.PrivateKey, creation int64, start int64, end int64, name string, lao_id []byte, attendeesPks [][]byte) (message2.DataCloseRollCall, error) {
	if (len(pubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return message2.DataCloseRollCall{}, errors.New("wrong argument -> size of public key don't respected ")
	}
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", b64.StdEncoding.EncodeToString(lao_id), strconv.FormatInt(creation, 10), name)
	idData := HashItems(elementsToHashForDataId)
	var data = message2.DataCloseRollCall{
		Object:    "roll_call",
		Action:    "close",
		ID:        idData,
		Name:      name,
		Start:     start,
		End:       end,
		Attendees: attendeesPks,
	}
	return data, nil
}
func createOpenRollCall(pubkey []byte, privkey ed.PrivateKey, creation int64, start int64, name string, lao_id []byte) (message2.DataOpenRollCall, error) {
	if (len(pubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return message2.DataOpenRollCall{}, errors.New("wrong argument -> size of public key don't respected ")
	}
	var elementsToHashForDataId []string
	elementsToHashForDataId = append(elementsToHashForDataId, "R", b64.StdEncoding.EncodeToString(lao_id), strconv.FormatInt(creation, 10), name)
	idData := HashItems(elementsToHashForDataId)
	var data = message2.DataOpenRollCall{
		Object: "roll_call",
		Action: "open",
		ID:     idData[:],
		Start:  start,
	}
	return data, nil
}

func getIdofMessage(data interface{}, privkey ed.PrivateKey) (dataFlat, signed, id []byte, err error) {
	dataFlat, err = json.Marshal(data)
	if err != nil {
		return nil, nil, nil, errors.New("Error : Impossible to marshal data")
	}
	signed = ed.Sign(privkey, dataFlat)

	var itemsToHashForMessageId []string
	itemsToHashForMessageId = append(itemsToHashForMessageId, b64.StdEncoding.EncodeToString(dataFlat), b64.StdEncoding.EncodeToString(signed))
	hash := HashItems(itemsToHashForMessageId)
	return dataFlat, signed, hash, nil

}
