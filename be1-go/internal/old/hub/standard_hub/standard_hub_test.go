package standard_hub

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	jsonrpc "popstellar/internal/handler/jsonrpc/mjsonrpc"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata/lao/mlao"
	"popstellar/internal/handler/messagedata/root/mroot"
	"popstellar/internal/handler/method/broadcast/mbroadcast"
	"popstellar/internal/handler/method/catchup/mcatchup"
	"popstellar/internal/handler/method/getmessagesbyid/mgetmessagesbyid"
	"popstellar/internal/handler/method/greetserver/mgreetserver"
	"popstellar/internal/handler/method/heartbeat/mheartbeat"
	"popstellar/internal/handler/method/publish/mpublish"
	"popstellar/internal/handler/method/subscribe/msubscribe"
	method2 "popstellar/internal/handler/method/unsubscribe/munsubscribe"
	"popstellar/internal/handler/query/mquery"
	"popstellar/internal/network/socket"
	"popstellar/internal/old/channel"
	"sync"
	"testing"
	"time"

	"golang.org/x/exp/slices"

	"github.com/stretchr/testify/assert"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
)

func Test_Add_Server_Socket(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	sock := &fakeSocket{id: "fakeID"}

	hub.NotifyNewServer(sock)
	require.Equal(t, 1, hub.serverSockets.Len())
}

