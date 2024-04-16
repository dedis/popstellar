package channel

import (
	"encoding/base64"
	"fmt"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/internal/popserver"
	"popstellar/internal/popserver/repo"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
)

const laoTestDataPath = "../test_data/lao/"
const sender = "HynYISQNI6XqvQNVzA8IzinV8ToiXyKRFsgR2zpP7j8="

func Test_handleChannelLao_LaoState(t *testing.T) {
	serverKeypair := popserver.GenerateKeyPair(t)

	file := filepath.Join(laoTestDataPath, "good_lao_update.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)
	laoID := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="

	UpdateMsg := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, sender),
		WitnessSignatures: []message.WitnessSignature{},
	}

	file = filepath.Join(laoTestDataPath, "good_lao_state.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)
	buf64 = base64.URLEncoding.EncodeToString(buf)

	stateMsg := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, sender),
		WitnessSignatures: []message.WitnessSignature{},
	}

	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("HasMessage", UpdateMsg.MessageID).
		Return(true, nil)
	mockRepository.On("GetLaoWitnesses", laoID).
		Return(map[string]struct{}{}, nil)
	mockRepository.On("StoreMessage", laoID, stateMsg).
		Return(nil)

	params := popserver.NewHandlerParametersWithOwnerAndServer(mockRepository, nil, serverKeypair)
	errAnswer := handleChannelLao(params, laoID, stateMsg)
	require.Nil(t, errAnswer)
}

func Test_handlerChanelLao_RollCallCreate(t *testing.T) {
	laoID := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="

	//Test 1: error when RollCallCreate ID is not the expected hash
	file := filepath.Join(laoTestDataPath, "wrong_rollCall_create_ID.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)
	createMsg := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, sender),
		WitnessSignatures: []message.WitnessSignature{},
	}
	params := popserver.NewHandlerParameters(repo.NewMockRepository(t))
	errAnswer := handleChannelLao(params, laoID, createMsg)

	wrongID := base64.URLEncoding.EncodeToString([]byte("test"))
	require.Contains(t, errAnswer.Error(), fmt.Sprintf("roll call id is %s, should be", wrongID))

	//Test 2: error when RollCallCreate proposed start is after creation

	file = filepath.Join(laoTestDataPath, "wrong_rollCall_create_start.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)
	buf64 = base64.URLEncoding.EncodeToString(buf)
	createMsg = message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, sender),
		WitnessSignatures: []message.WitnessSignature{},
	}
	params = popserver.NewHandlerParameters(repo.NewMockRepository(t))
	errAnswer = handleChannelLao(params, laoID, createMsg)
	require.Contains(t, errAnswer.Error(), "roll call proposed start time should be greater than creation time")

	//Test 3: error when RollCallCreate proposed end is after proposed start

	file = filepath.Join(laoTestDataPath, "wrong_rollCall_create_end.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)
	buf64 = base64.URLEncoding.EncodeToString(buf)
	createMsg = message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, sender),
		WitnessSignatures: []message.WitnessSignature{},
	}
	params = popserver.NewHandlerParameters(repo.NewMockRepository(t))
	errAnswer = handleChannelLao(params, laoID, createMsg)
	require.Contains(t, errAnswer.Error(), "roll call proposed end should be greater than proposed start")

}

func Test_handleChannelLao_RollCallOpen(t *testing.T) {
	laoID := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="

	//Test 1: error when RollCallOpen ID is not the expected hash
	file := filepath.Join(laoTestDataPath, "wrong_rollCall_open_ID.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)
	openMsg := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, sender),
		WitnessSignatures: []message.WitnessSignature{},
	}
	params := popserver.NewHandlerParameters(repo.NewMockRepository(t))
	errAnswer := handleChannelLao(params, laoID, openMsg)

	wrongID := base64.URLEncoding.EncodeToString([]byte("test"))
	require.Contains(t, errAnswer.Error(), fmt.Sprintf("roll call update id is %s, should be", wrongID))

	//Test 2: error when RollCallOpen opens is not the same as previous RollCallCreate

	file = filepath.Join(laoTestDataPath, "wrong_rollCall_open_opens.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)
	buf64 = base64.URLEncoding.EncodeToString(buf)
	openMsg = message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, sender),
		WitnessSignatures: []message.WitnessSignature{},
	}
}
