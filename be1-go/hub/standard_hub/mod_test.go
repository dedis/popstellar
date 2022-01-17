package standard_hub

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"popstellar/channel"
	"popstellar/hub"
	jsonrpc "popstellar/message"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"sync"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
)

func Test_Add_Server_Socket(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	sock := &fakeSocket{id: "fakeID"}

	err = hub.NotifyNewServer(sock)
	require.NoError(t, err)
	require.NotNil(t, hub.queries.queries[0])
	require.Equal(t, 1, hub.queries.nextID)
}

func Test_Create_LAO(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, nolog, fakeChannelFac.newChannel, hub.OrganizerHubType)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	h := sha256.New()
	h.Write(keypair.publicBuf)
	h.Write([]byte(fmt.Sprintf("%d", now)))
	h.Write([]byte(name))

	laoID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	data := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  123,
		Organizer: base64.URLEncoding.EncodeToString([]byte("XXX")),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	msg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(dataBuf),
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         base64.URLEncoding.EncodeToString(signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: "/root",
			Message: msg,
		},
	}

	publishBuf, err := json.Marshal(&publish)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	require.Equal(t, publish.ID, sock.resultID)

	// > we are expecting the lao channel factor be called with the right
	// arguments.
	require.Equal(t, rootPrefix+data.ID, fakeChannelFac.chanID)
	require.Equal(t, msg.Data, fakeChannelFac.msg.Data)
	require.Equal(t, msg.MessageID, fakeChannelFac.msg.MessageID)
	require.Equal(t, msg.Sender, fakeChannelFac.msg.Sender)
	require.Equal(t, msg.Signature, fakeChannelFac.msg.Signature)
	require.Equal(t, msg.WitnessSignatures, fakeChannelFac.msg.WitnessSignatures)

	// > the organizer should have saved the channel locally

	require.Contains(t, hub.channelByID, rootPrefix+data.ID)
	require.Equal(t, fakeChannelFac.c, hub.channelByID[rootPrefix+data.ID])
}

func Test_Wrong_Root_Publish(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	laoID := "/root"

	hub.channelByID[rootPrefix+laoID] = c

	data := messagedata.LaoState{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      "channel0",
		Creation:  123,
		Organizer: base64.URLEncoding.EncodeToString([]byte("XXX")),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	msg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(dataBuf),
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         base64.URLEncoding.EncodeToString(signature),
		WitnessSignatures: []message.WitnessSignature{},
	}
	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: rootPrefix + laoID,
			Message: msg,
		},
	}

	publishBuf, err := json.Marshal(&publish)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.Error(t, sock.err, "only lao#create is allowed on root, but found %s#%s", data.Object, data.Action)

	// > check that there is no errors with messages from witness too
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.Error(t, sock.err, "only lao#create is allowed on root, but found %s#%s", data.Object, data.Action)
}

func Test_Handle_Server_Catchup(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	serverCatchup := method.Catchup{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodCatchUp,
		},

		ID: 1,

		Params: struct {
			Channel string `json:"channel"`
		}{
			Channel: "/root",
		},
	}

	publishBuf, err := json.Marshal(serverCatchup)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.NoError(t, sock.err)
	require.Equal(t, serverCatchup.ID, sock.resultID)
}

func Test_Handle_Answer(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, nolog, fakeChannelFac.newChannel, hub.OrganizerHubType)
	require.NoError(t, err)

	result := struct {
		JSONRPC string `json:"jsonrpc"`
		ID      int    `json:"id"`
		Result  int    `json:"result"`
	}{
		JSONRPC: "2.0",
		ID:      1,
		Result:  0,
	}

	serverAnswer := struct {
		JSONRPC string            `json:"jsonrpc"`
		ID      int               `json:"id"`
		Result  []message.Message `json:"result"`
	}{
		JSONRPC: "2.0",
		ID:      1,
		Result:  make([]message.Message, 1),
	}
	messageDataPath := filepath.Join("..", "..", "..", "protocol",
		"examples", "messageData", "lao_create", "lao_create.json")

	messageDataBuf, err := os.ReadFile(messageDataPath)
	require.NoError(t, err)

	messageData := base64.URLEncoding.EncodeToString(messageDataBuf)

	msg := message.Message{
		Data:              messageData,
		Sender:            publicKey64,
		WitnessSignatures: []message.WitnessSignature{},
	}
	serverAnswer.Result[0] = msg

	serverAnswerBis := struct {
		JSONRPC string            `json:"jsonrpc"`
		ID      int               `json:"id"`
		Result  []message.Message `json:"result"`
	}{
		JSONRPC: "2.0",
		ID:      2,
		Result:  make([]message.Message, 0),
	}

	resultBuf, err := json.Marshal(result)
	require.NoError(t, err)

	answerBuf, err := json.Marshal(serverAnswer)
	require.NoError(t, err)

	answerBisBuf, err := json.Marshal(serverAnswerBis)
	require.NoError(t, err)

	queryState := false
	hub.queries.state[1] = &queryState
	hub.queries.queries[1] = method.Catchup{
		Params: struct {
			Channel string "json:\"channel\""
		}{
			Channel: "/root",
		},
	}

	sock := &fakeSocket{}

	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: resultBuf,
	})
	require.Error(t, sock.err, "rpc message sent by a client should be a query")
	sock.err = nil
	require.False(t, queryState)

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: resultBuf,
	})
	require.NoError(t, sock.err)
	require.False(t, queryState)

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: answerBuf,
	})
	require.NoError(t, sock.err)
	require.True(t, queryState)

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: answerBuf,
	})
	require.Error(t, sock.err, "query %v already got an answer", serverAnswer.ID)
	sock.err = nil

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: answerBisBuf,
	})
	require.Error(t, sock.err, "no query sent with id %v", serverAnswerBis.ID)
}