func Test_Create_LAO_Bad_Key(t *testing.T) {
	keypair := generateKeyPair(t)
	wrongKeypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{c: &fakeChannel{}}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(base64.URLEncoding.EncodeToString(wrongKeypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(wrongKeypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, wrongKeypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(wrongKeypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         mmessage.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	assert.Contains(t, sock.err.Error(), "access denied: sender's public key does not match the owner's")
}

func Test_Create_LAO_Different_Sender_And_Organizer_Keys(t *testing.T) {
	keypair := generateKeyPair(t)
	wrongKeypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{c: &fakeChannel{}}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, wrongKeypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(wrongKeypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         mmessage.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	assert.Contains(t, sock.err.Error(), "access denied: sender's public key does not match the organizer field")
}

func Test_Create_LAO_No_Key(t *testing.T) {
	wrongKeypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{c: &fakeChannel{}}

	hub, err := NewHub(nil, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(base64.URLEncoding.EncodeToString(wrongKeypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(wrongKeypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, wrongKeypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(wrongKeypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         mmessage.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	assert.NoError(t, sock.err)
}

func Test_Create_LAO_Bad_MessageID(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)
	badMessageID := ""

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         badMessageID,
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	expectedMessageID := mmessage.Hash(dataBase64, signatureBase64)
	require.EqualError(t, sock.err, fmt.Sprintf("invalid message field: message_id is wrong: expected %q found %q", expectedMessageID, badMessageID))
}

func Test_Create_LAO_Bad_Signature(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	expectedSignature := base64.URLEncoding.EncodeToString(signature)
	badSignatureBase64 := dataBase64

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         badSignatureBase64,
		MessageID:         mmessage.Hash(dataBase64, expectedSignature),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	require.EqualError(t, sock.err, fmt.Sprintf("%v", sock.err))
}

func Test_Create_LAO_Data_Not_Base64(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	expectedSignature := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              "ThisIsNotBase64Encoded",
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         expectedSignature,
		MessageID:         mmessage.Hash(dataBase64, expectedSignature),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	require.EqualError(t, sock.err, fmt.Sprintf("%v", sock.err))
}

func Test_Create_Invalid_Json_Schema(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	type N0thing struct {
		Object string `json:"object"`
		Action string `json:"action"`
		Not    string `json:"not"`
	}

	data := N0thing{
		Object: "lao",
		Action: "nothing",
		Not:    "no",
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	expectedSignature := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         expectedSignature,
		MessageID:         mmessage.Hash(dataBase64, expectedSignature),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	assert.Contains(t, sock.err.Error(), "failed to validate message against json schema", "error message %s", "formatted")
}

func Test_Create_Invalid_Lao_Id(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), "wrongName")
	trueLaoId := mmessage.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	expectedSignature := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         expectedSignature,
		MessageID:         mmessage.Hash(dataBase64, expectedSignature),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	require.Contains(t, sock.err.Error(), "lao id is "+laoID+", should be "+trueLaoId)
}

func Test_Create_LAO(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "LAO X"

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         mmessage.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	// we are expecting the lao channel factor be called with the right
	// arguments.
	require.Equal(t, rootPrefix+data.ID, fakeChannelFac.chanID)
	require.Equal(t, msg.Data, fakeChannelFac.msg.Data)
	require.Equal(t, msg.MessageID, fakeChannelFac.msg.MessageID)
	require.Equal(t, msg.Sender, fakeChannelFac.msg.Sender)
	require.Equal(t, msg.Signature, fakeChannelFac.msg.Signature)
	require.Equal(t, msg.WitnessSignatures, fakeChannelFac.msg.WitnessSignatures)

	// the server should have saved the channel locally

	require.Contains(t, hub.channelByID.GetTable(), rootPrefix+data.ID)

	channel, _ := hub.channelByID.Get(rootPrefix + data.ID)
	require.Equal(t, fakeChannelFac.c, channel)
}

func Test_Wrong_Root_Publish(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	laoID := "/root"

	hub.channelByID.Set(rootPrefix+laoID, c)

	data := mlao.LaoState{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
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

	msg := mmessage.Message{
		Data:              base64.URLEncoding.EncodeToString(dataBuf),
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         base64.URLEncoding.EncodeToString(signature),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}
	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
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

	// check the socket
	require.Error(t, sock.err, "only lao#create is allowed on root, but found %s#%s", data.Object, data.Action)

	// check that there is no errors with messages from witness too
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// check the socket
	require.Error(t, sock.err, "only lao#create is allowed on root, but found %s#%s", data.Object, data.Action)
}

func Test_Handle_Answer(t *testing.T) {
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	var output bytes.Buffer

	hub, err := NewHub(keypair.public, "", "", zerolog.New(&output), fakeChannelFac.newChannel)
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
		JSONRPC string             `json:"jsonrpc"`
		ID      int                `json:"id"`
		Result  []mmessage.Message `json:"result"`
	}{
		JSONRPC: "2.0",
		ID:      1,
		Result:  make([]mmessage.Message, 1),
	}
	messageDataPath := filepath.Join("..", "..", "..", "validation", "protocol",
		"examples", "messageData", "lao_create", "lao_create.json")

	messageDataBuf, err := os.ReadFile(messageDataPath)
	require.NoError(t, err)

	messageData := base64.URLEncoding.EncodeToString(messageDataBuf)

	msg := mmessage.Message{
		Data:              messageData,
		Sender:            publicKey64,
		WitnessSignatures: []mmessage.WitnessSignature{},
	}
	serverAnswer.Result[0] = msg

	serverAnswerBis := struct {
		JSONRPC string             `json:"jsonrpc"`
		ID      int                `json:"id"`
		Result  []mmessage.Message `json:"result"`
	}{
		JSONRPC: "2.0",
		ID:      2,
		Result:  make([]mmessage.Message, 0),
	}

	resultBuf, err := json.Marshal(result)
	require.NoError(t, err)

	answerBuf, err := json.Marshal(serverAnswer)
	require.NoError(t, err)

	answerBisBuf, err := json.Marshal(serverAnswerBis)
	require.NoError(t, err)

	query := mgetmessagesbyid.GetMessagesById{
		Base:   mquery.Base{},
		ID:     1,
		Params: nil,
	}
	hub.queries.AddQuery(1, query)
	sock := &fakeSocket{}

	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: resultBuf,
	})
	require.Error(t, sock.err, "rpc message sent by a client should be a query")
	sock.err = nil
	queryState, err := hub.queries.GetQueryState(1)
	require.NoError(t, err)
	require.False(t, queryState)

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: resultBuf,
	})
	require.NoError(t, sock.err)
	queryState, _ = hub.queries.GetQueryState(1)
	require.NoError(t, err)
	require.False(t, queryState)

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: answerBuf,
	})
	require.NoError(t, sock.err)
	queryState, _ = hub.queries.GetQueryState(1)
	require.NoError(t, err)
	require.True(t, queryState)

	output.Reset()

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: answerBuf,
	})
	// Check that receiving twice an answer for a query doesn't return an error
	require.NoError(t, sock.err)

	// Check that the log for receiving more than on an answer for a query exists
	outputString := output.String()
	require.Contains(t, outputString,
		fmt.Sprintf("query with id %d already answered", serverAnswer.ID))

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: answerBisBuf,
	})
	require.Error(t, sock.err, "no query sent with id %v", serverAnswerBis.ID)
}

