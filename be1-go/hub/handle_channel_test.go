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

type handleChannelInput struct {
	name      string
	params    handlerParameters
	channelID string
	message   message.Message
}

func Test_handleChannel(t *testing.T) {
	socket := &fakeSocket{id: "fakeID"}
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

	inputs := make([]handleChannelInput, 0)

	// unknown channelType

	wrongChannelID := "wrongChannelID"

	mockRepository := mocks.NewRepository(t)
	params := newHandlerParameters(mockRepository, socket)

	mockRepository.On("HasMessage", msg.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", wrongChannelID).Return("", nil)

	inputs = append(inputs, handleChannelInput{
		name:      "unknown channelType",
		params:    params,
		channelID: wrongChannelID,
		message:   msg,
	})

	// error while querying the channelType

	problemDBChannelID := "problemDBChannelID"

	mockRepository = mocks.NewRepository(t)
	params = newHandlerParameters(mockRepository, socket)

	mockRepository.On("HasMessage", msg.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", problemDBChannelID).Return("", xerrors.Errorf("db disconnected"))

	inputs = append(inputs, handleChannelInput{
		name:      "failed to query channelType",
		params:    params,
		channelID: problemDBChannelID,
		message:   msg,
	})

	// message already received

	mockRepository = mocks.NewRepository(t)
	params = newHandlerParameters(mockRepository, socket)

	mockRepository.On("HasMessage", msg.MessageID).Return(true, nil)

	inputs = append(inputs, handleChannelInput{
		name:      "message already received",
		params:    params,
		channelID: wrongChannelID,
		message:   msg,
	})

	// error while querying if the message already exists

	mockRepository = mocks.NewRepository(t)
	params = newHandlerParameters(mockRepository, socket)

	mockRepository.On("HasMessage", msg.MessageID).Return(false, xerrors.Errorf("db disconnected"))

	inputs = append(inputs, handleChannelInput{
		name:      "failed to query message",
		params:    params,
		channelID: wrongChannelID,
		message:   msg,
	})

	// wrong messageID

	msgWrongID := msg
	msgWrongID.MessageID = messagedata.Hash("wrong messageID")

	params = newHandlerParameters(nil, socket)

	inputs = append(inputs, handleChannelInput{
		name:      "wrong messageID",
		params:    params,
		channelID: "",
		message:   msgWrongID,
	})

	// failed signature check because wrong sender

	wrongKeypair := generateKeyPair(t)
	msgWrongSender := msg
	msgWrongSender.Sender = base64.URLEncoding.EncodeToString(wrongKeypair.publicBuf)

	params = newHandlerParameters(nil, socket)

	inputs = append(inputs, handleChannelInput{
		name:      "failed signature check wrong sender",
		params:    params,
		channelID: "",
		message:   msgWrongSender,
	})

	// failed signature check because wrong data

	msgWrongData := msg
	msgWrongData.Data = base64.URLEncoding.EncodeToString([]byte("wrong data"))

	params = newHandlerParameters(nil, socket)

	inputs = append(inputs, handleChannelInput{
		name:      "failed signature check wrong data",
		params:    params,
		channelID: "",
		message:   msgWrongData,
	})

	// failed signature check because wrong signature

	wrongKeypair = generateKeyPair(t)
	wrongSignature, err := schnorr.Sign(crypto.Suite, wrongKeypair.private, dataBuf)
	require.NoError(t, err)

	msgWrongSign := msg
	msgWrongSign.Signature = base64.URLEncoding.EncodeToString(wrongSignature)

	params = newHandlerParameters(nil, socket)

	inputs = append(inputs, handleChannelInput{
		name:      "failed signature check wrong signature",
		params:    params,
		channelID: "",
		message:   msgWrongSign,
	})

	// wrong signature encoding

	msgWrongSignEncoding := msg
	msgWrongSignEncoding.Signature = "wrong encoding"

	params = newHandlerParameters(nil, socket)

	inputs = append(inputs, handleChannelInput{
		name:      "wrong signature encoding",
		params:    params,
		channelID: "",
		message:   msgWrongSignEncoding,
	})

	// wrong sender encoding

	msgWrongSenderEncoding := msg
	msgWrongSenderEncoding.Sender = "wrong encoding"

	params = newHandlerParameters(nil, socket)

	inputs = append(inputs, handleChannelInput{
		name:      "wrong sender encoding",
		params:    params,
		channelID: "",
		message:   msgWrongSenderEncoding,
	})

	// wrong data encoding

	msgWrongDataEncoding := msg
	msgWrongDataEncoding.Data = "wrong encoding"

	params = newHandlerParameters(nil, socket)

	inputs = append(inputs, handleChannelInput{
		name:      "wrong data encoding",
		params:    params,
		channelID: "",
		message:   msgWrongDataEncoding,
	})

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			errAnswer := handleChannel(i.params, i.channelID, i.message)
			require.Error(t, errAnswer)
		})
	}

}
