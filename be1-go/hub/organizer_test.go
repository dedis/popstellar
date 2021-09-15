package hub

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"student20_pop/crypto"
	"student20_pop/message/messagedata"
	"student20_pop/message/query"
	"student20_pop/message/query/method"
	"student20_pop/message/query/method/message"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

type keypair struct {
	public    kyber.Point
	publicBuf []byte
	private   kyber.Scalar
}

var organizerKeyPair keypair

var suite = crypto.Suite

var oHub *organizerHub

func generateKeyPair() (keypair, error) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Pick(suite.RandomStream())
	point = point.Mul(secret, point)

	pkbuf, err := point.MarshalBinary()
	if err != nil {
		return keypair{}, xerrors.Errorf("failed to create keypair: %v", err)
	}
	return keypair{point, pkbuf, secret}, nil
}

func createLao(o *organizerHub, oKeypair keypair, name string) (string, *laoChannel, error) {
	now := time.Now().Unix()

	// LaoID is Hash(organizer||create||name) encoded in base64URL
	h := sha256.New()
	h.Write(oKeypair.publicBuf)
	h.Write([]byte(fmt.Sprintf("%d", now)))
	h.Write([]byte(name))

	laoID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	data := messagedata.LaoCreate{
		Object:    "lao",
		Action:    "create",
		ID:        laoID,
		Name:      name,
		Creation:  123,
		Organizer: "XXX",
		Witnesses: nil,
	}

	dataBuf, err := json.Marshal(data)
	if err != nil {
		return "", nil, err
	}

	signature, err := schnorr.Sign(suite, oKeypair.private, dataBuf)
	if err != nil {
		return "", nil, err
	}

	msg := message.Message{
		Data:              string(dataBuf),
		Sender:            string(oKeypair.publicBuf),
		Signature:         string(signature),
		WitnessSignatures: nil,
	}

	publish := method.Publish{
		Base: query.Base{
			Method: "publish",
		},

		ID: 1,

		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			Channel: "/root/" + laoID,
			Message: msg,
		},
	}
	o.createLao(publish, data)

	channel, ok := oHub.channelByID[rootPrefix+laoID]
	if !ok {
		return "", nil, xerrors.Errorf("Could not extract the channel of the lao")
	}
	laoChannel := channel.(*laoChannel)

	return laoID, laoChannel, nil
}

func createMessage(t *testing.T, data interface{}, publicKey []byte) message.Message {
	jsonbuf, err := json.Marshal(data)
	require.NoError(t, err)

	signature := "XXX"

	// messageID is H(data||signature) encoded in base64URL
	h := sha256.New()
	h.Write(jsonbuf)
	h.Write([]byte(signature))

	messageID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	encodedSender := base64.URLEncoding.EncodeToString(publicKey)

	return message.Message{
		MessageID:         messageID,
		Data:              base64.URLEncoding.EncodeToString(jsonbuf),
		Sender:            encodedSender,
		Signature:         signature,
		WitnessSignatures: []message.WitnessSignature{},
	}
}

func TestMain(m *testing.M) {
	log := zerolog.New(io.Discard)

	organizerKeyPair, _ = generateKeyPair()

	baseHub, err := NewBaseHub(organizerKeyPair.public, log)
	if err != nil {
		panic(err)
	}

	oHub = &organizerHub{
		baseHub: baseHub,
	}

	res := m.Run()
	os.Exit(res)
}

func TestOrganizer_CreateLAO(t *testing.T) {
	_, _, err := createLao(oHub, organizerKeyPair, "my lao")
	require.NoError(t, err)
}

