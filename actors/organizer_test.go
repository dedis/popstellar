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

func createKeyPair() ([]byte, ed.PrivateKey) {
	//randomize the key
	randomSeed := make([]byte, 32)
	rand.Read(randomSeed)
	privkey := ed.NewKeyFromSeed(randomSeed)
	return privkey.Public().(ed.PublicKey), privkey
}

func getCorrectDataCreateLAO(publicKey []byte) string {
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	tohash := lib.ComputeAsJsonArray([]string{pkeyb64,"1234","my_lao"})
	hashid := sha256.Sum256( []byte(tohash) )
	id := b64.StdEncoding.EncodeToString( hashid[:] )
	data := `{
		"object": "lao",
		"action": "create",
		"id": "`+id+`",
		"name": "my_lao",
		"creation": 1234,
		"organizer": "`+pkeyb64+`",
		"witnesses": {
	
		}
	}`
	// strings.Join(strings.Fields(str), "") remove all white spaces (and tabs, etc) from str
	data = strings.Join(strings.Fields(data), "")
	return data
}

func getCorrectPublishCreateLAO(publicKey []byte, privateKey ed.PrivateKey) []byte {
	data := []byte(getCorrectDataCreateLAO(publicKey))
	datab64 := b64.StdEncoding.EncodeToString(data)
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	signature := ed.Sign(privateKey, []byte(data))
	signatureb64 := b64.StdEncoding.EncodeToString(signature)
	// TODO I think it's weird to hash data in plain and signature in b64, but well, apparently, it's the protocol
	tohash := lib.ComputeAsJsonArray([]string{string(data),string(signatureb64)})
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


func getExpectedMsgAndChannelForPublishCreateLAO(publicKey []byte, privateKey ed.PrivateKey) []lib.MessageAndChannel {
	data := []byte(getCorrectDataCreateLAO(publicKey))
	datab64 := b64.StdEncoding.EncodeToString(data)
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	signature := ed.Sign(privateKey, []byte(data))
	signatureb64 := b64.StdEncoding.EncodeToString(signature)
	tohash := lib.ComputeAsJsonArray([]string{string(data),string(signatureb64)})
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


func TestReceivePublishCreateLAO(t *testing.T) {

	publicKey, privateKey := createKeyPair()

	receivedMsg := getCorrectPublishCreateLAO(publicKey, privateKey)
	userId := 5
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishCreateLAO(publicKey, privateKey)
	var expectedResponseToSender []byte = nil


	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
		actor: 			NewOrganizer(string(publicKey), "org.db"),
	}

	 

	msgAndChannel, responseToSender := h.actor.HandleReceivedMessage(receivedMsg, userId)
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, \n%+v\n vs, \n%+v", msgAndChannel, expectedMsgAndChannel)
	}

	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}
}