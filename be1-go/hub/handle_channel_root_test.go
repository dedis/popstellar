package hub

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/rs/zerolog/log"
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

func Test_MockExample(t *testing.T) {
	repo := mocks.NewRepository(t)
	repo.On("GetMessageByID", "messageID1").Return(message.Message{Data: "data1",
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}, nil)

	repo.On("GetMessageByID", "messageID2").Return(message.Message{Data: "data1",
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID2",
		WitnessSignatures: []message.WitnessSignature{},
	}, nil)

	msg, err := repo.GetMessageByID("messageID1")
	if err != nil {
		return
	}
	log.Info().Msg(msg.MessageID)

	msg, err = repo.GetMessageByID("messageID2")
	if err != nil {
		return
	}
	log.Info().Msg(msg.MessageID)
}

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
	mockRepository.On("StoreChannel", mock.AnythingOfType("string"), mock.AnythingOfType("[]uint8")).Return(nil)
	mockRepository.On("GetServerPubKey").Return(nil, nil)
	mockRepository.On("GetServerSecretKey").Return(nil, nil)

	params := newHandlerParameters(mockRepository)

	errAnswer := handleChannelRoot(params, "/root", msg)
	if errAnswer != nil {
		log.Error().Msg(errAnswer.Error())
	} else {
		log.Info().Msg("Success")
	}
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

	// Test 1: error when the lao id is not base64URL encoded
	wrongLaoCreate := laoCreate
	wrongLaoCreate.ID = "wrongID"

	params := newHandlerParameters(nil)

	_, errAnswer := verifyLAOCreation(params, msg, wrongLaoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 2: error when the lao id is not the expected one
	wrongLaoCreate = laoCreate
	wrongLaoCreate.ID = base64.URLEncoding.EncodeToString([]byte("wrongID"))

	params = newHandlerParameters(nil)

	_, errAnswer = verifyLAOCreation(params, msg, wrongLaoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 3: error when the lao name is empty
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Name = ""
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = newHandlerParameters(nil)

	_, errAnswer = verifyLAOCreation(params, msg, wrongLaoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 4: error when the lao creation is negative
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Creation = -1
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = newHandlerParameters(nil)

	_, errAnswer = verifyLAOCreation(params, msg, wrongLaoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 5: error when the organizer is not base64URL encoded
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = "wrongOrganizer"
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = newHandlerParameters(nil)

	_, errAnswer = verifyLAOCreation(params, msg, wrongLaoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 6: error when a witness is not base64URL encoded
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Witnesses = []string{"a wrong witness"}

	params = newHandlerParameters(nil)

	_, errAnswer = verifyLAOCreation(params, msg, wrongLaoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 7: error when the lao already exists
	mockRepository := mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(true, nil)
	params = newHandlerParameters(mockRepository)

	_, errAnswer = verifyLAOCreation(params, msg, laoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 8: error when querying the channel
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	_, errAnswer = verifyLAOCreation(params, msg, laoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 9: error when the sender's public key is not base64URL encoded
	wrongMsg := msg
	wrongMsg.Sender = "wrongSender"

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = newHandlerParameters(mockRepository)

	_, errAnswer = verifyLAOCreation(params, wrongMsg, laoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 10: error when the sender's public key is not unmarshable using Kyber
	wrongMsg = msg
	wrongMsg.Sender = base64.URLEncoding.EncodeToString([]byte("wrongSender"))

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = newHandlerParameters(mockRepository)

	_, errAnswer = verifyLAOCreation(params, wrongMsg, laoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 11: error when the organizer's public key is not base64URL encoded
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = "wrongOrganizer"
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	params = newHandlerParameters(nil)

	_, errAnswer = verifyLAOCreation(params, msg, wrongLaoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 12: error when the organizer's public key is not unmarshable using Kyber
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = base64.URLEncoding.EncodeToString([]byte("wrongOrganizer"))
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = newHandlerParameters(mockRepository)

	_, errAnswer = verifyLAOCreation(params, msg, wrongLaoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 13: error when the organizer's public key is not the same as the sender's public key
	wrongLaoCreate = laoCreate
	wrongLaoCreate.Organizer = base64.URLEncoding.EncodeToString(wrongKeyPair.publicBuf)
	wrongLaoCreate.ID = messagedata.Hash(wrongLaoCreate.Organizer, fmt.Sprintf("%d", wrongLaoCreate.Creation), wrongLaoCreate.Name)

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = newHandlerParameters(mockRepository)

	_, errAnswer = verifyLAOCreation(params, msg, wrongLaoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 14: error when querying the owner's public key
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(nil, fmt.Errorf("db is disconnected"))
	params = newHandlerParameters(mockRepository)

	_, errAnswer = verifyLAOCreation(params, msg, laoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 15: error when the owner's public key is not the same as the sender's public key
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("GetOwnerPubKey").Return(wrongKeyPair.public, nil)
	params = newHandlerParameters(mockRepository)

	_, errAnswer = verifyLAOCreation(params, msg, laoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)
}
