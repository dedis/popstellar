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
	creationstr := strconv.FormatInt(time.Now().Unix(), 10)
	tohash := lib.ComputeAsJsonArray([]string{string(publicKey),creationstr,"my_lao"})
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
	// TODO I think it's weird to hash data in plain and signature in b64, but well, apparently, it's the protocol
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
	expectedMsgAndChannel := getExpectedMsgAndChannelForPublishCreateLAO(publicKey, privateKey) // which will never be sent, but still produced)
	expectedResponseToSender := []byte(`{"jsonrpc":"2.0","result":0,"id":0}`) 


	h := &hub{
		connectionsMx:   sync.RWMutex{},
		receivedMessage: make(chan []byte),
		connections:     make(map[*connection]struct{}),
		connIndex:       0,
		idOfSender:      -1,
		actor: 			NewOrganizer(string(publicKey), "org.db"),
	}

	 

	msgAndChannel, responseToSender := h.actor.HandleReceivedMessage(receivedMsg, userId)
	// TODO this comparison fails only because of capital letters, which we need to export the fields... and I don't think actually matter in json
	if !reflect.DeepEqual(msgAndChannel, expectedMsgAndChannel) {
		t.Errorf("correct msgAndChannel are not as expected, got :\n %v\n vs expected:\n%v", string(msgAndChannel[0].Message), string(expectedMsgAndChannel[0].Message))
	}

	// TODO omitempty on Result  int    `json:"result,omitempty"` cause marshall to not add the field, to correct after discussion
	if !reflect.DeepEqual(responseToSender, expectedResponseToSender) {
		t.Errorf("correct structs are not as expected, \n%v\n vs, \n%v", string(responseToSender), string(expectedResponseToSender))
	}
}