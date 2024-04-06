package channel

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	"popstellar/internal/popserver"
	"popstellar/internal/popserver/repo"
	"popstellar/internal/popserver/state"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"testing"
	"time"
)

func Test_handleChannelRoot(t *testing.T) {
	keypair := popserver.GenerateKeyPair(t)
	serverkeyPair := popserver.GenerateKeyPair(t)
	now := time.Now().Unix()
	name := "LAO X"
	laoID := messagedata.Hash(base64.URLEncoding.EncodeToString(keypair.PublicBuf), fmt.Sprintf("%d", now), name)

	data := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.PublicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)
	signature, err := schnorr.Sign(crypto.Suite, keypair.Private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.PublicBuf),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	laoPath := rootPrefix + laoID

	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(nil, nil)
	mockRepository.On("StoreChannelsAndMessageWithLaoGreet",
		mock.AnythingOfType("[]string"),
		mock.AnythingOfType("string"), laoPath, mock.AnythingOfType("string"),
		mock.AnythingOfType("[]uint8"),
		mock.AnythingOfType("message.Message"), mock.AnythingOfType("message.Message")).Return(nil)
	mockRepository.On("GetServerPubKey").Return(serverkeyPair.PublicBuf, nil)
	mockRepository.On("GetServerSecretKey").Return(serverkeyPair.PrivateBuf, nil)
	params := popserver.NewHandlerParameters(mockRepository)

	errAnswer := handleChannelRoot(params, "/root", msg)
	require.Nil(t, errAnswer)
}

func Test_verifyLaoCreation(t *testing.T) {
	keypair := popserver.GenerateKeyPair(t)
	wrongKeyPair := popserver.GenerateKeyPair(t)
	now := time.Now().Unix()
	name := "LAO X"
	laoID := messagedata.Hash(base64.URLEncoding.EncodeToString(keypair.PublicBuf), fmt.Sprintf("%d", now), name)

	laoCreate := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.PublicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(laoCreate)
	require.NoError(t, err)
	signature, err := schnorr.Sign(crypto.Suite, keypair.Private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.PublicBuf),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}
	laoPath := rootPrefix + laoID

	type verifyLaoCreationInputs struct {
		name      string
		params    state.HandlerParameters
		message   message.Message
		laoCreate messagedata.LaoCreate
	}
	var args []verifyLaoCreationInputs

	// Test 1: error when the lao id is not base64URL encoded
	wrongLaoCreate := laoCreate
	wrongLaoCreate.ID = "wrongID"

	params := popserver.NewHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 1",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 2: error when the lao id is not the expected one
	wrongLaoCreate = laoCreate
	wrongLaoCreate.ID = base64.URLEncoding.EncodeToString([]byte("wrongID"))

	params = popserver.NewHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 2",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 3: error when the lao name is empty
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Name = ""
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = popserver.NewHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 3",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 4: error when the lao creation is negative
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Creation = -1
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = popserver.NewHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 4",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 5: error when the organizer is not base64URL encoded
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = "wrongOrganizer"
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = popserver.NewHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 5",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 6: error when a witness is not base64URL encoded
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Witnesses = []string{"a wrong witness"}

	params = popserver.NewHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 6",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 7: error when the lao already exists
	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(true, nil)
	params = popserver.NewHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 7",
		params:    params,
		message:   msg,
		laoCreate: laoCreate})

	// Test 8: error when querying the channel
	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, fmt.Errorf("DB is disconnected"))
	params = popserver.NewHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 8",
		params:    params,
		message:   msg,
		laoCreate: laoCreate})

	// Test 9: error when the sender's public key is not base64URL encoded
	wrongMsg := msg
	wrongMsg.Sender = "wrongSender"

	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = popserver.NewHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 9",
		params:    params,
		message:   wrongMsg,
		laoCreate: laoCreate})

	// Test 10: error when the sender's public key is not unmarshable using Kyber
	wrongMsg = msg
	wrongMsg.Sender = base64.URLEncoding.EncodeToString([]byte("wrongSender"))

	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = popserver.NewHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 10",
		params:    params,
		message:   wrongMsg,
		laoCreate: laoCreate})

	// Test 11: error when the organizer's public key is not base64URL encoded
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = "wrongOrganizer"
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = popserver.NewHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 11",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 12: error when the organizer's public key is not unmarshable using Kyber
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = base64.URLEncoding.EncodeToString([]byte("wrongOrganizer"))
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = popserver.NewHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 12",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 13: error when the organizer's public key is not the same as the sender's public key
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = base64.URLEncoding.EncodeToString(wrongKeyPair.PublicBuf)
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = popserver.NewHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 13",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 14: error when querying the owner's public key
	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(nil, fmt.Errorf("DB is disconnected"))
	params = popserver.NewHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 14",
		params:    params,
		message:   msg,
		laoCreate: laoCreate})

	// Test 15: error when the owner's public key is not the same as the sender's public key
	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(wrongKeyPair.Public, nil)
	params = popserver.NewHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 15",
		params:    params,
		message:   msg,
		laoCreate: laoCreate})

	// Run the tests
	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			_, errAnswer := verifyLaoCreation(arg.params, arg.message, arg.laoCreate, laoPath)
			require.Error(t, errAnswer)
		})
	}

	// Test 16: success
	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(keypair.Public, nil)
	params = popserver.NewHandlerParameters(mockRepository)

	t.Run("Test 16", func(t *testing.T) {
		pubKeybuf, errAnswer := verifyLaoCreation(params, msg, laoCreate, laoPath)
		require.Nil(t, errAnswer)
		assert.Equal(t, keypair.PublicBuf, pubKeybuf)

	})
}