// Check that if the server receives a publish message from an end user, it will call the
// publish function on the appropriate channel.
func Test_Handle_Publish_From_Client(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID.Set(rootPrefix+laoID, c)

	signature, err := schnorr.Sign(suite, keypair.private, []byte("XXX"))
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString([]byte("XXX"))
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         mmessage.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
		}{
			Channel: rootPrefix + laoID,
			Message: msg,
		},
	}

	publishBuf, err := json.Marshal(&publish)
	require.NoError(t, err)

	sock := &fakeSocket{}

	// check that there is no errors with messages from client
	hub.handleMessageFromClient(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// check the socket
	require.NoError(t, sock.err)
	require.Equal(t, publish.ID, sock.resultID)

	// check that the channel has been called with the publish message
	require.Equal(t, publish, c.publish)
}

// Check that if the server receives a publish message from an end user, it will call the
// publish function on the appropriate channel.
func Test_Handle_Publish_From_Server(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID.Set(rootPrefix+laoID, c)

	signature, err := schnorr.Sign(suite, keypair.private, []byte("XXX"))
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString([]byte("XXX"))
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         mmessage.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
		}{
			Channel: rootPrefix + laoID,
			Message: msg,
		},
	}

	publishBuf, err := json.Marshal(&publish)
	require.NoError(t, err)

	sock := &fakeSocket{}

	// check that there is no errors with messages from witness
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// check the socket
	require.NoError(t, sock.err)
	require.Equal(t, publish.ID, sock.resultID)

	// check that the channel has been called with the publish message
	require.Equal(t, publish, c.publish)
}

// Check that if the server receives a message twice, it will
// return an error
func Test_Receive_Publish_Twice(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID.Set(rootPrefix+laoID, c)

	signature, err := schnorr.Sign(suite, keypair.private, []byte("XXX"))
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString([]byte("XXX"))
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         mmessage.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	publish := mpublish.Publish{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodPublish,
		},

		ID: 1,

		Params: struct {
			Channel string           `json:"channel"`
			Message mmessage.Message `json:"message"`
		}{
			Channel: rootPrefix + laoID,
			Message: msg,
		},
	}

	publishBuf, err := json.Marshal(&publish)
	require.NoError(t, err)

	sock := &fakeSocket{}

	// Receive message from a server
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// check the socket
	require.NoError(t, sock.err)
	require.Equal(t, publish.ID, sock.resultID)

	// check that the channel has been called with the publish message
	require.Equal(t, publish, c.publish)

	// Receive the same message again
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// check the socket
	require.Error(t, sock.err, "message %s was already received", publish.Params.Message.MessageID)
}

