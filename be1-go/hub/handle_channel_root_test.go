package hub

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	"popstellar/hub/mocks"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
	"time"
)

func Test_handleChannelRoot(t *testing.T) {
	keypair := generateKeyPair(t)
	now := time.Now().Unix()
	name := "LAO X"
	laoID := messagedata.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	data := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)
	signature, err := schnorr.Sign(crypto.Suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	laoPath := rootPrefix + laoID

	mockRepository := mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(nil, nil)
	mockRepository.On("StoreMessage", mock.AnythingOfType("string"), mock.AnythingOfType("message.Message")).Return(nil)
	mockRepository.On("StoreMessageID", mock.AnythingOfType("string"), mock.AnythingOfType("string")).Return(nil)
	mockRepository.On("StoreChannel", mock.AnythingOfType("string"), mock.AnythingOfType("[]uint8")).Return(nil)
	mockRepository.On("GetServerPubKey").Return(nil, nil)
	mockRepository.On("GetServerSecretKey").Return(nil, nil)
	socket := &fakeSocket{}
	params := newHandlerParametersWithFakeSocket(mockRepository, socket)

	errAnswer := handleChannelRoot(params, "/root", msg)
	require.Nil(t, errAnswer)
}

func Test_verifyLaoCreation(t *testing.T) {
	keypair := generateKeyPair(t)
	wrongKeyPair := generateKeyPair(t)
	now := time.Now().Unix()
	name := "LAO X"
	laoID := messagedata.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	laoCreate := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(laoCreate)
	require.NoError(t, err)
	signature, err := schnorr.Sign(crypto.Suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}
	laoPath := rootPrefix + laoID

	type verifyLaoCreationInputs struct {
		name      string
		params    handlerParameters
		message   message.Message
		laoCreate messagedata.LaoCreate
	}
	var args []verifyLaoCreationInputs

	// Test 1: error when the lao id is not base64URL encoded
	wrongLaoCreate := laoCreate
	wrongLaoCreate.ID = "wrongID"

	params := newHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 1",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 2: error when the lao id is not the expected one
	wrongLaoCreate = laoCreate
	wrongLaoCreate.ID = base64.URLEncoding.EncodeToString([]byte("wrongID"))

	params = newHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 2",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 3: error when the lao name is empty
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Name = ""
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = newHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 3",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 4: error when the lao creation is negative
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Creation = -1
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = newHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 4",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 5: error when the organizer is not base64URL encoded
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = "wrongOrganizer"
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = newHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 5",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 6: error when a witness is not base64URL encoded
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Witnesses = []string{"a wrong witness"}

	params = newHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 6",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 7: error when the lao already exists
	mockRepository := mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(true, nil)
	params = newHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 7",
		params:    params,
		message:   msg,
		laoCreate: laoCreate})

	// Test 8: error when querying the channel
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 8",
		params:    params,
		message:   msg,
		laoCreate: laoCreate})

	// Test 9: error when the sender's public key is not base64URL encoded
	wrongMsg := msg
	wrongMsg.Sender = "wrongSender"

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = newHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 9",
		params:    params,
		message:   wrongMsg,
		laoCreate: laoCreate})

	// Test 10: error when the sender's public key is not unmarshable using Kyber
	wrongMsg = msg
	wrongMsg.Sender = base64.URLEncoding.EncodeToString([]byte("wrongSender"))

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = newHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 10",
		params:    params,
		message:   wrongMsg,
		laoCreate: laoCreate})

	// Test 11: error when the organizer's public key is not base64URL encoded
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = "wrongOrganizer"
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = newHandlerParameters(nil)

	args = append(args, verifyLaoCreationInputs{name: "Test 11",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 12: error when the organizer's public key is not unmarshable using Kyber
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = base64.URLEncoding.EncodeToString([]byte("wrongOrganizer"))
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = newHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 12",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 13: error when the organizer's public key is not the same as the sender's public key
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = base64.URLEncoding.EncodeToString(wrongKeyPair.publicBuf)
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = newHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 13",
		params:    params,
		message:   msg,
		laoCreate: wrongLaoCreate})

	// Test 14: error when querying the owner's public key
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(nil, fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	args = append(args, verifyLaoCreationInputs{name: "Test 14",
		params:    params,
		message:   msg,
		laoCreate: laoCreate})

	// Test 15: error when the owner's public key is not the same as the sender's public key
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(wrongKeyPair.public, nil)
	params = newHandlerParameters(mockRepository)

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
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(keypair.public, nil)
	params = newHandlerParameters(mockRepository)

	t.Run("Test 16", func(t *testing.T) {
		pubKeybuf, errAnswer := verifyLaoCreation(params, msg, laoCreate, laoPath)
		require.Nil(t, errAnswer)
		assert.Equal(t, keypair.publicBuf, pubKeybuf)

	})
}