func Test_createLaoGreet(t *testing.T) {
	keypair := popserver.GenerateKeyPair(t)
	privateKeyBuf, err := keypair.Private.MarshalBinary()
	require.NoError(t, err)
	type createAndSendLaoGreetInputs struct {
		name      string
		params    state.HandlerParameters
		pubKeyBuf []byte
	}
	var args []createAndSendLaoGreetInputs
	laoPath := "laoPath"

	// Test 1: error when getting the server's public key
	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("GetServerPubKey").Return(nil, fmt.Errorf("DB is disconnected"))
	params := popserver.NewHandlerParameters(mockRepository)
	err = params.Peers.AddPeerInfo("socketID1", method.GreetServerParams{ClientAddress: "clientAddress1"})
	require.NoError(t, err)

	args = append(args, createAndSendLaoGreetInputs{name: "Test 1",
		params:    params,
		pubKeyBuf: keypair.PublicBuf})

	// Test 2: error when querying the server's secret key when signing the laoGreet message
	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("GetServerPubKey").Return(keypair.PublicBuf, nil)
	mockRepository.On("GetServerSecretKey").Return(nil, fmt.Errorf("DB is disconnected"))
	params = popserver.NewHandlerParameters(mockRepository)
	err = params.Peers.AddPeerInfo("socketID1", method.GreetServerParams{ClientAddress: "clientAddress1"})
	require.NoError(t, err)

	args = append(args, createAndSendLaoGreetInputs{name: "Test 2",
		params:    params,
		pubKeyBuf: keypair.PublicBuf})

	// Test 3: error when unmarshalling the server's secret key
	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("GetServerPubKey").Return(keypair.PublicBuf, nil)
	mockRepository.On("GetServerSecretKey").Return([]byte("wrongKey"), nil)
	params = popserver.NewHandlerParameters(mockRepository)
	err = params.Peers.AddPeerInfo("socketID1", method.GreetServerParams{ClientAddress: "clientAddress1"})
	require.NoError(t, err)

	args = append(args, createAndSendLaoGreetInputs{name: "Test 3",
		params:    params,
		pubKeyBuf: keypair.PublicBuf})

	// Run the tests
	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			laoGreetMsg, errAnswer := createLaoGreet(arg.params, arg.pubKeyBuf, laoPath)
			assert.Equal(t, message.Message{}, laoGreetMsg)
			require.Error(t, errAnswer)
		})
	}

	// Test 5: success
	mockRepository = repo.NewMockRepository(t)
	mockRepository.On("GetServerPubKey").Return(keypair.PublicBuf, nil)
	mockRepository.On("GetServerSecretKey").Return(privateKeyBuf, nil)
	params = popserver.NewHandlerParameters(mockRepository)
	err = params.Peers.AddPeerInfo("socketID1", method.GreetServerParams{ClientAddress: "clientAddress1"})
	require.NoError(t, err)
	params.Subs.AddChannel(laoPath)

	t.Run("Test 5", func(t *testing.T) {
		laoGreetMsg, errAnswer := createLaoGreet(params, keypair.PublicBuf, laoPath)
		require.Nil(t, errAnswer)
		require.NotNil(t, laoGreetMsg)
	})
}
