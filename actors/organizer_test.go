package actors

import (
	"testing"
	"student20_pop/lib"
	"reflect"
	"sync"
	ed "crypto/ed25519"
	"crypto/sha256"
	"math/rand"
	b64 "encoding/base64"
	"strings"
	"time"
	"strconv"
	"os"
)

// The tests in this package have a rather global coverage: 
// they indirectly test most of the functions in parser, a good chunk of the function in security, and as well of the database functions.
// On the other hand, they admittedly do not try to test every branching path. Currently, the focus is very much on testing that correct strings are accepted rather that incorrect strings are rejected.
// This seems a decent trade-off for time-efficiency as our code is quite prone to raising errors.


// TODO: To not break package encapsulation, it should probably be wise to disable the two followings structs while not testing.
type hub struct {
	// the mutex to protect connections
	connectionsMx sync.RWMutex

	// Registered connections.
	connections map[*connection]struct{}

	idOfSender int
	//msg received from the sender through the websocket
	receivedMessage chan []byte

	logMx sync.RWMutex
	log   [][]byte

	actor Actor

	connIndex int
}

type connection struct {
	// Buffered channel of outbound messages.
	send chan []byte
	id   int
	// The hub.
	h *hub
}


////////////////////////////////////////////

// createKeyPair generate a new random pair of public/private key
func createKeyPair() ([]byte, ed.PrivateKey) {
	//randomize the key
	randomSeed := make([]byte, 32)
	rand.Read(randomSeed)
	privkey := ed.NewKeyFromSeed(randomSeed)
	return privkey.Public().(ed.PublicKey), privkey
}

