package hanswer

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"io"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/mock/generator"
	"popstellar/internal/network/socket"
	"popstellar/internal/state"
	"testing"
	"time"
)

const (
	wrongData      = "wrong data"
	wrongSender    = "wrong sender"
	wrongSignature = "wrong signature"
	wrongMessageID = "wrong messageID"
)

type nullMessageHandler struct{}

func (n *nullMessageHandler) Handle(channelPath string, msg message.Message, fromRumor bool) error {
	if msg.MessageID == wrongMessageID {
		return errors.NewInvalidMessageFieldError("Wrong messageID")
	} else {
		return nil
	}
}

type nullRumorSender struct{}

func (n *nullRumorSender) SendRumor(socket socket.Socket, rumor method.Rumor) {
}

func Test_handleMessagesByChannel(t *testing.T) {

	l := zerolog.New(io.Discard)

	queries := state.NewQueries(&l)

	answerHandlers := Handlers{
		MessageHandler: &nullMessageHandler{},
		RumorSender:    &nullRumorSender{},
	}

	handler := New(queries, answerHandlers)

	type input struct {
		name     string
		messages map[string]map[string]message.Message
		expected map[string]map[string]message.Message
	}

	_, publicBuf, private, _ := generator.GenerateKeyPair(t)
	now := time.Now().Unix()
	name := "LAO X"

	laoID := message.Hash(base64.URLEncoding.EncodeToString(publicBuf), fmt.Sprintf("%d", now), name)

	data := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      name,
		Creation:  now,
		Organizer: base64.URLEncoding.EncodeToString(publicBuf),
		Witnesses: []string{},
	}

	dataBuf, err := json.Marshal(data)
	require.NoError(t, err)
	signature, err := schnorr.Sign(crypto.Suite, private, dataBuf)
	require.NoError(t, err)

	dataBase64 := base64.URLEncoding.EncodeToString(dataBuf)
	signatureBase64 := base64.URLEncoding.EncodeToString(signature)

	msgValid := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(publicBuf),
		Signature:         signatureBase64,
		MessageID:         message.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	msgWithInvalidField := message.Message{
		Data:              wrongData,
		Sender:            wrongSender,
		Signature:         wrongSignature,
		MessageID:         wrongMessageID,
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
	expected["/root"][msgWithInvalidField.MessageID] = msgWithInvalidField
	expected["/root/lao1"] = make(map[string]message.Message)
	expected["/root/lao1"][msgWithInvalidField.MessageID] = msgWithInvalidField

	fmt.Println(messages)
	fmt.Println(expected)

	inputs = append(inputs, input{
		name:     "blacklist without invalid field error",
		messages: messages,
		expected: expected,
	})

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			handler.handleMessagesByChannel(i.messages)

			for k0, v0 := range i.expected {
				for k1 := range v0 {
					fmt.Println(i.messages[k0][k1])
					require.Equal(t, i.expected[k0][k1], i.messages[k0][k1])
				}
			}
		})
	}

}
