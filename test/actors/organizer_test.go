package actors

import (
	"testing"
	"student20_pop/lib"
	"reflect"
	"student20_pop/actors"
	"sync"
	ed "crypto/ed25519"
	"math/rand"
)

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

	actor actors.Actor

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

func createKeyPair() ([]byte, ed.PrivateKey) {
	//randomize the key
	randomSeed := make([]byte, 32)
	rand.Read(randomSeed)
	privkey := ed.NewKeyFromSeed(randomSeed)
	return privkey.Public().(ed.PublicKey), privkey
}

func getCorrectPublishCreateLAO() []byte {
	msg := `{
		"jsonrpc": "2.0",
		"method": "publish",
		"params": {
			"channel": "/root",
			"message": {
				"data": "ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogIllUSTVOVFk0TjJNNE1UUTBObVU1WVRJeE1USTNZbU5sTldaaU5ERTRNakJpWkRZNE9HTXlNVEl3WWpNM09HTTBPV1E1TW1RNE56Tm1aV05pTlRVNU9BPT0iLAogICAgIm5hbWUiOiAibXlfbGFvIiwKICAgICJjcmVhdGlvbiI6IDEyMzQsCiAgICAib3JnYW5pemVyIjogIk1USXoiLAogICAgIndpdG5lc3NlcyI6IHsKCiAgICB9Cn0=",
				"sender": "MTIz",
				"signature": "TUVZQ0lRRHdDUFdDcGx0Z1gzVWZCWk5HbVpqQzZLUVh6N2RkLzJvWHZwT3dHaWJSTXdJaEFQVGlBOWJ5aXA2YmZNaVdEemZQS0Q4OW83blNIeEJ4OGtvWVBKMWM1T3pr",
				"message_id": "OWQ3ZDVmNjFkNDlhYzc5NTE5M2NlMjlmYTRjZTU4MTRlZWUxOTRmY2M4OWFjYzZiMmUyMzNmYjk1ZmMwN2Q5Zg==",
				"witness_signatures": {
	
				}
			}
		},
		"id": 0
	}`
	return []byte(msg)
} 


func getExpectedMsgAndChannelForPublishCreateLAO() []lib.MessageAndChannel {
	msg := `{
		"jsonrpc": "2.0",
		"method": "broadcast",
		"params": {
			"channel": "/root",
			"message": {
				"data": "ewogICAgIm9iamVjdCI6ICJsYW8iLAogICAgImFjdGlvbiI6ICJjcmVhdGUiLAogICAgImlkIjogIllUSTVOVFk0TjJNNE1UUTBObVU1WVRJeE1USTNZbU5sTldaaU5ERTRNakJpWkRZNE9HTXlNVEl3WWpNM09HTTBPV1E1TW1RNE56Tm1aV05pTlRVNU9BPT0iLAogICAgIm5hbWUiOiAibXlfbGFvIiwKICAgICJjcmVhdGlvbiI6IDEyMzQsCiAgICAib3JnYW5pemVyIjogIk1USXoiLAogICAgIndpdG5lc3NlcyI6IHsKCiAgICB9Cn0=",
				"sender": "MTIz",
				"signature": "TUVZQ0lRRHdDUFdDcGx0Z1gzVWZCWk5HbVpqQzZLUVh6N2RkLzJvWHZwT3dHaWJSTXdJaEFQVGlBOWJ5aXA2YmZNaVdEemZQS0Q4OW83blNIeEJ4OGtvWVBKMWM1T3pr",
				"message_id": "OWQ3ZDVmNjFkNDlhYzc5NTE5M2NlMjlmYTRjZTU4MTRlZWUxOTRmY2M4OWFjYzZiMmUyMzNmYjk1ZmMwN2Q5Zg==",
				"witness_signatures": {
	
				}
			}
		},
		"id": 0
	}`
	answer := []lib.MessageAndChannel{{
		Message: []byte(msg),
		Channel: []byte("/root"),
	}}
	return answer
}


func TestReceivePublishCreateLAO(t *testing.T) {
	receivedMsg := getCorrectPublishCreateLAO()
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishCreateLAO()
	var expectedResponseToSender []byte = nil

	publicKey, _ := createKeyPair()

	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
		actor: 			actors.NewOrganizer(string(publicKey), "org.db"),
	}

	 

	msgAndChannel, responseToSender := h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}
}