// getCorrectDataCreateLAO generate a example JSON string of the data field of a request for LAO creation
func getCorrectDataCreateLAO(publicKey []byte, creationstr string) string {
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	tohash := lib.ComputeAsJsonArray([]string{string(pkeyb64),creationstr,"my_lao"})
	hashid := sha256.Sum256( []byte(tohash) )
	id := b64.StdEncoding.EncodeToString( hashid[:] )
	data := `{
		"object": "lao",
		"action": "create",
		"id": "`+id+`",
		"name": "my_lao",
		"creation": `+creationstr+`,
		"organizer": "`+pkeyb64+`",
		"witnesses": {
	
		}
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataCreateLAO generate a example JSON string of the data field of a request for meeting creation
func getCorrectDataCreateMeeting(publicKey []byte) string {
	//pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	creationstr := strconv.FormatInt(time.Now().Unix(), 10)
	startstr := strconv.FormatInt(time.Now().Unix()+1000, 10)
	tohash := lib.ComputeAsJsonArray([]string{"M","LAO_id",creationstr,"my_meeting"})
	hashid := sha256.Sum256( []byte(tohash) )
	id := b64.StdEncoding.EncodeToString( hashid[:] )
	data := `{
		"object": "meeting",
		"action": "create",
		"id": "`+id+`",
		"name": "my_meeting",
		"creation": `+creationstr+`,
		"location": "here",
		"start": `+startstr+`
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataCreateRollCallNow generate a example JSON string of the data field of a request for rollcall creation starting now
func getCorrectDataCreateRollCallNow(publicKey []byte) string {
	//pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	creationstr := strconv.FormatInt(time.Now().Unix(), 10)
	startstr := strconv.FormatInt(time.Now().Unix()+1000, 10)
	tohash := lib.ComputeAsJsonArray([]string{"R","LAO_id",creationstr,"my_roll_call"})
	hashid := sha256.Sum256( []byte(tohash) )
	id := b64.StdEncoding.EncodeToString( hashid[:] )
	data := `{
		"object": "roll_call",
		"action": "create",
		"id": "`+id+`",
		"name": "my_roll_call",
		"creation": `+creationstr+`,
		"start": `+startstr+`,
		"location": "here"
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataCreateRollCallLater generate a example JSON string of the data field of a request for rollcall creation starting at a scheduled time
func getCorrectDataCreateRollCallLater(publicKey []byte, creationstr string) string {
	//pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	startstr := strconv.FormatInt(time.Now().Unix()+1000, 10)
	tohash := lib.ComputeAsJsonArray([]string{"R","LAO_id",creationstr,"my_roll_call"})
	hashid := sha256.Sum256( []byte(tohash) )
	id := b64.StdEncoding.EncodeToString( hashid[:] )
	data := `{
		"object": "roll_call",
		"action": "create",
		"id": "`+id+`",
		"name": "my_roll_call",
		"creation": `+creationstr+`,
		"scheduled": `+startstr+`,
		"location": "here"
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataOpenRollCall generate a example JSON string of the data field of a request for opening a rollcall at a previously scheduled time
func getCorrectDataOpenRollCall(publicKey []byte, creationstr string) string {
	//pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	startstr := strconv.FormatInt(time.Now().Unix()+1000, 10)
	tohash := lib.ComputeAsJsonArray([]string{"R","LAO_id",creationstr,"my_roll_call"})
	hashid := sha256.Sum256( []byte(tohash) )
	id := b64.StdEncoding.EncodeToString( hashid[:] )
	data := `{
		"object": "roll_call",
		"action": "open",
		"id": "`+id+`",
		"start": `+startstr+`
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataCloseRollCall generate a example JSON string of the data field of a request for closing a rollcall 
func getCorrectDataCloseRollCall(publicKey []byte, creationstr string) string {
	//pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	startstr := strconv.FormatInt(time.Now().Unix()+1000, 10)
	endstr := strconv.FormatInt(time.Now().Unix()+2000, 10)
	tohash := lib.ComputeAsJsonArray([]string{"R","LAO_id",creationstr,"my_roll_call"})
	hashid := sha256.Sum256( []byte(tohash) )
	id := b64.StdEncoding.EncodeToString( hashid[:] )
	data := `{
		"object": "roll_call",
		"action": "close",
		"id": "`+id+`",
		"start": `+startstr+`,
		"end": `+endstr+`,
		"attendees": ["1234"] 
	}`
	// TODO maybe the attendees field check for a real public key??
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataStateLAO generate a example JSON string of the data field of a request for announcing the state of a LAO (typically once it has received enough certifications)
func getCorrectDataStateLAO(publicKey []byte, creationstr string) string {
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	lastmodified := strconv.FormatInt(time.Now().Unix(), 10)
	tohash := lib.ComputeAsJsonArray([]string{string(pkeyb64),creationstr,"my_lao"})
	hashid := sha256.Sum256( []byte(tohash) )
	id := b64.StdEncoding.EncodeToString( hashid[:] )
	data := `{
		"object": "lao",
		"action": "state",
		"id": "`+id+`",
		"name": "my_lao",
		"creation": `+creationstr+`,
		"last_modified": `+lastmodified+`,
		"organizer": "`+pkeyb64+`",
		"witnesses": {
	
		},
		"modification_id": "`+id+`",
		"modification_signatures": []
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// TODO test stateMeeting, updateLAO, witnessMessage. write data for stateMeeting, updateLAO


func getCorrectDataWitnessMessage(privateKey ed.PrivateKey,messageId string) string {
	signature := ed.Sign(privateKey, []byte(messageId))
	signatureb64 := b64.StdEncoding.EncodeToString(signature)
	data := `{
		"object": "message",
		"action": "witness",
		"message_id": "`+messageId+`",
		"signature	": "`+signatureb64+`"
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

/////////////////////////////////////////////////////////////////////////////////////////////////////

// getCorrectPublishOnRoot generate a example JSON string of the whole request for a publish, based on a data []byte
func getCorrectPublishOnRoot(publicKey []byte, privateKey ed.PrivateKey, data []byte) []byte {
	datab64 := b64.StdEncoding.EncodeToString(data)
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	signature := ed.Sign(privateKey, data)
	signatureb64 := b64.StdEncoding.EncodeToString(signature)
	tohash := lib.ComputeAsJsonArray([]string{datab64,signatureb64})
	msgid := sha256.Sum256( []byte(tohash))
	msgidb64 := b64.StdEncoding.EncodeToString(msgid[:])
	msg := `{
		"jsonrpc": "2.0",
		"method": "publish",
		"params": {
			"channel": "/root",
			"message": {
				"data": "`+datab64+`",
				"sender": "`+pkeyb64+`",
				"signature": "`+signatureb64+`",
				"message_id": "`+msgidb64+`",
				"witness_signatures": {
	
				}
			}
		},
		"id": 0
	}`
	// MTIz is b64encoded 123
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	msg = strings.Join(strings.Fields(msg), "")
	return []byte(msg)
}


// getCorrectPublishGeneral generate a example JSON string of the whole request for a publish, based on a data []byte
func getCorrectPublishGeneral(publicKey []byte, privateKey ed.PrivateKey, data []byte) []byte {
	datab64 := b64.StdEncoding.EncodeToString(data)
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	signature := ed.Sign(privateKey, data)
	signatureb64 := b64.StdEncoding.EncodeToString(signature)
	tohash := lib.ComputeAsJsonArray([]string{datab64,signatureb64})
	msgid := sha256.Sum256( []byte(tohash))
	msgidb64 := b64.StdEncoding.EncodeToString(msgid[:])
	msg := `{
		"jsonrpc": "2.0",
		"method": "publish",
		"params": {
			"channel": "/root/LAO_id",
			"message": {
				"data": "`+datab64+`",
				"sender": "`+pkeyb64+`",
				"signature": "`+signatureb64+`",
				"message_id": "`+msgidb64+`",
				"witness_signatures": {
	
				}
			}
		},
		"id": 0
	}`
	// MTIz is b64encoded 123
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	msg = strings.Join(strings.Fields(msg), "")
	return []byte(msg)
}

// getExpectedMsgAndChannelForPublishOnRoot generate a example JSON string of the whole broadcasted struct sent back for LAO creation
// according to the data field passed in argument
func getExpectedMsgAndChannelForPublishOnRoot(publicKey []byte, privateKey ed.PrivateKey, data []byte) []lib.MessageAndChannel {
	datab64 := b64.StdEncoding.EncodeToString(data)
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	signature := ed.Sign(privateKey, data)
	signatureb64 := b64.StdEncoding.EncodeToString(signature)
	tohash := lib.ComputeAsJsonArray([]string{datab64,signatureb64})
	msgid := sha256.Sum256( []byte(tohash))
	msgidb64 := b64.StdEncoding.EncodeToString(msgid[:])
	msg := `{
		"jsonrpc": "2.0",
		"method": "broadcast",
		"params": {
			"channel": "/root",
			"message": {
				"data": "`+datab64+`",
				"sender": "`+pkeyb64+`",
				"signature": "`+signatureb64+`",
				"message_id": "`+msgidb64+`",
				"witness_signatures": {
	
				}
			}
		},
		"id": 0
	}`
	// MTIz is b64encoded 123
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	msg = strings.Join(strings.Fields(msg), "")
	answer := []lib.MessageAndChannel{{
		Message: []byte(msg),
		Channel: []byte("/root"),
	}}
	return answer
}

// getExpectedMsgAndChannelForPublishGeneral generate a example JSON string of the whole broadcasted struct sent back for LAO creation
// according to the data field passed in argument
func getExpectedMsgAndChannelForPublishGeneral(publicKey []byte, privateKey ed.PrivateKey, data []byte) []lib.MessageAndChannel {
	datab64 := b64.StdEncoding.EncodeToString(data)
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	signature := ed.Sign(privateKey, data)
	signatureb64 := b64.StdEncoding.EncodeToString(signature)
	tohash := lib.ComputeAsJsonArray([]string{datab64,signatureb64})
	msgid := sha256.Sum256( []byte(tohash))
	msgidb64 := b64.StdEncoding.EncodeToString(msgid[:])
	msg := `{
		"jsonrpc": "2.0",
		"method": "broadcast",
		"params": {
			"channel": "/root/LAO_id",
			"message": {
				"data": "`+datab64+`",
				"sender": "`+pkeyb64+`",
				"signature": "`+signatureb64+`",
				"message_id": "`+msgidb64+`",
				"witness_signatures": {
	
				}
			}
		},
		"id": 0
	}`
	// MTIz is b64encoded 123
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	msg = strings.Join(strings.Fields(msg), "")
	answer := []lib.MessageAndChannel{{
		Message: []byte(msg),
		Channel: []byte("/root/LAO_id"),
	}}
	return answer
}

/////////////////////////////////////////////////////////////////////////////////////

// TestReceivePublishCreateLAO tests if sending a JSON string requesting to publish a LAO creation works 
// by comparing the messages (response and broadcasted answers) sent back
func TestReceivePublishCreateLAO(t *testing.T) {

	publicKey, privateKey := createKeyPair()

	creationstr := strconv.FormatInt(time.Now().Unix(), 10)
	receivedMsg := getCorrectPublishOnRoot(publicKey, privateKey, []byte(getCorrectDataCreateLAO(publicKey, creationstr)))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishOnRoot(publicKey, privateKey, []byte(getCorrectDataCreateLAO(publicKey, creationstr))) // which will never be sent, but still produced)
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`) 

	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
		actor: 			NewOrganizer(string(publicKey), "org_test.db"),
	}

	msgAndChannel, responseToSender := h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}
	_ = os.Remove("org_test.db")
}

// TestReceivePublishStateLAO should correctly receive a nil broadcast and an error answer as currently an organizer backend should never receive such a message
func TestReceivePublishStateLAO(t *testing.T) {

	publicKey, privateKey := createKeyPair()

	creationstr := strconv.FormatInt(time.Now().Unix(), 10)
	receivedMsg := getCorrectPublishOnRoot(publicKey, privateKey, []byte(getCorrectDataCreateLAO(publicKey, creationstr)))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishOnRoot(publicKey, privateKey, []byte(getCorrectDataCreateLAO(publicKey, creationstr))) // which will never be sent, but still produced)
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`) 

	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
		actor: 			NewOrganizer(string(publicKey), "org_test.db"),
	}

	msgAndChannel, responseToSender := h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	// An organizer back end should never receive a publishStateLao
	receivedMsg = getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataStateLAO(publicKey, creationstr)))
	userId = 5
	expectedMsgAndChannel = nil
	expectedResponseToSender = []byte(`{"jsonrpc":"2.0","error":{"code":-1,"description":"invalid action"},"id":0}`) 

	msgAndChannel, responseToSender = h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}


	_ = os.Remove("org_test.db")
}

// TestReceivePublishCreateMeeting tests if sending a JSON string requesting to a meeting creation works 
// by comparing the messages (response and broadcasted answers) sent back
func TestReceivePublishCreateMeeting(t *testing.T) {

	publicKey, privateKey := createKeyPair()

	receivedMsg := getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateMeeting(publicKey)))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateMeeting(publicKey)))
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`) 

	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
		actor: 			NewOrganizer(string(publicKey), "org_test.db"),
	}

	msgAndChannel, responseToSender := h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", string(msgAndChannel[0].Channel), string(expectedMsgAndChannel[0].Channel))
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}
	_ = os.Remove("org_test.db")
}


