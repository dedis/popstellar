package actors

import (
	ed "crypto/ed25519"
	"crypto/sha256"
	b64 "encoding/base64"
	"os"
	"reflect"
	"strconv"
	"strings"
	"student20_pop/lib"
	"testing"
	"time"
)

// The tests in this package have a rather global coverage:
// they indirectly test most of the functions in parser, a good chunk of the function in security, and as well of the
// database functions.
// On the other hand, they admittedly do not try to test every branching path. Currently, the focus is very much on
// testing that correct strings are accepted rather that incorrect strings are rejected.
// This seems a decent trade-off for time-efficiency as our code is quite prone to raising errors.

/////////////////////////////////////////////////////////////////////////////////////////////////////
// getters for data JSON strings

// getCorrectDataCreateLAO generate a example JSON string of the data field of a request for LAO creation
func getCorrectDataCreateLAO(publicKey []byte, creationStr string) string {
	publicKeyBase64 := b64.StdEncoding.EncodeToString(publicKey)
	toHash := lib.ArrayRepresentation([]string{publicKeyBase64, creationStr, "my_lao"})
	hashId := sha256.Sum256([]byte(toHash))
	id := b64.StdEncoding.EncodeToString(hashId[:])
	data := `{
		"object": "lao",
		"action": "create",
		"id": "` + id + `",
		"name": "my_lao",
		"creation": ` + creationStr + `,
		"organizer": "` + publicKeyBase64 + `",
		"witnesses": {
	
		}
	}`
	// strings.Join(strings.Fields(str), "") removes all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataCreateLAO generate a example JSON string of the data field of a request for meeting creation
func getCorrectDataCreateMeeting() string {
	creationString := strconv.FormatInt(time.Now().Unix(), 10)
	startString := strconv.FormatInt(time.Now().Unix()+1000, 10)
	toHash := lib.ArrayRepresentation([]string{"M", b64.StdEncoding.EncodeToString([]byte("LAO_id")), creationString, "my_meeting"})
	hashId := sha256.Sum256([]byte(toHash))
	id := b64.StdEncoding.EncodeToString(hashId[:])
	data := `{
		"object": "meeting",
		"action": "create",
		"id": "` + id + `",
		"name": "my_meeting",
		"creation": ` + creationString + `,
		"location": "here",
		"start": ` + startString + `
	}`
	// strings.Join(strings.Fields(str), "") removes all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataCreateRollCallNow generate a example JSON string of the data field of a request for rollCall creation starting now
func getCorrectDataCreateRollCallNow() string {
	creationString := strconv.FormatInt(time.Now().Unix(), 10)
	startStr := strconv.FormatInt(time.Now().Unix()+1000, 10)
	toHash := lib.ArrayRepresentation([]string{"R", b64.StdEncoding.EncodeToString([]byte("LAO_id")), creationString, "my_roll_call"})
	hashId := sha256.Sum256([]byte(toHash))
	id := b64.StdEncoding.EncodeToString(hashId[:])
	data := `{
		"object": "roll_call",
		"action": "create",
		"id": "` + id + `",
		"name": "my_roll_call",
		"creation": ` + creationString + `,
		"start": ` + startStr + `,
		"location": "here"
	}`
	// strings.Join(strings.Fields(str), "") removes all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataCreateRollCallLater generate a example JSON string of the data field of a request for rollCall creation starting at a scheduled time
func getCorrectDataCreateRollCallLater(creationString string) string {
	startString := strconv.FormatInt(time.Now().Unix()+1000, 10)
	toHash := lib.ArrayRepresentation([]string{"R", b64.StdEncoding.EncodeToString([]byte("LAO_id")), creationString, "my_roll_call"})
	hashId := sha256.Sum256([]byte(toHash))
	id := b64.StdEncoding.EncodeToString(hashId[:])
	data := `{
		"object": "roll_call",
		"action": "create",
		"id": "` + id + `",
		"name": "my_roll_call",
		"creation": ` + creationString + `,
		"scheduled": ` + startString + `,
		"location": "here"
	}`
	// strings.Join(strings.Fields(str), "") removes all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataOpenRollCall generate a example JSON string of the data field of a request for opening a rollCall at a previously scheduled time
func getCorrectDataOpenRollCall(creationString string) string {
	startString := strconv.FormatInt(time.Now().Unix()+1000, 10)
	toHash := lib.ArrayRepresentation([]string{"R", b64.StdEncoding.EncodeToString([]byte("LAO_id")), creationString, "my_roll_call"})
	hashId := sha256.Sum256([]byte(toHash))
	id := b64.StdEncoding.EncodeToString(hashId[:])
	data := `{
		"object": "roll_call",
		"action": "open",
		"id": "` + id + `",
		"start": ` + startString + `
	}`
	// strings.Join(strings.Fields(str), "") removes all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataCloseRollCall generate a example JSON string of the data field of a request for closing a rollCall
func getCorrectDataCloseRollCall(creationString string) string {
	startString := strconv.FormatInt(time.Now().Unix()+1000, 10)
	endString := strconv.FormatInt(time.Now().Unix()+2000, 10)
	toHash := lib.ArrayRepresentation([]string{"R", b64.StdEncoding.EncodeToString([]byte("LAO_id")), creationString, "my_roll_call"})
	hashId := sha256.Sum256([]byte(toHash))
	id := b64.StdEncoding.EncodeToString(hashId[:])
	data := `{
		"object": "roll_call",
		"action": "close",
		"id": "` + id + `",
		"start": ` + startString + `,
		"end": ` + endString + `,
		"attendees": ["1234"] 
	}`
	// TODO maybe check the attendees field for a real public key??
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// getCorrectDataStateLAO generate a example JSON string of the data field of a request for announcing the state of a
// LAO (typically once it has received enough certifications)
func getCorrectDataStateLAO(publicKey []byte, creationString string) string {
	publicKeyB64 := b64.StdEncoding.EncodeToString(publicKey)
	lastModified := strconv.FormatInt(time.Now().Unix(), 10)
	toHash := lib.ArrayRepresentation([]string{publicKeyB64, creationString, "my_lao"})
	hashId := sha256.Sum256([]byte(toHash))
	id := b64.StdEncoding.EncodeToString(hashId[:])
	data := `{
		"object": "lao",
		"action": "state",
		"id": "` + id + `",
		"name": "my_lao",
		"creation": ` + creationString + `,
		"last_modified": ` + lastModified + `,
		"organizer": "` + publicKeyB64 + `",
		"witnesses": {
	
		},
		"modification_id": "` + id + `",
		"modification_signatures": []
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

// TODO test stateMeeting, updateLAO, witnessMessage. write data for stateMeeting, updateLAO

func getCorrectDataWitnessMessage(privateKey ed.PrivateKey, messageId string) string {
	signature := ed.Sign(privateKey, []byte(messageId))
	signatureB64 := b64.StdEncoding.EncodeToString(signature)
	data := `{
		"object": "message",
		"action": "witness",
		"message_id": "` + messageId + `",
		"signature	": "` + signatureB64 + `"
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

/////////////////////////////////////////////////////////////////////////////////////////////////////
// getters for top-level JSON strings

// getCorrectPublishOnRoot generate a example JSON string of the whole request for a publish, based on a data []byte
func getCorrectPublishOnRoot(publicKey []byte, privateKey ed.PrivateKey, data []byte) []byte {
	dataB64 := b64.StdEncoding.EncodeToString(data)
	publicKeyB64 := b64.StdEncoding.EncodeToString(publicKey)
	signature := ed.Sign(privateKey, data)
	signatureB64 := b64.StdEncoding.EncodeToString(signature)
	toHash := lib.ArrayRepresentation([]string{dataB64, signatureB64})
	msgId := sha256.Sum256([]byte(toHash))
	msgIdB64 := b64.StdEncoding.EncodeToString(msgId[:])
	msg := `{
		"jsonrpc": "2.0",
		"method": "publish",
		"params": {
			"channel": "/root",
			"message": {
				"data": "` + dataB64 + `",
				"sender": "` + publicKeyB64 + `",
				"signature": "` + signatureB64 + `",
				"message_id": "` + msgIdB64 + `",
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
	dataB64 := b64.StdEncoding.EncodeToString(data)
	pkeyB64 := b64.StdEncoding.EncodeToString(publicKey)
	signature := ed.Sign(privateKey, data)
	signatureB64 := b64.StdEncoding.EncodeToString(signature)
	toHash := lib.ArrayRepresentation([]string{dataB64, signatureB64})
	msgId := sha256.Sum256([]byte(toHash))
	msgDdB64 := b64.StdEncoding.EncodeToString(msgId[:])
	msg := `{
		"jsonrpc": "2.0",
		"method": "publish",
		"params": {
			"channel": "/root/LAO_id",
			"message": {
				"data": "` + dataB64 + `",
				"sender": "` + pkeyB64 + `",
				"signature": "` + signatureB64 + `",
				"message_id": "` + msgDdB64 + `",
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
	return nil
}

// getExpectedMsgAndChannelForPublishGeneral generate a example JSON string of the whole broadcasted struct sent back
// for LAO creation according to the data field passed in argument
func getExpectedMsgAndChannelForPublishGeneral(publicKey []byte, privateKey ed.PrivateKey, data []byte) []lib.MessageAndChannel {
	dataB64 := b64.StdEncoding.EncodeToString(data)
	pkeyB64 := b64.StdEncoding.EncodeToString(publicKey)
	signature := ed.Sign(privateKey, data)
	signatureB64 := b64.StdEncoding.EncodeToString(signature)
	toHash := lib.ArrayRepresentation([]string{dataB64, signatureB64})
	msgId := sha256.Sum256([]byte(toHash))
	msgIdB64 := b64.StdEncoding.EncodeToString(msgId[:])
	msg := `{
		"jsonrpc": "2.0",
		"method": "broadcast",
		"params": {
			"channel": "/root/LAO_id",
			"message": {
				"data": "` + dataB64 + `",
				"sender": "` + pkeyB64 + `",
				"signature": "` + signatureB64 + `",
				"message_id": "` + msgIdB64 + `",
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
// Tests to run

// TestReceivePublishCreateLAO tests if sending a JSON string requesting to publish a LAO creation works
// by comparing the messages (response and broadcasted answers) sent back
func TestReceivePublishCreateLAO(t *testing.T) {

	publicKey, privateKey := lib.GenerateTestKeyPair()

	creationString := strconv.FormatInt(time.Now().Unix(), 10)
	receivedMsg := getCorrectPublishOnRoot(publicKey, privateKey, []byte(getCorrectDataCreateLAO(publicKey, creationString)))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishOnRoot(publicKey, privateKey, []byte(getCorrectDataCreateLAO(publicKey, creationString))) // which will never be sent, but still produced)
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`)

	org := NewOrganizer(string(publicKey), "org_test.db")

	msgAndChannel, responseToSender := org.HandleReceivedMessage(receivedMsg, userId)
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

	publicKey, privateKey := lib.GenerateTestKeyPair()

	creationString := strconv.FormatInt(time.Now().Unix(), 10)
	receivedMsg := getCorrectPublishOnRoot(publicKey, privateKey, []byte(getCorrectDataCreateLAO(publicKey, creationString)))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishOnRoot(publicKey, privateKey, []byte(getCorrectDataCreateLAO(publicKey, creationString))) // which will never be sent, but still produced)
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`)

	org := NewOrganizer(string(publicKey), "org_test.db")

	msgAndChannel, responseToSender := org.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	// An organizer back end should never receive a publishStateLao
	receivedMsg = getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataStateLAO(publicKey, creationString)))
	userId = 5
	expectedMsgAndChannel = nil
	expectedResponseToSender = []byte(`{"jsonrpc":"2.0","error":{"code":-1,"description":"invalid action"},"id":0}`)

	msgAndChannel, responseToSender = org.HandleReceivedMessage(receivedMsg, userId)
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

	publicKey, privateKey := lib.GenerateTestKeyPair()

	receivedMsg := getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateMeeting()))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateMeeting()))
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`)

	org := NewOrganizer(string(publicKey), "org_test.db")

	msgAndChannel, responseToSender := org.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", string(msgAndChannel[0].Channel), string(expectedMsgAndChannel[0].Channel))
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}
	_ = os.Remove("org_test.db")
}

// TestReceivePublishCreateRollCallNow tests if sending a JSON string requesting a rollCall creation starting now works
// by comparing the messages (response and broadcasted answers) sent back
func TestReceivePublishCreateRollCallNow(t *testing.T) {

	publicKey, privateKey := lib.GenerateTestKeyPair()

	receivedMsg := getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateRollCallNow()))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateRollCallNow()))
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`)

	org := NewOrganizer(string(publicKey), "org_test.db")

	msgAndChannel, responseToSender := org.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", string(msgAndChannel[0].Channel), string(expectedMsgAndChannel[0].Channel))
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}
	_ = os.Remove("org_test.db")
}

// TestReceivePublishCreateMeeting tests if sending a JSON string requesting a rollCall creation later works
// by comparing the messages (response and broadcasted answers) sent back, then open it, then closes it
func TestReceivePublishCreateRollCallLater(t *testing.T) {

	_ = os.Remove("org_test.db")

	publicKey, privateKey := lib.GenerateTestKeyPair()
	creationString := strconv.FormatInt(time.Now().Unix(), 10)

	receivedMsg := getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateRollCallLater(creationString)))
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCreateRollCallLater(creationString)))
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`)

	org := NewOrganizer(string(publicKey), "org_test.db")

	msgAndChannel, responseToSender := org.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", string(msgAndChannel[0].Channel), string(expectedMsgAndChannel[0].Channel))
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	receivedMsg = getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataOpenRollCall(creationString)))
	userId = 5
	expectedMsgAndChannel = getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataOpenRollCall(creationString)))
	expectedResponseToSender = []byte(`{"jsonrpc":"2.0","result":0,"id":0}`)

	msgAndChannel, responseToSender = org.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	receivedMsg = getCorrectPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCloseRollCall(creationString)))
	userId = 5
	expectedMsgAndChannel = getExpectedMsgAndChannelForPublishGeneral(publicKey, privateKey, []byte(getCorrectDataCloseRollCall(creationString)))
	expectedResponseToSender = []byte(`{"jsonrpc":"2.0","result":0,"id":0}`)

	msgAndChannel, responseToSender = org.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	_ = os.Remove("org_test.db")
}