// Check that if the server receives a publish message, it will call the
// publish function on the appropriate channel.
func Test_Handle_Publish(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID[rootPrefix+laoID] = c

	signature, err := schnorr.Sign(suite, keypair.private, []byte("XXX"))
	require.NoError(t, err)

	msg := message.Message{
		Data:              base64.URLEncoding.EncodeToString([]byte("XXX")),
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         base64.URLEncoding.EncodeToString(signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: rootPrefix + laoID,
			Message: msg,
		},
	}

	publishBuf, err := json.Marshal(&publish)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.NoError(t, sock.err)
	require.Equal(t, publish.ID, sock.resultID)

	// > check that the channel has been called with the publish message
	require.Equal(t, publish, c.publish)

	// > check that there is no errors with messages from witness too
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.NoError(t, sock.err)
	require.Equal(t, publish.ID, sock.resultID)

	// > check that the channel has been called with the publish message
	require.Equal(t, publish, c.publish)
}

// Check that if the server receives a broadcast message, it will call the
// broadcast function on the appropriate channel.
func Test_Handle_Broadcast(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID[rootPrefix+laoID] = c

	signature, err := schnorr.Sign(suite, keypair.private, []byte("XXX"))
	require.NoError(t, err)

	msg := message.Message{
		Data:              base64.URLEncoding.EncodeToString([]byte("XXX")),
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         base64.URLEncoding.EncodeToString(signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	broadcast := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodBroadcast,
		},

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: rootPrefix + laoID,
			Message: msg,
		},
	}

	broadcastBuf, err := json.Marshal(&broadcast)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: broadcastBuf,
	})

	// Check the socket
	require.Error(t, sock.err, "unexpected method: 'broadcast'")

	// Emtpy the socket
	sock.err = nil

	// Check that there is no errors with messages from witness too
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: broadcastBuf,
	})

	// Check the socket
	require.NoError(t, sock.err)
	require.Equal(t, 0, sock.resultID)

	// Check that the channel has been called with the publish message
	require.Equal(t, broadcast, c.broadcast)
}

// Test that a LAO is correctly created when receiveing a broadcast message
func Test_Create_LAO_Broadcast(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, nolog, fakeChannelFac.newChannel, hub.OrganizerHubType)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	h := sha256.New()
	h.Write(keypair.publicBuf)
	h.Write([]byte(fmt.Sprintf("%d", now)))
	h.Write([]byte(name))

	laoID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	data := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  123,
		Organizer: base64.URLEncoding.EncodeToString([]byte("XXX")),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	msg := message.Message{
		Data:              base64.URLEncoding.EncodeToString(dataBuf),
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         base64.URLEncoding.EncodeToString(signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	broadcast := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodBroadcast,
		},

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: "/root",
			Message: msg,
		},
	}

	publishBuf, err := json.Marshal(&broadcast)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	require.Equal(t, 0, sock.resultID)

	// > we are expecting the lao channel factor be called with the right
	// arguments.
	require.Equal(t, rootPrefix+data.ID, fakeChannelFac.chanID)
	require.Equal(t, msg.Data, fakeChannelFac.msg.Data)
	require.Equal(t, msg.MessageID, fakeChannelFac.msg.MessageID)
	require.Equal(t, msg.Sender, fakeChannelFac.msg.Sender)
	require.Equal(t, msg.Signature, fakeChannelFac.msg.Signature)
	require.Equal(t, msg.WitnessSignatures, fakeChannelFac.msg.WitnessSignatures)

	// > the organizer should have saved the channel locally

	require.Contains(t, hub.channelByID, rootPrefix+data.ID)
	require.Equal(t, fakeChannelFac.c, hub.channelByID[rootPrefix+data.ID])
}