// test Created → Opened → Closed → Reopened → Closed
func TestOrganizer_RollCall(t *testing.T) {
	laoID, laoChannel, err := createLao(oHub, organizerKeyPair, "lao roll call")
	require.NoError(t, err)

	now := time.Now().Unix()
	name := "XXX"

	// ID is H('R'||lao_id||creation||name) encoded in base64URL
	h := sha256.New()
	h.Write([]byte{'R'})
	h.Write([]byte(laoID))
	h.Write([]byte(fmt.Sprintf("%d", now)))
	h.Write([]byte(name))

	id := base64.URLEncoding.EncodeToString(h.Sum(nil))

	// Create

	dataCreate := messagedata.RollCallCreate{
		Object:        "roll_call",
		Action:        "create",
		ID:            id,
		Name:          name,
		Creation:      now,
		ProposedStart: now,
		ProposedEnd:   now,
		Location:      "XXX",
		Description:   "XXX",
	}

	msg := createMessage(t, &dataCreate, organizerKeyPair.publicBuf)
	err = laoChannel.processRollCallObject("create", msg)
	require.NoError(t, err)
	require.Equal(t, laoChannel.rollCall.state, Created)
	require.Equal(t, laoChannel.rollCall.id, string(dataCreate.ID))

	now = time.Now().Unix()

	// updateID is H('R'||lao_id||opens||opened_at) encoded as base64URL
	h = sha256.New()
	h.Write([]byte{'R'})
	h.Write([]byte(laoID))
	h.Write([]byte(dataCreate.ID))
	h.Write([]byte(fmt.Sprintf("%d", now)))

	updateID := base64.URLEncoding.EncodeToString(h.Sum(nil))

	dataOpen := messagedata.RollCallOpen{
		Object:   "roll_call",
		Action:   "open",
		UpdateID: updateID,
		Opens:    dataCreate.ID,
		OpenedAt: 123,
	}

	msg = createMessage(t, &dataOpen, organizerKeyPair.publicBuf)
	err = laoChannel.processRollCallObject("open", msg)
	require.NoError(t, err)
	require.Equal(t, laoChannel.rollCall.state, Open)
	require.Equal(t, laoChannel.rollCall.id, string(dataOpen.UpdateID))

	// Generate public keys
	var attendees []string

	for i := 0; i < 10; i++ {
		keypair, err := generateKeyPair()
		require.NoError(t, err)
		attendees = append(attendees, base64.URLEncoding.EncodeToString(keypair.publicBuf))
	}

	// Close

	now = time.Now().Unix()

	// updateID is H('R'||lao_id||closes||closed_at) encoded as base64URL
	h = sha256.New()
	h.Write([]byte{'R'})
	h.Write([]byte(laoID))
	h.Write([]byte(dataOpen.UpdateID))
	h.Write([]byte(fmt.Sprintf("%d", now)))

	updateID = base64.URLEncoding.EncodeToString(h.Sum(nil))

	dataClose1 := messagedata.RollCallClose{
		Object:    "roll_call",
		Action:    "close",
		UpdateID:  updateID,
		Closes:    dataOpen.UpdateID,
		ClosedAt:  now,
		Attendees: attendees,
	}

	msg = createMessage(t, &dataClose1, organizerKeyPair.publicBuf)
	err = laoChannel.processRollCallObject("close", msg)
	require.NoError(t, err)
	require.Equal(t, laoChannel.rollCall.state, Closed)
	require.Equal(t, laoChannel.rollCall.id, string(dataClose1.UpdateID))

	for _, attendee := range attendees[:8] {
		ok := laoChannel.attendees.IsPresent(attendee)
		require.True(t, ok)
	}

	// Reopen

	now = time.Now().Unix()

	// updateID is H('R'||lao_id||opens||opened_at) encoded as base64URL
	h = sha256.New()
	h.Write([]byte{'R'})
	h.Write([]byte(laoID))
	h.Write([]byte(dataClose1.UpdateID))
	h.Write([]byte(fmt.Sprintf("%d", now)))

	updateID = base64.URLEncoding.EncodeToString(h.Sum(nil))

	dataReopen := messagedata.RollCallReOpen{
		Object:   "roll_call",
		Action:   "reopen",
		UpdateID: updateID,
		Opens:    dataClose1.UpdateID,
		OpenedAt: now,
	}

	msg = createMessage(t, &dataReopen, organizerKeyPair.publicBuf)
	err = laoChannel.processRollCallObject("reopen", msg)
	require.NoError(t, err)
	require.Equal(t, laoChannel.rollCall.state, Open)
	require.Equal(t, laoChannel.rollCall.id, string(dataReopen.UpdateID))

	// Close

	now = time.Now().Unix()

	// updateID is H('R'||lao_id||closes||closed_at) encoded as base64URL
	h = sha256.New()
	h.Write([]byte{'R'})
	h.Write([]byte(laoID))
	h.Write([]byte(dataOpen.UpdateID))
	h.Write([]byte(fmt.Sprintf("%d", now)))

	updateID = base64.URLEncoding.EncodeToString(h.Sum(nil))

	dataClose2 := messagedata.RollCallClose{
		Object:    "roll_call",
		Action:    "close",
		UpdateID:  updateID,
		Closes:    dataReopen.UpdateID,
		ClosedAt:  now,
		Attendees: []string{},
	}

	msg = createMessage(t, &dataClose2, organizerKeyPair.publicBuf)
	err = laoChannel.processRollCallObject("close", msg)
	require.NoError(t, err)
	require.Equal(t, laoChannel.rollCall.state, Closed)
	require.Equal(t, laoChannel.rollCall.id, string(dataClose2.UpdateID))

	for _, attendee := range attendees {
		ok := laoChannel.attendees.IsPresent(attendee)
		require.True(t, ok)
	}
}

func TestOrganizer_RollCallProposedStartEnd(t *testing.T) {
	_, laoChannel, err := createLao(oHub, organizerKeyPair, "lao roll call proposed start")
	require.NoError(t, err)

	dataCreate := messagedata.RollCallCreate{
		Object:        "roll_call",
		Action:        "create",
		ID:            "XXX",
		Name:          "XXX",
		Creation:      1,
		ProposedStart: 2,
		ProposedEnd:   1,
		Location:      "XXX",
		Description:   "XXX",
	}

	msg := createMessage(t, &dataCreate, organizerKeyPair.publicBuf)
	err = laoChannel.processRollCallObject("create", msg)
	require.EqualError(t, err, "failed to process roll call create: The field `proposed_start` is greater than the field `proposed_end`: 2 > 1")
}
