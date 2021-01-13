package actors

import (
	"os"
	"reflect"
	"strings"
	"student20_pop/lib"
	"testing"
)

// getCorrectSubscribeGeneral generate a example JSON string of a request to subscribe to a channel
func getCorrectSubscribeGeneral() []byte {
	msg := `{
		"jsonrpc": "2.0",
		"method": "subscribe",
		"params": {
			"channel": "/root/LAO_id"
		},
		"id": 123
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	msg = strings.Join(strings.Fields(msg), "")
	return []byte(msg)
}

// getCorrectunSubscribeGeneral generate a example JSON string of a request to subscribe to a channel
func getCorrectUnSubscribeGeneral() []byte {
	msg := `{
		"jsonrpc": "2.0",
		"method": "unsubscribe",
		"params": {
			"channel": "/root/LAO_id"
		},
		"id": 123
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	msg = strings.Join(strings.Fields(msg), "")
	return []byte(msg)
}

// TestReceiveSubscribeUnsubscribe tests if sending a JSON string requesting to publish a LAO creation works
// by comparing the messages (response and broadcasted answers) sent back
func TestReceiveSubscribeUnsubscribe(t *testing.T) {

	publicKey, _ := createKeyPair()

	receivedMsg := getCorrectSubscribeGeneral()
	userId := 5
	var expectedMsgAndChannel []lib.MessageAndChannel = nil
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":123}`)

	org := NewOrganizer(string(publicKey), "org_test.db")

	msgAndChannel, responseToSender := org.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	// Check current state
	oneSub := org.GetSubscribers("/root/LAO_id")
	noSub1 := org.GetSubscribers("/root/nobody")

	correctOneSub := []int{5}

	if !reflect.DeepEqual(oneSub, correctOneSub) {
		t.Errorf("not correctly subscribed")
	}
	if len(noSub1) != 0 {
		t.Errorf("should not have sub")
	}

	// And then unsubscribe!
	receivedMsg = getCorrectUnSubscribeGeneral()
	msgAndChannel, responseToSender = org.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}

	noSub2 := org.GetSubscribers("/root/LAO_id")
	if len(noSub2) != 0 {
		t.Errorf("should not have sub")
	}

	_ = os.Remove("org_test.db")
}