// Check that if the organizer receives a subscribe message, it will call the
// subscribe function on the appropriate channel.
func Test_Handle_Subscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID[rootPrefix+laoID] = c

	subscribe := method.Subscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodSubscribe,
		},

		ID: 1,

		Params: struct {
			Channel string `json:"channel"`
		}{
			Channel: rootPrefix + laoID,
		},
	}

	publishBuf, err := json.Marshal(&subscribe)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.NoError(t, sock.err)
	require.Equal(t, subscribe.ID, sock.resultID)

	// > check that the channel has been called with the publish message
	require.Equal(t, subscribe, c.subscribe)

	// > check that there is no errors with messages from witness too
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.NoError(t, sock.err)
	require.Equal(t, subscribe.ID, sock.resultID)

	// > check that the channel has been called with the publish message
	require.Equal(t, subscribe, c.subscribe)
}

// Check that if the organizer receives an unsubscribe message, it will call the
// unsubscribe function on the appropriate channel.
func TestOrganizer_Handle_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID[rootPrefix+laoID] = c

	unsubscribe := method.Unsubscribe{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodUnsubscribe,
		},

		ID: 1,

		Params: struct {
			Channel string `json:"channel"`
		}{
			Channel: rootPrefix + laoID,
		},
	}

	publishBuf, err := json.Marshal(&unsubscribe)
	require.NoError(t, err)

	sock := &fakeSocket{id: "fakeID"}

	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.NoError(t, sock.err)
	require.Equal(t, unsubscribe.ID, sock.resultID)

	// > check that the channel has been called with the publish message
	require.Equal(t, unsubscribe, c.unsubscribe)
	require.Equal(t, sock.id, c.socketID)

	// > check that there is no errors with messages from witness too
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.NoError(t, sock.err)
	require.Equal(t, unsubscribe.ID, sock.resultID)

	// > check that the channel has been called with the publish message
	require.Equal(t, unsubscribe, c.unsubscribe)
	require.Equal(t, sock.id, c.socketID)
}

// Check that if the organizer receives a catchup message, it will call the
// catchup function on the appropriate channel.
func TestOrganizer_Handle_Catchup(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeMessages := []message.Message{
		{
			MessageID: "XXX",
		},
	}

	// set fake messages on the channel
	c := &fakeChannel{
		msgs: fakeMessages,
	}

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID[rootPrefix+laoID] = c

	catchup := method.Catchup{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodCatchUp,
		},

		ID: 1,

		Params: struct {
			Channel string `json:"channel"`
		}{
			Channel: rootPrefix + laoID,
		},
	}

	publishBuf, err := json.Marshal(&catchup)
	require.NoError(t, err)

	sock := &fakeSocket{id: "fakeID"}

	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.NoError(t, sock.err)
	require.Equal(t, catchup.ID, sock.resultID)

	// > check that the channel has been called with the publish message
	require.Equal(t, catchup, c.catchup)
	require.Equal(t, fakeMessages, c.msgs)

	// > check that there is no errors with messages from witness too
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// > check the socket
	require.NoError(t, sock.err)
	require.Equal(t, catchup.ID, sock.resultID)

	// > check that the channel has been called with the publish message
	require.Equal(t, catchup, c.catchup)
	require.Equal(t, fakeMessages, c.msgs)
}

// Test that the GetServerNumber works
func Test_Get_Server_Number(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	sock1 := &fakeSocket{id: "fakeID1"}
	sock2 := &fakeSocket{id: "fakeID2"}
	sock3 := &fakeSocket{id: "fakeID3"}

	hub.serverSockets.Upsert(sock1)
	hub.serverSockets.Upsert(sock2)
	hub.serverSockets.Upsert(sock3)

	require.Equal(t, 4, hub.GetServerNumber())
}

