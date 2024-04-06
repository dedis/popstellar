package message

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/internal/popserver"
	"popstellar/internal/popserver/repo"
	"popstellar/internal/popserver/types"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
	"time"
)

func Test_handleGetMessagesByIDAnswer(t *testing.T) {
	type input struct {
		name        string
		params      types.HandlerParameters
		message     answer.Answer
		isErrorTest bool
	}

	inputs := make([]input, 0)

	// failed to query DB

	id := 1

	result := make(map[string][]json.RawMessage)
	msgsByChannel := make(map[string]map[string]message.Message)

	msg := answer.Answer{
		ID: &id,
		Result: &answer.Result{
			MessagesByChannel: result,
		},
	}

	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("StorePendingMessages", msgsByChannel).Return(xerrors.Errorf("DB disconnected"))

	params := popserver.NewHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:        "failed to query DB",
		params:      params,
		message:     msg,
		isErrorTest: true,
	})

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			errAnswer := handleGetMessagesByIDAnswer(i.params, i.message)
			if i.isErrorTest {
				require.Error(t, errAnswer)
			}
		})
	}

}

func Test_handleMessagesByChannel(t *testing.T) {
	type input struct {
		name     string
		params   types.HandlerParameters
		messages map[string]map[string]message.Message
		expected map[string]map[string]message.Message
	}

	keypair := popserver.GenerateKeyPair(t)
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

	msgValid := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.PublicBuf),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	msgWithInvalidField := message.Message{
		Data:              "wrong data",
		Sender:            "wrong sender",
		Signature:         "wrong signature",
		MessageID:         "wrong messageID",
		WitnessSignatures: []message.WitnessSignature{},
	}

	inputs := make([]input, 0)

	// blacklist without invalid field error

	messages := make(map[string]map[string]message.Message)
	messages["/root"] = make(map[string]message.Message)
	messages["/root"][msgValid.MessageID] = msgValid
	messages["/root"][msgWithInvalidField.MessageID] = msgWithInvalidField
	messages["/root/lao1"] = make(map[string]message.Message)
	messages["/root/lao1"][msgValid.MessageID] = msgValid
	messages["/root/lao1"][msgWithInvalidField.MessageID] = msgWithInvalidField

	expected := make(map[string]map[string]message.Message)
	expected["/root"] = make(map[string]message.Message)
	expected["/root"][msgValid.MessageID] = msgValid
	expected["/root/lao1"] = make(map[string]message.Message)
	expected["/root/lao1"][msgValid.MessageID] = msgValid

	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("HasMessage", msgValid.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", "/root").Return("", nil)
	mockRepository.On("GetChannelType", "/root/lao1").Return("", nil)

	params := popserver.NewHandlerParameters(mockRepository)

	inputs = append(inputs, input{
		name:     "blacklist without invalid field error",
		params:   params,
		messages: messages,
		expected: expected,
	})

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			handleMessagesByChannel(i.params, i.messages)

			for k0, v0 := range i.expected {
				for k1 := range v0 {
					require.Equal(t, i.expected[k0][k1], i.messages[k0][k1])
				}
			}
		})
	}

}
