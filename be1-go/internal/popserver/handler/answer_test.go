package handler

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	"popstellar/internal/popserver/database"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
	"time"
)

func Test_handleMessagesByChannel(t *testing.T) {
	mockRepository, err := database.SetDatabase(t)
	require.NoError(t, err)

	type input struct {
		name     string
		messages map[string]map[string]message.Message
		expected map[string]map[string]message.Message
	}

	keypair := GenerateKeyPair(t)
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

	mockRepository.On("HasMessage", msgValid.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", "/root").Return("", nil)
	mockRepository.On("GetChannelType", "/root/lao1").Return("", nil)

	inputs = append(inputs, input{
		name:     "blacklist without invalid field error",
		messages: messages,
		expected: expected,
	})

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			handleMessagesByChannel(i.messages)

			for k0, v0 := range i.expected {
				for k1 := range v0 {
					require.Equal(t, i.expected[k0][k1], i.messages[k0][k1])
				}
			}
		})
	}

}