// Test that a LAO is correctly created when receiving a getMessagesById answer
func Test_Create_LAO_GetMessagesById_Result(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	name := "LAO X"
	creationTime := 123
	organizer := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(organizer, fmt.Sprintf("%d", creationTime), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  int64(creationTime),
		Organizer: organizer,
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         mmessage.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	result := make(map[string][]mmessage.Message)
	result["/root"] = []mmessage.Message{msg}

	missingMessages := make(map[string][]string)
	missingMessages["/root"] = []string{msg.MessageID}

	getMessagesByIdQuery := mgetmessagesbyid.GetMessagesById{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGetMessagesById,
		}, ID: 1,
		Params: missingMessages,
	}

	hub.queries.AddQuery(1, getMessagesByIdQuery)

	ans := struct {
		JSONRPC string                        `json:"jsonrpc"`
		ID      int                           `json:"id"`
		Result  map[string][]mmessage.Message `json:"result"`
	}{
		JSONRPC: "2.0",
		ID:      1,
		Result:  result,
	}

	answerBuf, err := json.Marshal(ans)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: answerBuf,
	})

	require.Equal(t, 0, sock.resultID)

	// we are expecting the lao channel factor be called with the right
	// arguments.
	require.Equal(t, rootPrefix+data.ID, fakeChannelFac.chanID)
	require.Equal(t, msg.Data, fakeChannelFac.msg.Data)
	require.Equal(t, msg.MessageID, fakeChannelFac.msg.MessageID)
	require.Equal(t, msg.Sender, fakeChannelFac.msg.Sender)
	require.Equal(t, msg.Signature, fakeChannelFac.msg.Signature)
	require.Equal(t, msg.WitnessSignatures, fakeChannelFac.msg.WitnessSignatures)

	// the server should have saved the channel locally

	require.Contains(t, hub.channelByID.GetTable(), rootPrefix+data.ID)
	channel, _ := hub.channelByID.Get(rootPrefix + laoID)
	require.Equal(t, fakeChannelFac.c, channel)
}

// Tests that an answer to a getMessagesById without a valid message id returns an error
func Test_Create_LAO_GetMessagesById_Wrong_MessageID(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeChannelFac := &fakeChannelFac{
		c: &fakeChannel{},
	}

	hub, err := NewHub(keypair.public, "", "", nolog, fakeChannelFac.newChannel)
	require.NoError(t, err)

	name := "LAO X"
	creationTime := 12300000
	organizer := base64.URLEncoding.EncodeToString([]byte("Somebody"))

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	laoID := mmessage.Hash(organizer, fmt.Sprintf("%d", creationTime), name)

	data := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  int64(creationTime),
		Organizer: organizer,
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature, err := schnorr.Sign(suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)
	fakeMessageID := ""

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         fakeMessageID,
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	result := make(map[string][]mmessage.Message)
	result["/root"] = []mmessage.Message{msg}

	missingMessages := make(map[string][]string)
	missingMessages["/root"] = []string{msg.MessageID}

	getMessagesByIdQuery := mgetmessagesbyid.GetMessagesById{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGetMessagesById,
		}, ID: 1,
		Params: missingMessages,
	}

	hub.queries.AddQuery(1, getMessagesByIdQuery)

	ans := struct {
		JSONRPC string                        `json:"jsonrpc"`
		ID      int                           `json:"id"`
		Result  map[string][]mmessage.Message `json:"result"`
	}{
		JSONRPC: "2.0",
		ID:      1,
		Result:  result,
	}

	answerBuf, err := json.Marshal(ans)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: answerBuf,
	})

	expectedMessageID := mmessage.Hash(dataBase64, signatureBase64)
	require.EqualError(t, sock.err, fmt.Sprintf("failed to handle answer message: failed to process messages: message_id is wrong: expected %q found %q", expectedMessageID, fakeMessageID))
}

// Check that if the server receives a subscribe message, it will call the
// subscribe function on the appropriate channel.
func Test_Handle_Subscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID.Set(rootPrefix+laoID, c)

	subscribe := msubscribe.Subscribe{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodSubscribe,
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

	// check the socket
	require.NoError(t, sock.err)
	require.Equal(t, subscribe.ID, sock.resultID)

	// check that the channel has been called with the publish message
	require.Equal(t, subscribe, c.subscribe)

	// check that there is no errors with messages from witness too
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// check the socket
	require.NoError(t, sock.err)
	require.Equal(t, subscribe.ID, sock.resultID)

	// check that the channel has been called with the publish message
	require.Equal(t, subscribe, c.subscribe)
}