// Test that SendAndHandleMessage works
func Test_Send_And_Handle_Message(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, nolog, nil, hub.OrganizerHubType)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID[rootPrefix+laoID] = c

	signature, err := schnorr.Sign(suite, keypair.private, []byte("XXX"))
	require.NoError(t, err)

	msg := message.Message{
		Data:              base64.URLEncoding.EncodeToString([]byte("XXX")),
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         base64.URLEncoding.EncodeToString(signature),
		WitnessSignatures: []message.WitnessSignature{},
	}

	publish := method.Publish{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: rootPrefix + laoID,
			Message: msg,
		},
	}

	broadcast := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodBroadcast,
		},

		Params: struct {
			Channel string          "json:\"channel\""
			Message message.Message "json:\"message\""
		}{
			Channel: rootPrefix + laoID,
			Message: msg,
		},
	}

	broadcastBuf, err := json.Marshal(&broadcast)
	require.NoError(t, err)

	sock := &fakeSocket{}
	hub.serverSockets.Upsert(sock)

	err = hub.SendAndHandleMessage(publish)
	require.NoError(t, err)

	// wait for the goroutine created by the function
	time.Sleep(100 * time.Millisecond)

	// Check the socket. The message is a broadcast message because it will be
	// broadcast before the check
	sock.Lock()
	require.Equal(t, broadcastBuf, sock.msg)
	sock.Unlock()

	// > check that the channel has been called with the publish message
	c.Lock()
	require.Equal(t, publish, c.publish)
	c.Unlock()
}

// -----------------------------------------------------------------------------
// Utility functions

type keypair struct {
	public    kyber.Point
	publicBuf []byte
	private   kyber.Scalar
}

var nolog = zerolog.New(io.Discard)

//var suite = crypto.Suite

func generateKeyPair(t *testing.T) keypair {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	pkbuf, err := point.MarshalBinary()
	require.NoError(t, err)

	return keypair{point, pkbuf, secret}
}

// fakeChannelFac implements a channel.LaoFactory function. It takes care
// of keeping what has been provided to that function for check in the tests.
type fakeChannelFac struct {
	chanID string
	msg    message.Message
	c      channel.Channel
	log    zerolog.Logger
}

// newChannel implement the type channel.LaoFactory
func (c *fakeChannelFac) newChannel(channelID string, hub channel.HubFunctionalities,
	msg message.Message, log zerolog.Logger, organizerKey kyber.Point, socket socket.Socket) channel.Channel {

	c.chanID = channelID
	c.msg = msg
	c.log = log
	return c.c
}

// fakeChannel is a fake implementation of a channel
//
// - implements channel.Channel
type fakeChannel struct {
	sync.Mutex

	subscribe   method.Subscribe
	unsubscribe method.Unsubscribe
	publish     method.Publish
	catchup     method.Catchup
	broadcast   method.Broadcast

	// set by the subscribe
	socket socket.Socket
	// set by the unsubscribe
	socketID string

	// fake messages to return in a catchup
	msgs []message.Message
}

// Subscribe implements channel.Channel
func (f *fakeChannel) Subscribe(socket socket.Socket, msg method.Subscribe) error {
	f.Lock()
	defer f.Unlock()

	f.socket = socket
	f.subscribe = msg
	return nil
}

// Unsubscribe implements channel.Channel
func (f *fakeChannel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	f.Lock()
	defer f.Unlock()

	f.socketID = socketID
	f.unsubscribe = msg
	return nil
}

// Publish implements channel.Channel
func (f *fakeChannel) Publish(msg method.Publish, socket socket.Socket) error {
	f.Lock()
	defer f.Unlock()

	f.publish = msg
	return nil
}

// Catchup implements channel.Channel
func (f *fakeChannel) Catchup(msg method.Catchup) []message.Message {
	f.Lock()
	defer f.Unlock()

	f.catchup = msg
	return f.msgs
}

// Broadcast implements channel.Channel
func (f *fakeChannel) Broadcast(msg method.Broadcast, _ socket.Socket) error {
	f.Lock()
	defer f.Unlock()

	f.broadcast = msg
	return nil
}

// fakeSocket is a fake implementation of a socket
//
// - implements socket.Socket
type fakeSocket struct {
	sync.Mutex
	socket.Socket

	resultID int
	res      []message.Message
	msg      []byte

	err error

	// the socket ID
	id string
}

// Send implements socket.Socket
func (f *fakeSocket) Send(msg []byte) {
	f.Lock()
	defer f.Unlock()

	f.msg = msg
}

// SendResult implements socket.Socket
func (f *fakeSocket) SendResult(id int, res []message.Message) {
	f.Lock()
	defer f.Unlock()

	f.resultID = id
	f.res = res
}

// SendError implements socket.Socket
func (f *fakeSocket) SendError(id *int, err error) {
	f.Lock()
	defer f.Unlock()

	f.err = err
}

func (f *fakeSocket) ID() string {
	f.Lock()
	defer f.Unlock()

	return f.id
}

func (f *fakeSocket) Type() socket.SocketType {
	return socket.ClientSocketType
}
