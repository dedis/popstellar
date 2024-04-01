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
	now := time.Now().Unix()
	name := "LAO X"
	laoID := messagedata.Hash(base64.URLEncoding.EncodeToString(keypair.publicBuf), fmt.Sprintf("%d", now), name)

	// Test 1: verifyLaoCreation should return a "duplicate" error when the lao already exists

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
	mockRepository := mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(true, nil)
	params := newHandlerParameters(mockRepository)
	_, errAnswer := verifyLAOCreation(params, msg, laoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// Test 2: verifyLaoCreation should return an error when the lao id is not base64URL encoded

	wrongKeyPair := generateKeyPair(t)
	msgWrongSender := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(wrongKeyPair.publicBuf),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}
	mockRepository = mocks.NewRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	params = newHandlerParameters(mockRepository)
	_, errAnswer = verifyLAOCreation(params, msgWrongSender, laoCreate, laoPath)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

}
