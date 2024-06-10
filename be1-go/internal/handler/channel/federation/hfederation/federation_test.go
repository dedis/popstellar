package hfederation

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/gorilla/websocket"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"net"
	"net/http"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/federation/hfederation/mocks"
	mfederation2 "popstellar/internal/handler/channel/federation/mfederation"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/broadcast/mbroadcast"
	"popstellar/internal/handler/method/publish/mpublish"
	method2 "popstellar/internal/handler/method/subscribe/msubscribe"
	"popstellar/internal/handler/query/mquery"
	mock2 "popstellar/internal/network/socket/mocks"
	"popstellar/internal/state"
	generator2 "popstellar/internal/test/generator"
	"popstellar/internal/validation"
	"testing"
	"time"
)

func Test_handleChannelFederation(t *testing.T) {
	type input struct {
		name        string
		channelPath string
		msg         mmessage.Message
		isError     bool
		contains    string
	}

	var args []input

	db := mocks.NewRepository(t)
	subs := state.NewSubscribers()
	hub := state.NewHubParams()
	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	federationHandler := New(hub, subs, db, schema)

	organizerPk, _, organizerSk, _ := generator2.GenerateKeyPair(t)
	organizer2Pk, _, organizer2Sk, _ := generator2.GenerateKeyPair(t)
	notOrganizerPk, _, notOrganizerSk, _ := generator2.GenerateKeyPair(t)

	organizerBuf, err := organizerPk.MarshalBinary()
	require.NoError(t, err)
	organizer := base64.URLEncoding.EncodeToString(organizerBuf)

	organizer2Buf, err := organizer2Pk.MarshalBinary()
	require.NoError(t, err)
	organizer2 := base64.URLEncoding.EncodeToString(organizer2Buf)

	notOrganizerBuf, err := notOrganizerPk.MarshalBinary()
	require.NoError(t, err)
	notOrganizer := base64.URLEncoding.EncodeToString(notOrganizerBuf)

	laoID := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	laoPath := fmt.Sprintf("/root/%s", laoID)
	channelPath := fmt.Sprintf("/root/%s/federation", laoID)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()

	db.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)

	// Test 1 Error when FederationChallengeRequest sender is not the same as
	// the lao organizer
	args = append(args, input{
		name:        "Test 1",
		channelPath: channelPath,
		msg: generator2.NewFederationChallengeRequest(t,
			notOrganizer, validUntil, notOrganizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channelPath",
	})

	// Test 2 Error when FederationChallengeRequest timestamp is negative
	args = append(args, input{
		name:        "Test 2",
		channelPath: channelPath,
		msg: generator2.NewFederationChallengeRequest(t,
			organizer, -1, organizerSk),
		isError:  true,
		contains: "failed to validate schema:",
	})

	// Test 3 Error when FederationExpect sender is not the same as the lao
	// organizer
	args = append(args, input{
		name:        "Test 3",
		channelPath: channelPath,
		msg: generator2.NewFederationExpect(t, notOrganizer, laoID,
			serverAddressA, organizer2,
			generator2.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			notOrganizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channelPath",
	})

	// Test 4 Error when FederationExpect serverAddress is not valid format
	args = append(args, input{
		name:        "Test 4",
		channelPath: channelPath,
		msg: generator2.NewFederationExpect(t, organizer, laoID,
			"ws:localhost:12345/client", organizer2,
			generator2.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "failed to validate schema:",
	})

	// Test 5 Error when FederationExpect publicKey is not valid format
	args = append(args, input{
		name:        "Test 5",
		channelPath: channelPath,
		msg: generator2.NewFederationExpect(t, organizer, laoID,
			serverAddressA, "organizer2",
			generator2.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "failed to validate schema:",
	})

	// Test 6 Error when FederationExpect laoId is not valid format
	args = append(args, input{
		name:        "Test 6",
		channelPath: channelPath,
		msg: generator2.NewFederationExpect(t, organizer, "laoID",
			serverAddressA, organizer2,
			generator2.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "failed to validate schema:",
	})

	// Test 7 Error when FederationExpect challenge message is not a challenge
	args = append(args, input{
		name:        "Test 7",
		channelPath: channelPath,
		msg: generator2.NewFederationExpect(t, organizer, laoID,
			serverAddressA, organizer2,
			generator2.NewFederationChallengeRequest(t, organizer,
				validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "invalid message",
	})

	// Test 8 Error when FederationExpect challenge is not from organizer
	args = append(args, input{
		name:        "Test 8",
		channelPath: channelPath,
		msg: generator2.NewFederationExpect(t, organizer, laoID,
			serverAddressA, organizer2,
			generator2.NewFederationChallenge(t, notOrganizer,
				value, validUntil, notOrganizerSk),
			organizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channelPath",
	})

	// Test 9 Error when FederationInit sender is not the same as the lao
	// organizer
	args = append(args, input{
		name:        "Test 9",
		channelPath: channelPath,
		msg: generator2.NewFederationInit(t, notOrganizer, laoID,
			serverAddressA, organizer2,
			generator2.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			notOrganizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channelPath",
	})

	// Test 10 Error when FederationInit serverAddress is not valid format
	args = append(args, input{
		name:        "Test 10",
		channelPath: channelPath,
		msg: generator2.NewFederationInit(t, organizer, laoID,
			"ws:localhost:12345/client", organizer2,
			generator2.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "failed to validate schema:",
	})

	// Test 11 Error when FederationInit publicKey is not valid format
	args = append(args, input{
		name:        "Test 11",
		channelPath: channelPath,
		msg: generator2.NewFederationInit(t, organizer, laoID,
			serverAddressA, "organizer2",
			generator2.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "failed to validate schema:",
	})

	// Test 12 Error when FederationInit laoId is not valid format
	args = append(args, input{
		name:        "Test 12",
		channelPath: channelPath,
		msg: generator2.NewFederationInit(t, organizer, "laoID",
			serverAddressA, organizer2,
			generator2.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "failed to validate schema:",
	})

	// Test 13 Error when FederationInit challenge message is not a challenge
	args = append(args, input{
		name:        "Test 13",
		channelPath: channelPath,
		msg: generator2.NewFederationInit(t, organizer, laoID,
			serverAddressA, organizer2,
			generator2.NewFederationChallengeRequest(t, organizer,
				validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "invalid message",
	})

	// Test 14 Error when FederationInit challenge is not from organizer
	args = append(args, input{
		name:        "Test 14",
		channelPath: channelPath,
		msg: generator2.NewFederationInit(t, organizer, laoID,
			serverAddressA, organizer2,
			generator2.NewFederationChallenge(t, notOrganizer,
				value, validUntil, notOrganizerSk),
			organizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channelPath",
	})

	federationChallenge1 := mfederation2.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	db.On("GetFederationExpect", organizer,
		notOrganizer, federationChallenge1, channelPath).Return(mfederation2.FederationExpect{}, sql.ErrNoRows)

	// Test 15 Error when FederationChallenge is received without any
	// matching FederationExpect
	args = append(args, input{
		name:        "Test 15",
		channelPath: channelPath,
		msg: generator2.NewFederationChallenge(t, notOrganizer, value,
			validUntil, notOrganizerSk),
		isError:  true,
		contains: "sql: no rows in result set",
	})

	// Test 16 Error when FederationResult challenge message is not a challenge
	args = append(args, input{
		name:        "Test 16",
		channelPath: channelPath,
		msg: generator2.NewSuccessFederationResult(t, organizer2,
			organizer, generator2.NewFederationChallengeRequest(t,
				organizer2, validUntil, organizer2Sk), organizer2Sk),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 17 Error when FederationResult PublicKey is not the organizerPk
	args = append(args, input{
		name:        "Test 17",
		channelPath: channelPath,
		msg: generator2.NewSuccessFederationResult(t, organizer2,
			notOrganizer, generator2.NewFederationChallenge(t, organizer2,
				value, validUntil, organizer2Sk), organizer2Sk),
		isError:  true,
		contains: "invalid message field",
	})

	db.On("GetFederationInit", organizer,
		organizer2, federationChallenge1, channelPath).Return(mfederation2.FederationInit{}, sql.ErrNoRows)

	// Test 18 Error when FederationResult is received without any
	// matching FederationInit
	args = append(args, input{
		name:        "Test 18",
		channelPath: channelPath,
		msg: generator2.NewSuccessFederationResult(t, organizer2,
			organizer, generator2.NewFederationChallenge(t, organizer2,
				value, validUntil, organizer2Sk), organizer2Sk),
		isError:  true,
		contains: "sql: no rows in result set",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err = federationHandler.Handle(arg.channelPath, arg.msg)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}
}

func Test_handleRequestChallenge(t *testing.T) {
	db := mocks.NewRepository(t)
	subs := state.NewSubscribers()
	hub := state.NewHubParams()
	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	federationHandler := New(hub, subs, db, schema)

	organizerPk, _, organizerSk, _ := generator2.GenerateKeyPair(t)
	serverPk, _, serverSk, _ := generator2.GenerateKeyPair(t)

	organizerBuf, err := organizerPk.MarshalBinary()
	require.NoError(t, err)
	organizer := base64.URLEncoding.EncodeToString(organizerBuf)

	laoID := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	laoPath := fmt.Sprintf("/root/%s", laoID)
	channelPath := fmt.Sprintf("/root/%s/federation", laoID)

	err = subs.AddChannel(channelPath)
	require.NoError(t, err)

	fakeSocket := mock2.FakeSocket{Id: "1"}
	err = subs.Subscribe(channelPath, &fakeSocket)
	require.NoError(t, err)

	db.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	db.On("GetServerKeys").Return(serverPk, serverSk, nil)
	db.On("StoreMessageAndData", channelPath,
		mock.AnythingOfType("mmessage.Message")).Return(nil)

	err = federationHandler.handleRequestChallenge(generator2.NewFederationChallengeRequest(t, organizer, time.Now().Unix(), organizerSk), channelPath)
	require.NoError(t, err)

	require.NotNil(t, fakeSocket.Msg)
	var broadcastMsg mbroadcast.Broadcast
	err = json.Unmarshal(fakeSocket.Msg, &broadcastMsg)
	require.NoError(t, err)

	require.Equal(t, "broadcast", broadcastMsg.Method)
	require.Equal(t, channelPath, broadcastMsg.Params.Channel)

	var challenge mfederation2.FederationChallenge
	err = broadcastMsg.Params.Message.UnmarshalData(&challenge)
	require.NoError(t, err)

	err = challenge.Verify()
	require.NoError(t, err)
}

func Test_handleFederationExpect(t *testing.T) {
	db := mocks.NewRepository(t)
	subs := state.NewSubscribers()
	hub := state.NewHubParams()
	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	federationHandler := New(hub, subs, db, schema)

	organizerPk, _, organizerSk, _ := generator2.GenerateKeyPair(t)
	organizer2Pk, _, _, _ := generator2.GenerateKeyPair(t)
	serverPk, _, serverSk, _ := generator2.GenerateKeyPair(t)

	organizerBuf, err := organizerPk.MarshalBinary()
	require.NoError(t, err)
	organizer := base64.URLEncoding.EncodeToString(organizerBuf)

	organizer2Buf, err := organizer2Pk.MarshalBinary()
	require.NoError(t, err)
	organizer2 := base64.URLEncoding.EncodeToString(organizer2Buf)

	serverBuf, err := serverPk.MarshalBinary()
	require.NoError(t, err)
	server := base64.URLEncoding.EncodeToString(serverBuf)

	laoID := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	laoPath := fmt.Sprintf("/root/%s", laoID)
	channelPath := fmt.Sprintf("/root/%s/federation", laoID)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()

	federationChallenge := mfederation2.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	db.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	db.On("GetServerKeys").Return(serverPk, serverSk, nil)
	db.On("StoreMessageAndData", channelPath,
		mock.AnythingOfType("mmessage.Message")).Return(nil)

	db.On("IsChallengeValid", server, federationChallenge,
		channelPath).Return(nil)

	federationExpect := generator2.NewFederationExpect(t, organizer, laoID,
		serverAddressA, organizer2, generator2.NewFederationChallenge(t,
			organizer, value, validUntil, organizerSk), organizerSk)

	err = federationHandler.handleExpect(federationExpect, channelPath)
	require.NoError(t, err)
}

func Test_handleFederationInit(t *testing.T) {
	db := mocks.NewRepository(t)
	subs := state.NewSubscribers()
	hub := state.NewHubParams()
	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	federationHandler := New(hub, subs, db, schema)

	organizerPk, _, organizerSk, _ := generator2.GenerateKeyPair(t)
	organizer2Pk, _, _, _ := generator2.GenerateKeyPair(t)

	organizerBuf, err := organizerPk.MarshalBinary()
	require.NoError(t, err)
	organizer := base64.URLEncoding.EncodeToString(organizerBuf)

	organizer2Buf, err := organizer2Pk.MarshalBinary()
	require.NoError(t, err)
	organizer2 := base64.URLEncoding.EncodeToString(organizer2Buf)

	laoID := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	laoID2 := "OWY4NmQwODE4ODRjN2Q2NTlhMmZlYWEwYzU1YWQwMQ=="
	laoPath := fmt.Sprintf("/root/%s", laoID)
	channelPath := fmt.Sprintf("/root/%s/federation", laoID)
	channelPath2 := fmt.Sprintf("/root/%s/federation", laoID2)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()

	challengeMsg := generator2.NewFederationChallenge(t, organizer, value,
		validUntil, organizerSk)

	initMsg := generator2.NewFederationInit(t, organizer, laoID2,
		serverAddressA, organizer2, challengeMsg, organizerSk)

	db.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	db.On("StoreMessageAndData", channelPath, initMsg).Return(nil)

	serverBStarted := make(chan struct{})
	msgChan := make(chan []byte, 10)
	mux := http.NewServeMux()
	mux.HandleFunc("/client", websocketHandler(t, msgChan))
	serverB := &http.Server{Addr: "localhost:9801", Handler: mux}

	go websocketServer(t, serverB, serverBStarted)
	<-serverBStarted
	defer serverB.Close()

	err = federationHandler.handleInit(initMsg, channelPath)
	require.NoError(t, err)

	var msgBytes []byte
	select {
	case msgBytes = <-msgChan:
	case <-time.After(time.Second):
		require.Fail(t, "Timed out waiting for expected message")
	}

	var subMsg method2.Subscribe
	err = json.Unmarshal(msgBytes, &subMsg)
	require.NoError(t, err)
	require.Equal(t, mquery.MethodSubscribe, subMsg.Method)
	require.Equal(t, method2.SubscribeParams{Channel: channelPath}, subMsg.Params)

	select {
	case msgBytes = <-msgChan:
	case <-time.After(time.Second):
		require.Fail(t, "Timed out waiting for expected message")
	}
	var publishMsg mpublish.Publish
	err = json.Unmarshal(msgBytes, &publishMsg)
	require.NoError(t, err)
	require.Equal(t, mquery.MethodPublish, publishMsg.Method)
	require.Equal(t, channelPath2, publishMsg.Params.Channel)
	require.Equal(t, challengeMsg, publishMsg.Params.Message)
}

func Test_handleFederationChallenge(t *testing.T) {
	db := mocks.NewRepository(t)
	subs := state.NewSubscribers()
	hub := state.NewHubParams()
	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	federationHandler := New(hub, subs, db, schema)

	organizerPk, _, organizerSk, _ := generator2.GenerateKeyPair(t)
	organizer2Pk, _, organizer2Sk, _ := generator2.GenerateKeyPair(t)
	serverPk, _, serverSk, _ := generator2.GenerateKeyPair(t)

	organizerBuf, err := organizerPk.MarshalBinary()
	require.NoError(t, err)
	organizer := base64.URLEncoding.EncodeToString(organizerBuf)

	organizer2Buf, err := organizer2Pk.MarshalBinary()
	require.NoError(t, err)
	organizer2 := base64.URLEncoding.EncodeToString(organizer2Buf)

	laoID := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	laoID2 := "OWY4NmQwODE4ODRjN2Q2NTlhMmZlYWEwYzU1YWQwMQ=="
	laoPath := fmt.Sprintf("/root/%s", laoID)
	channelPath := fmt.Sprintf("/root/%s/federation", laoID)
	channelPath2 := fmt.Sprintf("/root/%s/federation", laoID2)

	err = subs.AddChannel(channelPath)
	require.NoError(t, err)
	err = subs.AddChannel(channelPath2)
	require.NoError(t, err)

	fakeSocket1 := mock2.FakeSocket{Id: "1"}
	err = subs.Subscribe(channelPath, &fakeSocket1)
	require.NoError(t, err)

	fakeSocket2 := mock2.FakeSocket{Id: "2"}
	err = subs.Subscribe(channelPath2, &fakeSocket2)
	require.NoError(t, err)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()
	challenge := mfederation2.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	challengeMsg := generator2.NewFederationChallenge(t, organizer, value,
		validUntil, organizerSk)

	challengeMsg2 := generator2.NewFederationChallenge(t, organizer2,
		value, validUntil, organizer2Sk)

	federationExpect := mfederation2.FederationExpect{
		Object:        messagedata.FederationObject,
		Action:        messagedata.FederationActionExpect,
		LaoId:         laoID2,
		ServerAddress: serverAddressA,
		PublicKey:     organizer2,
		ChallengeMsg:  challengeMsg,
	}

	db.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	db.On("StoreMessageAndData", channelPath,
		mock.AnythingOfType("mmessage.Message")).Return(nil)
	db.On("GetFederationExpect", organizer, organizer2,
		challenge, channelPath).Return(federationExpect, nil)
	db.On("RemoveChallenge", challenge).Return(nil)
	db.On("GetServerKeys").Return(serverPk, serverSk, nil)

	err = federationHandler.handleChallenge(challengeMsg2, channelPath)
	require.NoError(t, err)

	// The same federation result message should be received by both sockets
	// on fakeSocket1, representing the organizer, it should be in a broadcast
	require.NotNil(t, fakeSocket1.Msg)
	var broadcastMsg mbroadcast.Broadcast
	err = json.Unmarshal(fakeSocket1.Msg, &broadcastMsg)
	require.NoError(t, err)
	require.Equal(t, mquery.MethodBroadcast, broadcastMsg.Method)

	// on fakeSocket2, representing the other server, it should in a publish
	require.NotNil(t, fakeSocket2.Msg)
	var publishMsg mpublish.Publish
	err = json.Unmarshal(fakeSocket2.Msg, &publishMsg)
	require.NoError(t, err)
	require.Equal(t, mquery.MethodPublish, publishMsg.Method)
	require.Equal(t, broadcastMsg.Params.Message, publishMsg.Params.Message)

	var resultMsg mfederation2.FederationResult
	err = broadcastMsg.Params.Message.UnmarshalData(&resultMsg)
	require.NoError(t, err)

	// it should contain the challenge from organizer, not organizer2
	require.Equal(t, challengeMsg, resultMsg.ChallengeMsg)
	require.Equal(t, "success", resultMsg.Status)
	require.Empty(t, resultMsg.Reason)
	require.Equal(t, organizer2, resultMsg.PublicKey)
}

func Test_handleFederationResult(t *testing.T) {
	db := mocks.NewRepository(t)
	subs := state.NewSubscribers()
	hub := state.NewHubParams()
	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	federationHandler := New(hub, subs, db, schema)

	organizerPk, _, organizerSk, _ := generator2.GenerateKeyPair(t)
	organizer2Pk, _, organizer2Sk, _ := generator2.GenerateKeyPair(t)

	organizerBuf, err := organizerPk.MarshalBinary()
	require.NoError(t, err)
	organizer := base64.URLEncoding.EncodeToString(organizerBuf)

	organizer2Buf, err := organizer2Pk.MarshalBinary()
	require.NoError(t, err)
	organizer2 := base64.URLEncoding.EncodeToString(organizer2Buf)

	laoID := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	laoPath := fmt.Sprintf("/root/%s", laoID)
	channelPath := fmt.Sprintf("/root/%s/federation", laoID)

	err = subs.AddChannel(channelPath)
	require.NoError(t, err)

	fakeSocket := mock2.FakeSocket{Id: "1"}
	err = subs.Subscribe(channelPath, &fakeSocket)
	require.NoError(t, err)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()
	challenge := mfederation2.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	challengeMsg := generator2.NewFederationChallenge(t, organizer, value,
		validUntil, organizerSk)

	challengeMsg2 := generator2.NewFederationChallenge(t, organizer2, value,
		validUntil, organizer2Sk)

	federationInit := mfederation2.FederationInit{
		Object:        messagedata.FederationObject,
		Action:        messagedata.FederationActionInit,
		LaoId:         laoID,
		ServerAddress: serverAddressA,
		PublicKey:     organizer,
		ChallengeMsg:  challengeMsg,
	}

	federationResultMsg := generator2.NewSuccessFederationResult(t,
		organizer2, organizer, challengeMsg2, organizer2Sk)

	db.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	db.On("StoreMessageAndData", channelPath,
		federationResultMsg).Return(nil)
	db.On("GetFederationInit", organizer, organizer2, challenge,
		channelPath).Return(federationInit, nil)

	err = federationHandler.handleResult(federationResultMsg, channelPath)
	require.NoError(t, err)

	require.NotNil(t, fakeSocket.Msg)
	var broadcastMsg mbroadcast.Broadcast
	err = json.Unmarshal(fakeSocket.Msg, &broadcastMsg)
	require.NoError(t, err)

	require.Equal(t, mquery.MethodBroadcast, broadcastMsg.Method)
	require.Equal(t, federationResultMsg, broadcastMsg.Params.Message)
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

func websocketHandler(t *testing.T, msgCh chan []byte) func(w http.ResponseWriter, r *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		conn, err := upgrader.Upgrade(w, r, nil)
		require.NoError(t, err)
		defer conn.Close()

		for {
			mt, msg, err := conn.ReadMessage()
			require.NoError(t, err)

			require.Equal(t, websocket.TextMessage, mt)
			msgCh <- msg
		}
	}
}

func websocketServer(t *testing.T, server *http.Server, started chan struct{}) {
	l, err := net.Listen("tcp", server.Addr)
	require.NoError(t, err)

	started <- struct{}{}

	err = server.Serve(l)
	require.ErrorIs(t, http.ErrServerClosed, err)
}