// Check that if the server receives an unsubscribe message, it will call the
// unsubscribe function on the appropriate channel.
func TestServer_Handle_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	c := &fakeChannel{}

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID.Set(rootPrefix+laoID, c)

	unsubscribe := method2.Unsubscribe{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodUnsubscribe,
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

	// check the socket
	require.NoError(t, sock.err)
	require.Equal(t, unsubscribe.ID, sock.resultID)

	// check that the channel has been called with the publish message
	require.Equal(t, unsubscribe, c.unsubscribe)
	require.Equal(t, sock.id, c.socketID)

	// check that there is no errors with messages from witness too
	hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: publishBuf,
	})

	// check the socket
	require.NoError(t, sock.err)
	require.Equal(t, unsubscribe.ID, sock.resultID)

	// check that the channel has been called with the publish message
	require.Equal(t, unsubscribe, c.unsubscribe)
	require.Equal(t, sock.id, c.socketID)
}

// Check that if the server receives a catchup message, it will call the
// catchup function on the appropriate channel.
func TestServer_Handle_Catchup(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeMessages := []mmessage.Message{
		{
			MessageID: "XXX",
		},
	}

	// set fake messages on the channel
	c := &fakeChannel{
		msgs: fakeMessages,
	}

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID.Set(rootPrefix+laoID, c)

	catchup := mcatchup.Catchup{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodCatchUp,
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

	// check the socket
	require.NoError(t, sock.err)
	require.Equal(t, catchup.ID, sock.resultID)

	// check that the channel has been called with the publish message
	require.Equal(t, catchup, c.catchup)
	require.Equal(t, fakeMessages, c.msgs)

	// check that the channel has been called with the publish message
	require.Equal(t, catchup, c.catchup)
	require.Equal(t, fakeMessages, c.msgs)
}

// Test that the GetServerNumber works
func Test_Get_Server_Number(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
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

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	laoID := "XXX"

	hub.channelByID.Set(rootPrefix+laoID, c)

	signature, err := schnorr.Sign(suite, keypair.private, []byte("XXX"))
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString([]byte("XXX"))
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := mmessage.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         mmessage.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	broadcast := mbroadcast.Broadcast{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: mquery.MethodBroadcast,
		},

		Params: struct {
			Channel string           "json:\"channel\""
			Message mmessage.Message "json:\"message\""
		}{
			Channel: rootPrefix + laoID,
			Message: msg,
		},
	}

	broadcastBuf, err := json.Marshal(&broadcast)
	require.NoError(t, err)

	sock := &fakeSocket{}
	hub.serverSockets.Upsert(sock)

	err = hub.SendAndHandleMessage(broadcast)
	require.NoError(t, err)

	// wait for the goroutine created by the function
	time.Sleep(100 * time.Millisecond)

	// Check the socket.
	sock.Lock()
	require.Equal(t, broadcastBuf, sock.msg)
	sock.Unlock()

	// check that the channel has been called with the broadcast message
	c.Lock()
	require.Equal(t, broadcast, c.broadcast)
	c.Unlock()
}

// Test that the correct heartbeat message is sent
func Test_Send_Heartbeat_Message(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.serverSockets.Upsert(sock)

	hub.hubInbox.StoreMessage("/root", msg1)
	hub.hubInbox.StoreMessage("/root", msg2)
	hub.hubInbox.StoreMessage("/root/channel1", msg3)

	hub.sendHeartbeatToServers()

	heartbeatMsg := sock.msg

	var heartbeat mheartbeat.Heartbeat

	err = json.Unmarshal(heartbeatMsg, &heartbeat)
	require.NoError(t, err)

	messageIdsSent := heartbeat.Params

	// Check that all the stored messages where sent
	for storedChannel, storedIds := range hub.hubInbox.GetIDsTable() {
		sentIds, exists := messageIdsSent[storedChannel]
		require.True(t, exists)
		for _, storedId := range storedIds {
			require.True(t, slices.Contains(sentIds, storedId))
		}
	}
}