// TestReceivePublishCreateRollCallNow tests if sending a JSON string requesting a rollcall creation starting now works 
// by comparing the messages (response and broadcasted answers) sent back
func TestReceivePublishCreateRollCallNow(t *testing.T) {

	publicKey, privateKey := createKeyPair()

	receivedMsg := getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateRollCallNow(publicKey)))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateRollCallNow(publicKey)))
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`) 

	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
		actor: 			NewOrganizer(string(publicKey), "org_test.db"),
	}

	msgAndChannel, responseToSender := h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", string(msgAndChannel[0].Channel), string(expectedMsgAndChannel[0].Channel))
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}
	_ = os.Remove("org_test.db")
}

// TestReceivePublishCreateMeeting tests if sending a JSON string requesting a rollcall creation later works 
// by comparing the messages (response and broadcasted answers) sent back, then open it, then closes it
func TestReceivePublishCreateRollCallLater(t *testing.T) {

	_ = os.Remove("org_test.db")

	publicKey, privateKey := createKeyPair()
	creationstr := strconv.FormatInt(time.Now().Unix(), 10)

	receivedMsg := getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateRollCallLater(publicKey,creationstr)))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateRollCallLater(publicKey,creationstr))) 
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`) 

	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
		actor: 			NewOrganizer(string(publicKey), "org_test.db"),
	}

	msgAndChannel, responseToSender := h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", string(msgAndChannel[0].Channel), string(expectedMsgAndChannel[0].Channel))
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	receivedMsg = getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataOpenRollCall(publicKey,creationstr)))
	userId = 5
	expectedMsgAndChannel = getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataOpenRollCall(publicKey,creationstr))) 
	expectedResponseToSender = []byte(`{"jsonrpc":"2.0","result":0,"id":0}`) 

	msgAndChannel, responseToSender = h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
		//t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", string(msgAndChannel[0].Channel), string(expectedMsgAndChannel[0].Channel))
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	receivedMsg = getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCloseRollCall(publicKey,creationstr)))
	userId = 5
	expectedMsgAndChannel = getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCloseRollCall(publicKey,creationstr))) 
	expectedResponseToSender = []byte(`{"jsonrpc":"2.0","result":0,"id":0}`) 

	msgAndChannel, responseToSender = h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
		//t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", string(msgAndChannel[0].Channel), string(expectedMsgAndChannel[0].Channel))
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	_ = os.Remove("org_test.db")
}