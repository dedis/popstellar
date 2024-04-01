package hub

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/hub/mocks"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
	"time"
)

func Test_handleChannel(t *testing.T) {
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

	// unknown channelType

	wrongChannelID := "wrongChannelID"

	mockRepository := mocks.NewRepository(t)
	params := newHandlerParameters(mockRepository)

	mockRepository.On("HasMessage", msg.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", wrongChannelID).Return("", nil)

	errAnswer := handleChannel(params, wrongChannelID, msg)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// error while querying the channelType

	problemDBChannelID := "problemDBChannelID"

	mockRepository = mocks.NewRepository(t)
	params = newHandlerParameters(mockRepository)

	mockRepository.On("HasMessage", msg.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", problemDBChannelID).Return("", xerrors.Errorf("db disconnected"))

	errAnswer = handleChannel(params, problemDBChannelID, msg)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// message already received

	mockRepository = mocks.NewRepository(t)
	params = newHandlerParameters(mockRepository)

	mockRepository.On("HasMessage", msg.MessageID).Return(true, nil)

	errAnswer = handleChannel(params, "", msg)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// error while querying if the message already exists

	mockRepository = mocks.NewRepository(t)
	params = newHandlerParameters(mockRepository)

	mockRepository.On("HasMessage", msg.MessageID).Return(false, xerrors.Errorf("db disconnected"))

	errAnswer = handleChannel(params, "", msg)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// wrong messageID

	msgWrongID := msg
	msgWrongID.MessageID = messagedata.Hash("wrong messageID")

	params = newHandlerParameters(nil)

	errAnswer = handleChannel(params, "", msgWrongID)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// failed signature check because wrong sender

	wrongKeypair := generateKeyPair(t)
	msgWrongSender := msg
	msgWrongSender.Sender = base64.URLEncoding.EncodeToString(wrongKeypair.publicBuf)

	params = newHandlerParameters(nil)

	errAnswer = handleChannel(params, "", msgWrongSender)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// failed signature check because wrong data

	msgWrongData := msg
	msgWrongData.Data = base64.URLEncoding.EncodeToString([]byte("wrong data"))

	params = newHandlerParameters(nil)

	errAnswer = handleChannel(params, "", msgWrongData)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// failed signature check because wrong signature

	wrongKeypair = generateKeyPair(t)
	wrongSignature, err := schnorr.Sign(crypto.Suite, wrongKeypair.private, dataBuf)
	require.NoError(t, err)

	msgWrongSign := msg
	msgWrongSign.Signature = base64.URLEncoding.EncodeToString(wrongSignature)

	params = newHandlerParameters(nil)

	errAnswer = handleChannel(params, "", msgWrongSign)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// wrong signature encoding

	msgWrongSignEncoding := msg
	msgWrongSignEncoding.Signature = "wrong encoding"

	params = newHandlerParameters(nil)

	errAnswer = handleChannel(params, "", msgWrongSignEncoding)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// wrong sender encoding

	msgWrongSenderEncoding := msg
	msgWrongSenderEncoding.Sender = "wrong encoding"

	params = newHandlerParameters(nil)

	errAnswer = handleChannel(params, "", msgWrongSenderEncoding)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)

	// wrong data encoding

	msgWrongDataEncoding := msg
	msgWrongDataEncoding.Data = "wrong encoding"

	params = newHandlerParameters(nil)

	errAnswer = handleChannel(params, "", msgWrongDataEncoding)
	fmt.Println(errAnswer)
	require.Error(t, errAnswer)
}