// Test that the heartbeat messages are properly handled
func Test_Handle_Heartbeat(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	hub.hubInbox.StoreMessage("/root", msg1)

	sock := &fakeSocket{}

	// The message Ids sent in hearbeat message
	messageIds := make(map[string][]string)
	messageIds["/root"] = idsRoot
	messageIds["/root/channel1"] = idsChannel1

	// The missing Ids the server should request
	missingIds := make(map[string][]string)
	missingIds["/root"] = []string{msg2.MessageID}
	missingIds["/root/channel1"] = idsChannel1

	heartbeatMessage := mheartbeat.Heartbeat{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodHeartbeat,
		},
		Params: messageIds,
	}

	msg, err := json.Marshal(heartbeatMessage)
	require.NoError(t, err)

	err = hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: msg,
	})
	require.NoError(t, err)
	require.NoError(t, sock.err)

	// socket should receive a getMessagesById query after handling of heartbeat
	var getMessagesById mgetmessagesbyid.GetMessagesById

	err = json.Unmarshal(sock.msg, &getMessagesById)
	require.NoError(t, err)

	requestedIds := getMessagesById.Params

	for channelId, messageIds := range missingIds {
		requestedIds, exists := requestedIds[channelId]
		require.True(t, exists)
		for _, storedId := range messageIds {
			require.True(t, slices.Contains(requestedIds, storedId))
		}
	}
}

// Test that the getMessagesById messages are properly handled
func Test_Handle_GetMessagesById(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	sock := &fakeSocket{}

	hub.serverSockets.Upsert(sock)

	hub.hubInbox.StoreMessage("/root", msg1)
	hub.hubInbox.StoreMessage("/root", msg2)
	hub.hubInbox.StoreMessage("/root/channel1", msg3)

	// The missing Ids requested by the server
	missingIds := make(map[string][]string)
	missingIds["/root"] = []string{msg2.MessageID}
	missingIds["/root/channel1"] = idsChannel1

	// The missing messages the server should receive
	missingMessages := make(map[string][]mmessage.Message)
	missingMessages["/root"] = []mmessage.Message{msg2}
	missingMessages["/root/channel1"] = res2

	getMessagesById := mgetmessagesbyid.GetMessagesById{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGetMessagesById,
		}, ID: 5,
		Params: missingIds,
	}

	msg, err := json.Marshal(getMessagesById)
	require.NoError(t, err)

	err = hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: msg,
	})
	require.NoError(t, err)

	require.Equal(t, getMessagesById.ID, sock.resultID)

	receivedMessages := sock.missingMsgs
	for receivedChannelIds, receivedMessagesForChannel := range receivedMessages {
		for _, msg := range receivedMessagesForChannel {
			require.Contains(t, missingMessages[receivedChannelIds], msg)
		}
	}
}

// Test that the correct greet server message is sent
func Test_Send_GreetServer_Message(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, "ws://localhost:9000/client", "ws://localhost:9001/server", nolog, nil)
	require.NoError(t, err)

	pkServ, err := hub.pubKeyServ.MarshalBinary()
	require.NoError(t, err)
	pk := base64.URLEncoding.EncodeToString(pkServ)

	sock := &fakeSocket{}
	err = hub.SendGreetServer(sock)
	require.NoError(t, err)

	greetServerMsg := sock.msg

	var greetServer mgreetserver.GreetServer
	err = json.Unmarshal(greetServerMsg, &greetServer)
	require.NoError(t, err)
	require.Equal(t, mquery.MethodGreetServer, greetServer.Method)
	require.Equal(t, pk, greetServer.Params.PublicKey)
	require.Equal(t, "ws://localhost:9001/server", greetServer.Params.ServerAddress)
	require.Equal(t, "ws://localhost:9000/client", greetServer.Params.ClientAddress)
}

