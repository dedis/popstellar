package election

import (
	"encoding/base64"
	"encoding/json"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
	"io"
	"os"
	"path/filepath"
	"popstellar/channel"
	"popstellar/crypto"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"
	"testing"
)

const relativeExamplePath string = "../../../protocol/examples/messageData"

func Test_CastVoteAndResult(t *testing.T) {

	// create election channel
	electChannel, pkOrganizer := newFakeChannel(t)

	// get cast vote message data
	file := filepath.Join(relativeExamplePath, "vote_cast_vote", "vote_cast_vote.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action := "election", "cast_vote"
	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)
	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var castVote messagedata.VoteCastVote
	err = json.Unmarshal(buf, &castVote)
	require.NoError(t, err)

	// update vote
	err = updateVote("123", pkOrganizer, castVote, &electChannel.questions)
	require.NoError(t, err)

	// check new vote is in map
	for _, question := range electChannel.questions {
		question.validVotesMu.RLock()

		require.Equal(t, len(question.validVotes), 1)
		_, ok := question.validVotes[pkOrganizer]
		require.True(t, ok)

		question.validVotesMu.RUnlock()
	}

	// get election result message data
	file = filepath.Join(relativeExamplePath, "election_result.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)

	object, action = "election", "result"
	obj, act, err = messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)
	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var electionResult messagedata.ElectionResult
	err = json.Unmarshal(buf, &electionResult)
	require.NoError(t, err)

	// gather results of election
	results, err := gatherResults(electChannel.questions, nolog)
	require.NoError(t, err)

	// check the results are as expected
	require.Equal(t, 1, len(results.Questions))
	expectedRes := electionResult.Questions[0].Result
	res := results.Questions[0].Result
	require.Equal(t, len(expectedRes), len(res))
	require.Equal(t, expectedRes[0].Count, res[0].Count)
	require.Equal(t, expectedRes[0].BallotOption, res[0].BallotOption)
	require.Equal(t, expectedRes[1].Count, res[1].Count)
	require.Equal(t, expectedRes[1].BallotOption, res[1].BallotOption)
}

// -----------------------------------------------------------------------------
// Utility functions

func newFakeChannel(t *testing.T) (*Channel, string) {
	// Create the hub
	keypair := generateKeyPair(t)
	pkOrganizer := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	file := filepath.Join(relativeExamplePath, "election_setup", "election_setup.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	// object and action
	object, action := "election", "setup"

	obj, act, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, object, obj)
	require.Equal(t, action, act)

	var electionSetup messagedata.ElectionSetup

	err = json.Unmarshal(buf, &electionSetup)
	require.NoError(t, err)

	attendees := make(map[string]struct{})
	attendees[base64.URLEncoding.EncodeToString(keypair.publicBuf)] = struct{}{}
	channelPath := "/root/" + electionSetup.Lao + "/" + electionSetup.ID
	channel := NewChannel(channelPath, electionSetup.StartTime, electionSetup.EndTime, false,
		electionSetup.Questions, attendees, fakeHub, nolog)

	fakeHub.NotifyNewChannel(channel.channelID, &channel, &fakeSocket{id: "socket"})

	return &channel, pkOrganizer
}

type keypair struct {
	public    kyber.Point
	publicBuf []byte
	private   kyber.Scalar
}

var nolog = zerolog.New(io.Discard)
var suite = crypto.Suite

func generateKeyPair(t *testing.T) keypair {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	pkbuf, err := point.MarshalBinary()
	require.NoError(t, err)

	return keypair{point, pkbuf, secret}
}

type fakeHub struct {
	messageChan chan socket.IncomingMessage

	sync.RWMutex
	channelByID map[string]channel.Channel

	closedSockets chan string

	pubKeyOrg kyber.Point

	pubKeyServ kyber.Point
	secKeyServ kyber.Scalar

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	workers *semaphore.Weighted

	log zerolog.Logger

	laoFac channel.LaoFactory
}

// NewHub returns a Organizer Hub.
func NewfakeHub(publicOrg kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory) (*fakeHub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	pubServ, secServ := generateKeys()

	hub := fakeHub{
		messageChan:     make(chan socket.IncomingMessage),
		channelByID:     make(map[string]channel.Channel),
		closedSockets:   make(chan string),
		pubKeyOrg:       publicOrg,
		pubKeyServ:      pubServ,
		secKeyServ:      secServ,
		schemaValidator: schemaValidator,
		stop:            make(chan struct{}),
		workers:         semaphore.NewWeighted(10),
		log:             log,
		laoFac:          laoFac,
	}

	return &hub, nil
}

func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point().Mul(secret, nil)

	return point, secret
}

func (h *fakeHub) NotifyNewChannel(channeID string, channel channel.Channel, socket socket.Socket) {
	h.Lock()
	h.channelByID[channeID] = channel
	h.Unlock()
}

// GetPubKeyOrg implements channel.HubFunctionalities
func (h *fakeHub) GetPubKeyOrg() kyber.Point {
	return h.pubKeyOrg
}

// GetPubKeyServ implements channel.HubFunctionalities
func (h *fakeHub) GetPubKeyServ() kyber.Point {
	return h.pubKeyOrg
}

// Sign implements channel.HubFunctionalities
func (h *fakeHub) Sign(data []byte) ([]byte, error) {
	signatureBuf, err := schnorr.Sign(crypto.Suite, h.secKeyServ, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}
	return signatureBuf, nil
}

func (h *fakeHub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

// fakeSocket is a fake implementation of a socket
//
// - implements socket.Socket
type fakeSocket struct {
	socket.Socket

	resultID int
	res      []message.Message
	msg      []byte

	err error

	// the socket ID
	id string
}

// Send implements socket.Socket
func (f *fakeSocket) Send(msg []byte) {
	f.msg = msg
}

// SendResult implements socket.Socket
func (f *fakeSocket) SendResult(id int, res []message.Message) {
	f.resultID = id
	f.res = res
}

// SendError implements socket.Socket
func (f *fakeSocket) SendError(id *int, err error) {
	if id != nil {
		f.resultID = *id
	} else {
		f.resultID = -1
	}
	f.err = err
}

func (f *fakeSocket) ID() string {
	return f.id
}
