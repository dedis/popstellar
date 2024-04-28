package channel

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"io"
	"os"
	"popstellar/crypto"
	"popstellar/hub/standard_hub/hub_state"
	"popstellar/internal/popserver"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/internal/popserver/utils"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"popstellar/validation"
	"testing"
	"time"
)

// the public key used in every lao_create json files in the test_data/root folder
const ownerPubBuf64 = "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="

var subs *types.Subscribers
var queries hub_state.Queries
var peers hub_state.Peers

var ownerPublicKey kyber.Point
var serverPublicKey kyber.Point
var serverSecretKey kyber.Scalar

func TestMain(m *testing.M) {

	subs = types.NewSubscribers()
	queries = hub_state.NewQueries(zerolog.New(io.Discard))
	peers = hub_state.NewPeers()

	state.InitState(subs, &peers, &queries)

	log := zerolog.New(io.Discard)
	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		os.Exit(1)
	}

	utils.InitUtils(&log, schemaValidator)
	organizerBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		os.Exit(1)
	}

	ownerPublicKey = crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(organizerBuf)
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		os.Exit(1)
	}

	serverSecretKey = crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey = crypto.Suite.Point().Mul(serverSecretKey, nil)

	config.InitConfig(ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	exitVal := m.Run()

	os.Exit(exitVal)
}

type handleChannelInput struct {
	name      string
	channelID string
	message   message.Message
}

func Test_handleChannel(t *testing.T) {
	mockRepository, err := database.SetDatabase(t)
	require.NoError(t, err)

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

	msg := message.Message{
		Data:              dataBase64,
		Sender:            base64.URLEncoding.EncodeToString(keypair.PublicBuf),
		Signature:         signatureBase64,
		MessageID:         messagedata.Hash(dataBase64, signatureBase64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	inputs := make([]handleChannelInput, 0)

	// unknown channelType

	wrongChannelID := "wrongChannelID"

	mockRepository.On("HasMessage", msg.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", wrongChannelID).Return("", nil)

	inputs = append(inputs, handleChannelInput{
		name:      "unknown channelType",
		channelID: wrongChannelID,
		message:   msg,
	})

	// error while querying the channelType

	problemDBChannelID := "problemDBChannelID"

	mockRepository.On("HasMessage", msg.MessageID).Return(false, nil)
	mockRepository.On("GetChannelType", problemDBChannelID).Return("", xerrors.Errorf("DB disconnected"))

	inputs = append(inputs, handleChannelInput{
		name:      "failed to query channelType",
		channelID: problemDBChannelID,
		message:   msg,
	})

	// message already received

	mockRepository.On("HasMessage", msg.MessageID).Return(true, nil)

	inputs = append(inputs, handleChannelInput{
		name:      "message already received",
		channelID: wrongChannelID,
		message:   msg,
	})

	// error while querying if the message already exists

	mockRepository.On("HasMessage", msg.MessageID).Return(false, xerrors.Errorf("DB disconnected"))

	inputs = append(inputs, handleChannelInput{
		name:      "failed to query message",
		channelID: wrongChannelID,
		message:   msg,
	})

	// wrong messageID

	msgWrongID := msg
	msgWrongID.MessageID = messagedata.Hash("wrong messageID")

	inputs = append(inputs, handleChannelInput{
		name:      "wrong messageID",
		channelID: "",
		message:   msgWrongID,
	})

	// failed signature check because wrong sender

	wrongKeypair := popserver.GenerateKeyPair(t)
	msgWrongSender := msg
	msgWrongSender.Sender = base64.URLEncoding.EncodeToString(wrongKeypair.PublicBuf)

	inputs = append(inputs, handleChannelInput{
		name:      "failed signature check wrong sender",
		channelID: "",
		message:   msgWrongSender,
	})

	// failed signature check because wrong data

	msgWrongData := msg
	msgWrongData.Data = base64.URLEncoding.EncodeToString([]byte("wrong data"))

	inputs = append(inputs, handleChannelInput{
		name:      "failed signature check wrong data",
		channelID: "",
		message:   msgWrongData,
	})

	// failed signature check because wrong signature

	wrongKeypair = popserver.GenerateKeyPair(t)
	wrongSignature, err := schnorr.Sign(crypto.Suite, wrongKeypair.Private, dataBuf)
	require.NoError(t, err)

	msgWrongSign := msg
	msgWrongSign.Signature = base64.URLEncoding.EncodeToString(wrongSignature)

	inputs = append(inputs, handleChannelInput{
		name:      "failed signature check wrong signature",
		channelID: "",
		message:   msgWrongSign,
	})

	// wrong signature encoding

	msgWrongSignEncoding := msg
	msgWrongSignEncoding.Signature = "wrong encoding"

	inputs = append(inputs, handleChannelInput{
		name:      "wrong signature encoding",
		channelID: "",
		message:   msgWrongSignEncoding,
	})

	// wrong sender encoding

	msgWrongSenderEncoding := msg
	msgWrongSenderEncoding.Sender = "wrong encoding"

	inputs = append(inputs, handleChannelInput{
		name:      "wrong sender encoding",
		channelID: "",
		message:   msgWrongSenderEncoding,
	})

	// wrong data encoding

	msgWrongDataEncoding := msg
	msgWrongDataEncoding.Data = "wrong encoding"

	inputs = append(inputs, handleChannelInput{
		name:      "wrong data encoding",
		channelID: "",
		message:   msgWrongDataEncoding,
	})

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			fakeSocket := popserver.FakeSocket{Id: "fakesocket"}
			errAnswer := HandleChannel(&fakeSocket, i.channelID, i.message)
			require.Error(t, errAnswer)
		})
	}

}
