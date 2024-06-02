package channel

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
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query"
	"popstellar/internal/message/query/method"
	mock2 "popstellar/internal/mocks"
	"popstellar/internal/mocks/generator"
	"popstellar/internal/singleton/config"
	"popstellar/internal/singleton/database"
	"popstellar/internal/singleton/state"
	"popstellar/internal/types"
	"testing"
	"time"
)

func Test_handleChannelFederation(t *testing.T) {
	var args []input

	mockRepository := mock2.NewRepository(t)
	database.SetDatabase(mockRepository)

	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state.SetState(subs, peers, queries, hubParams)

	organizerPk, organizerSk := generateKeys()
	organizer2Pk, organizer2Sk := generateKeys()
	notOrganizerPk, notOrganizerSk := generateKeys()
	serverPk, serverSk := generateKeys()

	config.SetConfig(organizerPk, serverPk, serverSk, "client", "server")

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

	mockRepository.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)

	// Test 1 Error when FederationChallengeRequest sender is not the same as
	// the lao organizer
	args = append(args, input{
		name:        "Test 1",
		channelPath: channelPath,
		msg: generator.NewFederationChallengeRequest(t,
			notOrganizer, validUntil, notOrganizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channelPath",
	})

	// Test 2 Error when FederationChallengeRequest timestamp is negative
	args = append(args, input{
		name:        "Test 2",
		channelPath: channelPath,
		msg: generator.NewFederationChallengeRequest(t,
			organizer, -1, organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 3 Error when FederationExpect sender is not the same as the lao
	// organizer
	args = append(args, input{
		name:        "Test 3",
		channelPath: channelPath,
		msg: generator.NewFederationExpect(t, notOrganizer, laoID,
			serverAddressA, organizer2,
			generator.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			notOrganizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channelPath",
	})

	// Test 4 Error when FederationExpect serverAddress is not valid format
	args = append(args, input{
		name:        "Test 4",
		channelPath: channelPath,
		msg: generator.NewFederationExpect(t, organizer, laoID,
			"ws:localhost:12345/client", organizer2,
			generator.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 5 Error when FederationExpect publicKey is not valid format
	args = append(args, input{
		name:        "Test 5",
		channelPath: channelPath,
		msg: generator.NewFederationExpect(t, organizer, laoID,
			serverAddressA, "organizer2",
			generator.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 6 Error when FederationExpect laoId is not valid format
	args = append(args, input{
		name:        "Test 6",
		channelPath: channelPath,
		msg: generator.NewFederationExpect(t, organizer, "laoID",
			serverAddressA, organizer2,
			generator.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 7 Error when FederationExpect challenge message is not a challenge
	args = append(args, input{
		name:        "Test 7",
		channelPath: channelPath,
		msg: generator.NewFederationExpect(t, organizer, laoID,
			serverAddressA, organizer2,
			generator.NewFederationChallengeRequest(t, organizer,
				validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "invalid message",
	})

	// Test 8 Error when FederationExpect challenge is not from organizer
	args = append(args, input{
		name:        "Test 8",
		channelPath: channelPath,
		msg: generator.NewFederationExpect(t, organizer, laoID,
			serverAddressA, organizer2,
			generator.NewFederationChallenge(t, notOrganizer,
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
		msg: generator.NewFederationInit(t, notOrganizer, laoID,
			serverAddressA, organizer2,
			generator.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			notOrganizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channelPath",
	})

	// Test 10 Error when FederationInit serverAddress is not valid format
	args = append(args, input{
		name:        "Test 10",
		channelPath: channelPath,
		msg: generator.NewFederationInit(t, organizer, laoID,
			"ws:localhost:12345/client", organizer2,
			generator.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 11 Error when FederationInit publicKey is not valid format
	args = append(args, input{
		name:        "Test 11",
		channelPath: channelPath,
		msg: generator.NewFederationInit(t, organizer, laoID,
			serverAddressA, "organizer2",
			generator.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 12 Error when FederationInit laoId is not valid format
	args = append(args, input{
		name:        "Test 12",
		channelPath: channelPath,
		msg: generator.NewFederationInit(t, organizer, "laoID",
			serverAddressA, organizer2,
			generator.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 13 Error when FederationInit challenge message is not a challenge
	args = append(args, input{
		name:        "Test 13",
		channelPath: channelPath,
		msg: generator.NewFederationInit(t, organizer, laoID,
			serverAddressA, organizer2,
			generator.NewFederationChallengeRequest(t, organizer,
				validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "invalid message",
	})

	// Test 14 Error when FederationInit challenge is not from organizer
	args = append(args, input{
		name:        "Test 14",
		channelPath: channelPath,
		msg: generator.NewFederationInit(t, organizer, laoID,
			serverAddressA, organizer2,
			generator.NewFederationChallenge(t, notOrganizer,
				value, validUntil, notOrganizerSk),
			organizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channelPath",
	})

	federationChallenge1 := messagedata.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	mockRepository.On("GetFederationExpect", organizer,
		notOrganizer, federationChallenge1, channelPath).Return(messagedata.
		FederationExpect{}, sql.ErrNoRows)

	// Test 15 Error when FederationChallenge is received without any
	// matching FederationExpect
	args = append(args, input{
		name:        "Test 15",
		channelPath: channelPath,
		msg: generator.NewFederationChallenge(t, notOrganizer, value,
			validUntil, notOrganizerSk),
		isError:  true,
		contains: "failed to get federation expect",
	})

	// Test 16 Error when FederationResult challenge message is not a challenge
	args = append(args, input{
		name:        "Test 16",
		channelPath: channelPath,
		msg: generator.NewSuccessFederationResult(t, organizer2,
			organizer, generator.NewFederationChallengeRequest(t,
				organizer2, validUntil, organizer2Sk), organizer2Sk),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 17 Error when FederationResult PublicKey is not the organizerPk
	args = append(args, input{
		name:        "Test 17",
		channelPath: channelPath,
		msg: generator.NewSuccessFederationResult(t, organizer2,
			notOrganizer, generator.NewFederationChallenge(t, organizer2,
				value, validUntil, organizer2Sk), organizer2Sk),
		isError:  true,
		contains: "invalid message field",
	})

	mockRepository.On("GetFederationInit", organizer,
		organizer2, federationChallenge1, channelPath).Return(messagedata.
		FederationInit{}, sql.ErrNoRows)

	// Test 18 Error when FederationResult is received without any
	// matching FederationInit
	args = append(args, input{
		name:        "Test 18",
		channelPath: channelPath,
		msg: generator.NewSuccessFederationResult(t, organizer2,
			organizer, generator.NewFederationChallenge(t, organizer2,
				value, validUntil, organizer2Sk), organizer2Sk),
		isError:  true,
		contains: "failed to get federation init",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelFederation(arg.channelPath, arg.msg)
			if arg.isError {
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
			}
		})
	}
}

func Test_handleRequestChallenge(t *testing.T) {
	mockRepository := mock2.NewRepository(t)
	database.SetDatabase(mockRepository)

	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state.SetState(subs, peers, queries, hubParams)

	organizerPk, organizerSk := generateKeys()
	serverPk, serverSk := generateKeys()

	config.SetConfig(organizerPk, serverPk, serverSk, "client", "server")

	organizerBuf, err := organizerPk.MarshalBinary()
	require.NoError(t, err)
	organizer := base64.URLEncoding.EncodeToString(organizerBuf)

	laoID := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	laoPath := fmt.Sprintf("/root/%s", laoID)
	channelPath := fmt.Sprintf("/root/%s/federation", laoID)

	errAnswer := subs.AddChannel(channelPath)
	require.Nil(t, errAnswer)

	fakeSocket := mock2.FakeSocket{Id: "1"}
	errAnswer = subs.Subscribe(channelPath, &fakeSocket)
	require.Nil(t, errAnswer)

	mockRepository.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	mockRepository.On("GetServerKeys").Return(serverPk, serverSk, nil)
	mockRepository.On("StoreMessageAndData", channelPath,
		mock.AnythingOfType("message.Message")).Return(nil)

	errAnswer = handleRequestChallenge(generator.NewFederationChallengeRequest(t, organizer, time.Now().Unix(),
		organizerSk), channelPath)
	require.Nil(t, errAnswer)

	require.NotNil(t, fakeSocket.Msg)
	var broadcastMsg method.Broadcast
	err = json.Unmarshal(fakeSocket.Msg, &broadcastMsg)
	require.NoError(t, err)

	require.Equal(t, "broadcast", broadcastMsg.Method)
	require.Equal(t, channelPath, broadcastMsg.Params.Channel)

	var challenge messagedata.FederationChallenge
	errAnswer = broadcastMsg.Params.Message.UnmarshalMsgData(&challenge)
	require.Nil(t, errAnswer)

	errAnswer = challenge.Verify()
	require.Nil(t, errAnswer)
}

func Test_handleFederationExpect(t *testing.T) {
	mockRepository := mock2.NewRepository(t)
	database.SetDatabase(mockRepository)

	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state.SetState(subs, peers, queries, hubParams)

	organizerPk, organizerSk := generateKeys()
	organizer2Pk, _ := generateKeys()
	serverPk, serverSk := generateKeys()

	config.SetConfig(organizerPk, serverPk, serverSk, "client", "server")

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

	federationChallenge := messagedata.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	mockRepository.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	mockRepository.On("GetServerKeys").Return(serverPk, serverSk, nil)
	mockRepository.On("StoreMessageAndData", channelPath,
		mock.AnythingOfType("message.Message")).Return(nil)

	mockRepository.On("IsChallengeValid", server, federationChallenge,
		channelPath).Return(nil)

	federationExpect := generator.NewFederationExpect(t, organizer, laoID,
		serverAddressA, organizer2, generator.NewFederationChallenge(t,
			organizer, value, validUntil, organizerSk), organizerSk)

	errAnswer := handleExpect(federationExpect, channelPath)
	require.Nil(t, errAnswer)
}

func Test_handleFederationInit(t *testing.T) {
	mockRepository := mock2.NewRepository(t)
	database.SetDatabase(mockRepository)

	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state.SetState(subs, peers, queries, hubParams)

	organizerPk, organizerSk := generateKeys()
	organizer2Pk, _ := generateKeys()
	serverPk, serverSk := generateKeys()

	config.SetConfig(organizerPk, serverPk, serverSk, "client", "server")

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

	challengeMsg := generator.NewFederationChallenge(t, organizer, value,
		validUntil, organizerSk)

	initMsg := generator.NewFederationInit(t, organizer, laoID2,
		serverAddressA, organizer2, challengeMsg, organizerSk)

	mockRepository.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	mockRepository.On("StoreMessageAndData", channelPath, initMsg).Return(nil)

	serverBStarted := make(chan struct{})
	msgChan := make(chan []byte, 10)
	mux := http.NewServeMux()
	mux.HandleFunc("/client", websocketHandler(t, msgChan))
	serverB := &http.Server{Addr: "localhost:9801", Handler: mux}

	go websocketServer(t, serverB, serverBStarted)
	<-serverBStarted
	defer serverB.Close()

	errAnswer := handleInit(initMsg, channelPath)
	require.Nil(t, errAnswer)

	var msgBytes []byte
	select {
	case msgBytes = <-msgChan:
	case <-time.After(time.Second):
		require.Fail(t, "Timed out waiting for expected message")
	}

	var subMsg method.Subscribe
	err = json.Unmarshal(msgBytes, &subMsg)
	require.NoError(t, err)
	require.Equal(t, query.MethodSubscribe, subMsg.Method)
	require.Equal(t, method.SubscribeParams{Channel: channelPath}, subMsg.Params)

	select {
	case msgBytes = <-msgChan:
	case <-time.After(time.Second):
		require.Fail(t, "Timed out waiting for expected message")
	}
	var publishMsg method.Publish
	err = json.Unmarshal(msgBytes, &publishMsg)
	require.NoError(t, err)
	require.Equal(t, query.MethodPublish, publishMsg.Method)
	require.Equal(t, channelPath2, publishMsg.Params.Channel)
	require.Equal(t, challengeMsg, publishMsg.Params.Message)
}

func Test_handleFederationChallenge(t *testing.T) {
	mockRepository := mock2.NewRepository(t)
	database.SetDatabase(mockRepository)

	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state.SetState(subs, peers, queries, hubParams)

	organizerPk, organizerSk := generateKeys()
	organizer2Pk, organizer2Sk := generateKeys()
	serverPk, serverSk := generateKeys()

	config.SetConfig(organizerPk, serverPk, serverSk, "client", "server")

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

	errAnswer := subs.AddChannel(channelPath)
	require.Nil(t, errAnswer)
	errAnswer = subs.AddChannel(channelPath2)
	require.Nil(t, errAnswer)

	fakeSocket1 := mock2.FakeSocket{Id: "1"}
	errAnswer = subs.Subscribe(channelPath, &fakeSocket1)
	require.Nil(t, errAnswer)

	fakeSocket2 := mock2.FakeSocket{Id: "2"}
	errAnswer = subs.Subscribe(channelPath2, &fakeSocket2)
	require.Nil(t, errAnswer)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()
	challenge := messagedata.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	challengeMsg := generator.NewFederationChallenge(t, organizer, value,
		validUntil, organizerSk)

	challengeMsg2 := generator.NewFederationChallenge(t, organizer2,
		value, validUntil, organizer2Sk)

	federationExpect := messagedata.FederationExpect{
		Object:        messagedata.FederationObject,
		Action:        messagedata.FederationActionExpect,
		LaoId:         laoID2,
		ServerAddress: serverAddressA,
		PublicKey:     organizer2,
		ChallengeMsg:  challengeMsg,
	}

	mockRepository.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	mockRepository.On("StoreMessageAndData", channelPath,
		mock.AnythingOfType("message.Message")).Return(nil)
	mockRepository.On("GetFederationExpect", organizer, organizer2,
		challenge, channelPath).Return(federationExpect, nil)
	mockRepository.On("RemoveChallenge", challenge).Return(nil)
	mockRepository.On("GetServerKeys").Return(serverPk, serverSk, nil)

	errAnswer = handleChallenge(challengeMsg2, channelPath)
	require.Nil(t, errAnswer)

	// The same federation result message should be received by both sockets
	// on fakeSocket1, representing the organizer, it should be in a broadcast
	require.NotNil(t, fakeSocket1.Msg)
	var broadcastMsg method.Broadcast
	err = json.Unmarshal(fakeSocket1.Msg, &broadcastMsg)
	require.NoError(t, err)
	require.Equal(t, query.MethodBroadcast, broadcastMsg.Method)

	// on fakeSocket2, representing the other server, it should in a publish
	require.NotNil(t, fakeSocket2.Msg)
	var publishMsg method.Publish
	err = json.Unmarshal(fakeSocket2.Msg, &publishMsg)
	require.NoError(t, err)
	require.Equal(t, query.MethodPublish, publishMsg.Method)
	require.Equal(t, broadcastMsg.Params.Message, publishMsg.Params.Message)

	var resultMsg messagedata.FederationResult
	errAnswer = broadcastMsg.Params.Message.UnmarshalMsgData(&resultMsg)
	require.Nil(t, errAnswer)

	// it should contain the challenge from organizer, not organizer2
	require.Equal(t, challengeMsg, resultMsg.ChallengeMsg)
	require.Equal(t, "success", resultMsg.Status)
	require.Empty(t, resultMsg.Reason)
	require.Equal(t, organizer2, resultMsg.PublicKey)
}

func Test_handleFederationResult(t *testing.T) {
	mockRepository := mock2.NewRepository(t)
	database.SetDatabase(mockRepository)

	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	organizerPk, organizerSk := generateKeys()
	organizer2Pk, organizer2Sk := generateKeys()
	serverPk, serverSk := generateKeys()

	config.SetConfig(organizerPk, serverPk, serverSk, "client", "server")

	state.SetState(subs, peers, queries, hubParams)

	organizerBuf, err := organizerPk.MarshalBinary()
	require.NoError(t, err)
	organizer := base64.URLEncoding.EncodeToString(organizerBuf)

	organizer2Buf, err := organizer2Pk.MarshalBinary()
	require.NoError(t, err)
	organizer2 := base64.URLEncoding.EncodeToString(organizer2Buf)

	laoID := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	laoPath := fmt.Sprintf("/root/%s", laoID)
	channelPath := fmt.Sprintf("/root/%s/federation", laoID)

	errAnswer := subs.AddChannel(channelPath)
	require.Nil(t, errAnswer)

	fakeSocket := mock2.FakeSocket{Id: "1"}
	errAnswer = subs.Subscribe(channelPath, &fakeSocket)
	require.Nil(t, errAnswer)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()
	challenge := messagedata.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	challengeMsg := generator.NewFederationChallenge(t, organizer, value,
		validUntil, organizerSk)

	challengeMsg2 := generator.NewFederationChallenge(t, organizer2, value,
		validUntil, organizer2Sk)

	federationInit := messagedata.FederationInit{
		Object:        messagedata.FederationObject,
		Action:        messagedata.FederationActionInit,
		LaoId:         laoID,
		ServerAddress: serverAddressA,
		PublicKey:     organizer,
		ChallengeMsg:  challengeMsg,
	}

	federationResultMsg := generator.NewSuccessFederationResult(t,
		organizer2, organizer, challengeMsg2, organizer2Sk)

	mockRepository.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)
	mockRepository.On("StoreMessageAndData", channelPath,
		federationResultMsg).Return(nil)
	mockRepository.On("GetFederationInit", organizer, organizer2, challenge,
		channelPath).Return(federationInit, nil)

	errAnswer = handleResult(federationResultMsg, channelPath)
	require.Nil(t, errAnswer)

	require.NotNil(t, fakeSocket.Msg)
	var broadcastMsg method.Broadcast
	err = json.Unmarshal(fakeSocket.Msg, &broadcastMsg)
	require.NoError(t, err)

	require.Equal(t, query.MethodBroadcast, broadcastMsg.Method)
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