// Test that the greet server messages received from non greeted servers are properly handled
func Test_Handle_GreetServer_First_Time(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, "ws://localhost:9000/client", "ws://localhost:9001/server", nolog, nil)
	require.NoError(t, err)

	pkServ, err := hub.pubKeyServ.MarshalBinary()
	require.NoError(t, err)
	pk := base64.URLEncoding.EncodeToString(pkServ)

	sock := &fakeSocket{}

	serverInfo := mgreetserver.GreetServerParams{
		PublicKey:     "",
		ServerAddress: "ws://localhost:9003/server",
		ClientAddress: "ws://localhost:9002/client",
	}

	serverGreet := mgreetserver.GreetServer{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGreetServer,
		},
		Params: serverInfo,
	}

	msg, err := json.Marshal(serverGreet)
	require.NoError(t, err)

	err = hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: msg,
	})
	require.NoError(t, err)
	require.NoError(t, sock.err)

	// socket should receive a server greet back after handling of server greet
	var serverGreetResponse mgreetserver.GreetServer

	err = json.Unmarshal(sock.msg, &serverGreetResponse)
	require.NoError(t, err)

	require.Equal(t, mquery.MethodGreetServer, serverGreetResponse.Method)
	require.Equal(t, pk, serverGreetResponse.Params.PublicKey)
	require.Equal(t, "ws://localhost:9001/server", serverGreetResponse.Params.ServerAddress)
	require.Equal(t, "ws://localhost:9000/client", serverGreetResponse.Params.ClientAddress)
}

// Test that the greet server messages received from already greeted servers are properly handled
// and that the server is not greeted again to avoid loops
func Test_Handle_GreetServer_Already_Greeted(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, "ws://localhost:9000/client", "ws://localhost:9001/server", nolog, nil)
	require.NoError(t, err)

	sock := &fakeSocket{}

	err = hub.SendGreetServer(sock)
	require.NoError(t, err)
	require.True(t, hub.peers.IsPeerGreeted(sock.ID()))

	// reset socket message
	sock.msg = nil

	serverInfo := mgreetserver.GreetServerParams{
		PublicKey:     "",
		ServerAddress: "ws://localhost:9003/server",
		ClientAddress: "ws://localhost:9002/client",
	}

	serverGreet := mgreetserver.GreetServer{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGreetServer,
		},
		Params: serverInfo,
	}

	msg, err := json.Marshal(serverGreet)
	require.NoError(t, err)

	err = hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: msg,
	})
	require.NoError(t, err)
	require.NoError(t, sock.err)

	// socket should not receive anything back after handling of server greet
	require.Nil(t, sock.msg)
}

// Test that receiving multiple greet server messages from the same source will
// not override the existing server information and that an error is raised
func Test_Handle_GreetServer_Already_Received(t *testing.T) {
	keypair := generateKeyPair(t)

	hub, err := NewHub(keypair.public, "", "", nolog, nil)
	require.NoError(t, err)

	serverInfo1 := mgreetserver.GreetServerParams{
		PublicKey:     "",
		ServerAddress: "ws://localhost:9003/server",
		ClientAddress: "ws://localhost:9002/client",
	}

	serverInfo2 := mgreetserver.GreetServerParams{
		PublicKey:     "",
		ServerAddress: "ws://localhost:9005/server",
		ClientAddress: "ws://localhost:9004/client",
	}

	serverGreet1 := mgreetserver.GreetServer{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGreetServer,
		},
		Params: serverInfo1,
	}

	serverGreet2 := mgreetserver.GreetServer{
		Base: mquery.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: mquery.MethodGreetServer,
		},
		Params: serverInfo2,
	}

	sock := &fakeSocket{id: "fakeID"}

	msg1, err := json.Marshal(serverGreet1)
	require.NoError(t, err)

	msg2, err := json.Marshal(serverGreet2)
	require.NoError(t, err)

	err = hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: msg1,
	})
	require.NoError(t, err)
	require.NoError(t, sock.err)

	// check that handling GreetServer from the same source twice throw an error
	err = hub.handleMessageFromServer(&socket.IncomingMessage{
		Socket:  sock,
		Message: msg2,
	})
	require.Error(t, err)
	require.Error(t, sock.err)

	// check that the peersInfo were not modified by the second GreetServer
	peersInfo := hub.GetPeersInfo()
	require.Len(t, peersInfo, 1)
	require.Equal(t, serverInfo1, peersInfo[0])
	require.NotEqual(t, serverInfo2, peersInfo[0])
}