func Test_createLaoAndSubChannels(t *testing.T) {
	keypair := generateKeyPair(t)
	now := time.Now().Unix()
	name := "LAO X"
	laoID := messagedata.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	laoCreate := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(laoCreate)
	require.NoError(t, err)
	signature, err := schnorr.Sign(crypto.Suite, keypair.private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msg := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}
	laoPath := rootPrefix + laoID

	type createLaoInputs struct {
		name      string
		params    handlerParameters
		msg       message.Message
		pubKeyBuf []byte
	}
	var args []createLaoInputs

	// Test 1: error when storing the lao channel
	mockRepository := mocks.NewRepository(t)
	mockRepository.On("StoreChannel", laoPath, keypair.publicBuf).Return(fmt.Errorf("db is disconnected"))
	params := newHandlerParameters(mockRepository)

	args = append(args, createLaoInputs{name: "Test 1",
		params:    params,
		msg:       msg,
		pubKeyBuf: keypair.publicBuf})

	// Test 2: error when storing the message in the lao channel
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("StoreChannel", laoPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreMessageID", msg.MessageID, laoPath).Return(fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	args = append(args, createLaoInputs{name: "Test 2",
		params:    params,
		msg:       msg,
		pubKeyBuf: keypair.publicBuf})

	// Test 3 error when the storing general chirping channel:
	generalChirpingPath := laoPath + social + chirps
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("StoreChannel", laoPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreMessageID", msg.MessageID, laoPath).Return(nil)
	mockRepository.On("StoreChannel", generalChirpingPath, keypair.publicBuf).Return(fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	args = append(args, createLaoInputs{name: "Test 3",
		params:    params,
		msg:       msg,
		pubKeyBuf: keypair.publicBuf})

	// Test 4: error when storing the reactions channel
	reactionsPath := laoPath + social + reactions
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("StoreChannel", laoPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreMessageID", msg.MessageID, laoPath).Return(nil)
	mockRepository.On("StoreChannel", generalChirpingPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", reactionsPath, keypair.publicBuf).Return(fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	args = append(args, createLaoInputs{name: "Test 4",
		params:    params,
		msg:       msg,
		pubKeyBuf: keypair.publicBuf})

	// Test 5: error when storing the consensus channel
	consensusPath := laoPath + consensus
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("StoreChannel", laoPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreMessageID", msg.MessageID, laoPath).Return(nil)
	mockRepository.On("StoreChannel", generalChirpingPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", reactionsPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", consensusPath, keypair.publicBuf).Return(fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	args = append(args, createLaoInputs{name: "Test 5",
		params:    params,
		msg:       msg,
		pubKeyBuf: keypair.publicBuf})

	// Test 6: error when storing the coin channel
	coinPath := laoPath + coin
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("StoreChannel", laoPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreMessageID", msg.MessageID, laoPath).Return(nil)
	mockRepository.On("StoreChannel", generalChirpingPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", reactionsPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", consensusPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", coinPath, keypair.publicBuf).Return(fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	args = append(args, createLaoInputs{name: "Test 6",
		params:    params,
		msg:       msg,
		pubKeyBuf: keypair.publicBuf})

	// Test 7: error when storing the auth channel
	authPath := laoPath + auth
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("StoreChannel", laoPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreMessageID", msg.MessageID, laoPath).Return(nil)
	mockRepository.On("StoreChannel", generalChirpingPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", reactionsPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", consensusPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", coinPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", authPath, keypair.publicBuf).Return(fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	args = append(args, createLaoInputs{name: "Test 7",
		params:    params,
		msg:       msg,
		pubKeyBuf: keypair.publicBuf})

	// Run the tests
	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := createLaoAndSubChannels(arg.params, arg.msg, arg.pubKeyBuf, laoPath)
			require.Error(t, errAnswer)
		})
	}

	// Test 8: success
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("StoreChannel", laoPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreMessageID", msg.MessageID, laoPath).Return(nil)
	mockRepository.On("StoreChannel", generalChirpingPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", reactionsPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", consensusPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", coinPath, keypair.publicBuf).Return(nil)
	mockRepository.On("StoreChannel", authPath, keypair.publicBuf).Return(nil)
	params = newHandlerParameters(mockRepository)

	t.Run("Test 8", func(t *testing.T) {
		errAnswer := createLaoAndSubChannels(params, msg, keypair.publicBuf, laoPath)
		require.Nil(t, errAnswer)
		assert.Equal(t, 6, len(params.subs))
	})
}
