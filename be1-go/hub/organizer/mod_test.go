package organizer

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"popstellar/channel"
	"popstellar/crypto"
	jsonrpc "popstellar/message"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
)

func TestOrganizer_Create_LAO(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, nolog, fakeChannelFac.newChannel)
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

// Check that if the organizer receives a publish message, it will call the
// publish function on the appropriate channel.
func TestOrganizer_Handle_Publish(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID[rootPrefix+laoID] = c

	msg := message.Message{
		Data:              base64.URLEncoding.EncodeToString([]byte("XXX")),
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         base64.URLEncoding.EncodeToString([]byte("XXX")),
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
}

// Check that if the organizer receives a subscribe message, it will call the
// subscribe function on the appropriate channel.
func TestOrganizer_Handle_Subscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, nolog, nil)
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
}

// Check that if the organizer receives an unsubscribe message, it will call the
// unsubscribe function on the appropriate channel.
func TestOrganizer_Handle_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, nolog, nil)
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

	hub, err := NewHub(keypair.public, nolog, nil)
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
}

// -----------------------------------------------------------------------------
// Utility functions

type keypair struct {
	public    kyber.Point
	publicBuf []byte
	private   kyber.Scalar
}

var nolog = zerolog.New(io.Discard)
var suite = crypto.Suite

func generateKeyPair(t *testing.T) keypair {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Pick(suite.RandomStream())
	point = point.Mul(secret, point)

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
func (c *fakeChannelFac) newChannel(channelID string,
	hub channel.HubFunctionalities, msg message.Message, log zerolog.Logger) channel.Channel {

	c.chanID = channelID
	c.msg = msg
	return c.c
}

// fakeChannel is a fake implementation of a channel
//
// - implements channel.Channel
type fakeChannel struct {
	subscribe   method.Subscribe
	unsubscribe method.Unsubscribe
	publish     method.Publish
	catchup     method.Catchup

	// set by the subscribe
	socket socket.Socket
	// set by the unsubscribe
	socketID string

	// fake messages to return in a catchup
	msgs []message.Message
}

// Subscribe implements channel.Channel
func (f *fakeChannel) Subscribe(socket socket.Socket, msg method.Subscribe) error {
	f.socket = socket
	f.subscribe = msg
	return nil
}

// Unsubscribe implements channel.Channel
func (f *fakeChannel) Unsubscribe(socketID string, msg method.Unsubscribe) error {
	f.socketID = socketID
	f.unsubscribe = msg
	return nil
}

// Publish implements channel.Channel
func (f *fakeChannel) Publish(msg method.Publish) error {
	f.publish = msg
	return nil
}

// Catchup implements channel.Channel
func (f *fakeChannel) Catchup(msg method.Catchup) []message.Message {
	f.catchup = msg
	return f.msgs
}

// fakeSocket is a fake implementation of a socket
//
// - implements socket.Socket
type fakeSocket struct {
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
	f.msg = msg
}

// SendResult implements socket.Socket
func (f *fakeSocket) SendResult(id int, res []message.Message) {
	f.resultID = id
	f.res = res
}

// SendError implements socket.Socket
func (f *fakeSocket) SendError(id *int, err error) {
	f.err = err
}

func (f *fakeSocket) ID() string {
	return f.id
}