// -----------------------------------------------------------------------------
// Utility functions

type keypair struct {
	public    kyber.Point
	publicBuf []byte
	private   kyber.Scalar
}

var nolog = zerolog.New(io.Discard)

// var suite = crypto.Suite

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
	msg    mmessage.Message
	c      channel.Channel
	log    zerolog.Logger
}

// newChannel implement the type channel.LaoFactory
func (c *fakeChannelFac) newChannel(channelID string, hub channel.HubFunctionalities,
	msg mmessage.Message, log zerolog.Logger, organizerKey kyber.Point, socket socket.Socket,
) (channel.Channel, error) {
	c.chanID = channelID
	c.msg = msg
	c.log = log
	return c.c, nil
}

// fakeChannel is a fake implementation of a channel
//
// - implements channel.Channel
type fakeChannel struct {
	sync.Mutex

	subscribe   msubscribe.Subscribe
	unsubscribe method2.Unsubscribe
	publish     mpublish.Publish
	catchup     mcatchup.Catchup
	broadcast   mbroadcast.Broadcast

	// set by the subscribe
	socket socket.Socket
	// set by the unsubscribe
	socketID string

	// fake messages to return in a catchup
	msgs []mmessage.Message
}

// Subscribe implements channel.Channel
func (f *fakeChannel) Subscribe(socket socket.Socket, msg msubscribe.Subscribe) error {
	f.Lock()
	defer f.Unlock()

	f.socket = socket
	f.subscribe = msg
	return nil
}

// Unsubscribe implements channel.Channel
func (f *fakeChannel) Unsubscribe(socketID string, msg method2.Unsubscribe) error {
	f.Lock()
	defer f.Unlock()

	f.socketID = socketID
	f.unsubscribe = msg
	return nil
}

// Publish implements channel.Channel
func (f *fakeChannel) Publish(msg mpublish.Publish, socket socket.Socket) error {
	f.Lock()
	defer f.Unlock()

	f.publish = msg
	return nil
}

// Catchup implements channel.Channel
func (f *fakeChannel) Catchup(msg mcatchup.Catchup) []mmessage.Message {
	f.Lock()
	defer f.Unlock()

	f.catchup = msg
	return f.msgs
}

// Broadcast implements channel.Channel
func (f *fakeChannel) Broadcast(msg mbroadcast.Broadcast, _ socket.Socket) error {
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

	resultID    int
	res         []mmessage.Message
	missingMsgs map[string][]mmessage.Message
	msg         []byte

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
func (f *fakeSocket) SendResult(id int, res []mmessage.Message, missingMsgs map[string][]mmessage.Message) {
	f.Lock()
	defer f.Unlock()

	f.resultID = id
	f.res = res
	f.missingMsgs = missingMsgs
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

// -------------------------------------
// Test variables definition

var msg1 = mmessage.Message{
	Data:              "data1",
	Sender:            "sender1",
	Signature:         "signature1",
	MessageID:         "message1",
	WitnessSignatures: nil,
}

var msg2 = mmessage.Message{
	Data:              "data2",
	Sender:            "sender2",
	Signature:         "signature2",
	MessageID:         "message2",
	WitnessSignatures: nil,
}

var msg3 = mmessage.Message{
	Data:              "data3",
	Sender:            "sender3",
	Signature:         "signature3",
	MessageID:         "message3",
	WitnessSignatures: nil,
}

var res2 = []mmessage.Message{msg3}

var (
	idsRoot     = []string{msg1.MessageID, msg2.MessageID}
	idsChannel1 = []string{msg3.MessageID}
)
