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
	jsonrpc "popstellar/message"
	"popstellar/message/messagedata"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"testing"
	"time"
)

type handleQueryInput struct {
	name    string
	params  handlerParameters
	message []byte
	socket  *fakeSocket
}

type handleGoodCatchUpInput struct {
	base              handleQueryInput
	messagesToCatchUp []message.Message
}

func Test_handleCatchUp(t *testing.T) {
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

	catchup := method.Catchup{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},

			Method: query.MethodCatchUp,
		},
		ID: 1,
		Params: struct {
			Channel string `json:"channel"`
		}{
			Channel: "/root",
		},
	}

	goodInputs := make([]handleGoodCatchUpInput, 0)
	badInputs := make([]handleQueryInput, 0)

	// Catch up 2 messages

	catchupBuf, err := json.Marshal(&catchup)
	require.NoError(t, err)

	messagesToCatchUp := []message.Message{msg, msg, msg}

	mockRepository := mocks.NewRepository(t)
	mockRepository.On("GetAllMessagesFromChannel", catchup.Params.Channel).Return(messagesToCatchUp, nil)

	s := &fakeSocket{id: "fakesocket"}

	params := newHandlerParametersWithFakeSocket(mockRepository, s)

	goodInputs = append(goodInputs, handleGoodCatchUpInput{
		base: handleQueryInput{
			name:    "catchUp three messages",
			params:  params,
			message: catchupBuf,
			socket:  s,
		},
		messagesToCatchUp: messagesToCatchUp,
	})

	// failed to query db

	catchupBuf, err = json.Marshal(&catchup)
	require.NoError(t, err)

	mockRepository = mocks.NewRepository(t)
	mockRepository.On("GetAllMessagesFromChannel", catchup.Params.Channel).Return(nil, xerrors.Errorf("db is disconnected"))

	params = newHandlerParameters(mockRepository)

	badInputs = append(badInputs, handleQueryInput{
		name:    "failed to query db",
		params:  params,
		message: catchupBuf,
	})

	// run all tests

	for _, i := range goodInputs {
		t.Run(i.base.name, func(t *testing.T) {
			_, errAnswer := handleCatchUp(i.base.params, i.base.message)
			require.Nil(t, errAnswer)
			require.Equal(t, i.messagesToCatchUp, i.base.socket.res)
		})
	}

	for _, i := range badInputs {
		t.Run(i.name, func(t *testing.T) {
			errAnswer := handleMessage(i.params, i.message)
			fmt.Println(errAnswer)
			require.NotNil(t, errAnswer)
		})
	}
}
