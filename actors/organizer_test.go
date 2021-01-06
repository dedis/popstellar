package actors

import (
	"testing"
	"student20_pop/lib"
	"reflect"
	"student20_pop/actors"
	"sync"
	ed "crypto/ed25519"
	"crypto/sha256"
	"math/rand"
	b64 "encoding/base64"
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

func getCorrectDataCreateLAO(publicKey []byte) string {
	pkeyb64 := b64.StdEncoding.EncodeToString(publicKey)
	hashid := sha256.Sum256( []byte(`["123","`+pkeyb64+`","my_lao"]`) )
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
	return data
}

func getCorrectPublishCreateLAO(publicKey []byte, privateKey ed.PrivateKey) []byte {
	data := b64.StdEncoding.EncodeToString([]byte(getCorrectDataCreateLAO(publicKey)))
	signature := ed.Sign(privateKey, []byte(data))
	signatureb64 := b64.StdEncoding.EncodeToString(signature)
	msgid := sha256.Sum256( []byte(`["`+data+`","`+string(signature)+`"]`))
	msgidb64 := b64.StdEncoding.EncodeToString(msgid[:])
	msg := `{
		"jsonrpc": "2.0",
		"method": "publish",
		"params": {
			"channel": "/root",
			"message": {
				"data": "`+data+`",
				"sender": "MTIz",
				"signature": "`+signatureb64+`",
				"message_id": "`+msgidb64+`",
				"witness_signatures": {
	
				}
			}
		},
		"id": 0
	}`
	// MTIz is b64encoded 123
	return []byte(msg)
} 


func getExpectedMsgAndChannelForPublishCreateLAO(publicKey []byte, privateKey ed.PrivateKey) []lib.MessageAndChannel {
	data := b64.StdEncoding.EncodeToString([]byte(getCorrectDataCreateLAO(publicKey)))
	signature := ed.Sign(privateKey, []byte(data))
	signatureb64 := b64.StdEncoding.EncodeToString(signature)
	msgid := sha256.Sum256( []byte(`["`+data+`","`+string(signature)+`"]`))
	msgidb64 := b64.StdEncoding.EncodeToString(msgid[:])
	msg := `{
		"jsonrpc": "2.0",
		"method": "broadcast",
		"params": {
			"channel": "/root",
			"message": {
				"data": "`+data+`",
				"sender": "MTIz",
				"signature": "`+signatureb64+`",
				"message_id": "`+msgidb64+`",
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