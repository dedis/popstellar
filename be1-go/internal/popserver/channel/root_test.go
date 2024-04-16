package channel

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"os"
	"path/filepath"
	"popstellar/crypto"
	"popstellar/internal/popserver"
	"popstellar/internal/popserver/repo"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
)

const (
	rootPath = "../test_data/root/"
	//the public key used in every lao_create json files in the test_data/root folder
	organizer = "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="
	// A public key different from the organizer public key
	wrongSender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
)

type input struct {
	name     string
	params   types.HandlerParameters
	msg      message.Message
	channel  string
	isError  bool
	contains string
}

func Test_handleChannelRoot(t *testing.T) {
	var args []input

	organizerKey := crypto.Suite.Point()
	organizerBuf, err := base64.URLEncoding.DecodeString(organizer)
	require.NoError(t, err)
	err = organizerKey.UnmarshalBinary(organizerBuf)
	require.NoError(t, err)
	wrongOwnerKeypair := popserver.GenerateKeyPair(t)

	// Test 1: error when different sender and owner keys
	args = append(args, newInputError(t,
		"wrong_lao_create_different_sender_owner.json",
		organizer,                // sender public key in base64 format
		wrongOwnerKeypair.Public, // owner public key
		"Test 1",
		"sender's public key does not match the owner public key"))

	// Test 2: error when different organizer and sender keys
	args = append(args, newInputError(t,
		"wrong_lao_create_different_sender_organizer.json",
		wrongSender,  // sender public key in base64 format
		organizerKey, // owner public key
		"Test 2",
		"sender's public key does not match the organizer public key"))

	// Test 3: error when the lao name is not the same as the one used for the laoID
	args = append(args, newInputError(t,
		"wrong_lao_create_wrong_name.json",
		organizer,    // sender public key in base64 format
		organizerKey, // owner public key
		"Test 3",
		"failed to verify message data: invalid message field: lao id"))

	// Test 4: error when message data is not lao_create
	args = append(args, newInputError(t,
		"",
		organizer,    // sender public key in base64 format
		organizerKey, // owner public key
		"Test 4",
		"failed to validate message against json schema"))

	// Test 5: success with owner public key not nil
	args = append(args, newInputSuccess(t,
		"good_lao_create.json",
		organizer,    // sender public key in base64 format
		organizerKey, // owner public key
		"Test 5",
		""))

	// Test 6: success with owner public key nil
	args = append(args, newInputSuccess(t,
		"good_lao_create.json",
		organizer, // sender public key in base64 format
		nil,       // owner public key
		"Test 6",
		""))

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelRoot(arg.params, arg.channel, arg.msg)
			if arg.isError {
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
				assert.Equal(t, 5, len(arg.params.Subs))
			}
		})
	}
}

func newInputError(t *testing.T, fileName, sender string, ownerKey kyber.Point, testName, contains string) input {

	var buf []byte
	var err error
	if fileName == "" {
		type N0thing struct {
			Object string `json:"object"`
			Action string `json:"action"`
			Not    string `json:"not"`
		}

		data := N0thing{
			Object: "lao",
			Action: "nothing",
			Not:    "no",
		}

		buf, err = json.Marshal(data)
	} else {
		file := filepath.Join(rootPath, fileName)
		buf, err = os.ReadFile(file)
	}
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)

	var laoCreate messagedata.LaoCreate
	err = json.Unmarshal(buf, &laoCreate)
	require.NoError(t, err)

	msg := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "signature",
		MessageID:         "ID",
		WitnessSignatures: []message.WitnessSignature{},
	}

	var mockRepository *repo.MockRepository
	if fileName != "" {
		mockRepository = repo.NewMockRepository(t)
		mockRepository.On("HasChannel", rootPrefix+laoCreate.ID).Return(false, nil)
	} else {
		mockRepository = nil
	}
	params := popserver.NewHandlerParametersWithOwnerAndServer(mockRepository, ownerKey, popserver.GenerateKeyPair(t))

	return input{name: testName,
		params:   params,
		msg:      msg,
		channel:  rootChannel,
		isError:  true,
		contains: contains}
}

func newInputSuccess(t *testing.T, fileName, sender string, ownerKey kyber.Point, testName, contains string) input {
	file := filepath.Join(rootPath, fileName)
	buf, err := os.ReadFile(file)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)

	var laoCreate messagedata.LaoCreate
	err = json.Unmarshal(buf, &laoCreate)
	require.NoError(t, err)
	laoPath := rootPrefix + laoCreate.ID
	serverKeypair := popserver.GenerateKeyPair(t)

	msg := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "signature",
		MessageID:         "ID",
		WitnessSignatures: []message.WitnessSignature{},
	}

	channels := []string{laoPath + social + chirps,
		laoPath + social + reactions,
		laoPath + consensus,
		laoPath + coin,
		laoPath + auth}

	organizerBuf, err := base64.URLEncoding.DecodeString(laoCreate.Organizer)
	require.NoError(t, err)
	mockRepository := repo.NewMockRepository(t)
	mockRepository.On("HasChannel", laoPath).Return(false, nil)
	mockRepository.On("StoreChannelsAndMessageWithLaoGreet",
		channels,
		rootChannel, laoPath, msg.MessageID,
		organizerBuf,
		msg, mock.AnythingOfType("message.Message")).Return(nil)
	params := popserver.NewHandlerParametersWithOwnerAndServer(mockRepository, ownerKey, serverKeypair)

	return input{name: testName,
		params:   params,
		msg:      msg,
		channel:  rootChannel,
		isError:  false,
		contains: contains}
}
