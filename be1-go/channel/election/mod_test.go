package election

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"popstellar/channel"
	"popstellar/crypto"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
)

const (
	relativeQueryExamplePath   string = "../../../protocol/examples/query"
	relativeMsgDataExamplePath string = "../../../protocol/examples/messageData"
)

// Tests that the channel works correctly when it receives a subscribe
func Test_Election_Channel_Subscribe(t *testing.T) {

	// create election channel: election with one question
	electChannel, _ := newFakeChannel(t)

	file := filepath.Join(relativeQueryExamplePath, "subscribe", "subscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var sub method.Subscribe
	err = json.Unmarshal(buf, &sub)
	require.NoError(t, err)

	fakeSock := &fakeSocket{id: "socket"}

	err = electChannel.Subscribe(fakeSock, sub)
	require.NoError(t, err)

	// Delete returns false if the socket is not present in the store
	require.True(t, electChannel.sockets.Delete("socket"))
}

// Tests that the channel works correctly when it receives an unsubscribe
func Test_Election_Channel_Unsubscribe(t *testing.T) {

	// create election channel: election with one question
	electChannel, _ := newFakeChannel(t)

	file := filepath.Join(relativeQueryExamplePath, "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var unsub method.Unsubscribe
	err = json.Unmarshal(buf, &unsub)
	require.NoError(t, err)

	fakeSock := &fakeSocket{id: "socket"}
	electChannel.sockets.Upsert(fakeSock)

	err = electChannel.Unsubscribe("socket", unsub)
	require.NoError(t, err)

	// Delete returns false if the socket is not present in the store
	require.False(t, electChannel.sockets.Delete("socket"))
}

// Test that the channel throws an error when it receives an unsubscribe from a
// non-subscribed source
func Test_General_Channel_Wrong_Unsubscribe(t *testing.T) {

	// create election channel: election with one question
	electChannel, _ := newFakeChannel(t)

	file := filepath.Join(relativeQueryExamplePath, "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var unsub method.Unsubscribe
	err = json.Unmarshal(buf, &unsub)
	require.NoError(t, err)

	err = electChannel.Unsubscribe("socket", unsub)
	require.Error(t, err, "client is not subscribed to this channel")
}

// Tests that the channel works correctly when it receives a catchup
func Test_Election_Channel_Catchup(t *testing.T) {

	// create election channel: election with one question
	electChannel, _ := newFakeChannel(t)

	// Create the messages
	numMessages := 5

	messages := make([]message.Message, numMessages)

	for i := 0; i < numMessages; i++ {
		// Create a new message containing only an id
		msg := message.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = msg

		// Store the message in the inbox
		electChannel.inbox.StoreMessage(msg)

		// Wait before storing a new message to be able to have an unique
		// timestamp for each message
		time.Sleep(time.Millisecond)
	}

	// Compute the catchup method
	catchupAnswer := electChannel.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in
	// `catchupAnswer`
	for i := 0; i < numMessages; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID,
			catchupAnswer)
	}
}

// Tests that the channel works when it receives a broadcast message
func Test_Election_Channel_Broadcast(t *testing.T) {

	// create election channel: election with one question
	electChannel, _ := newFakeChannel(t)

	// create a fakeSocket that is listening to the channel
	fakeSock := &fakeSocket{id: "socket"}
	electChannel.sockets.Upsert(fakeSock)

	relativePath := filepath.Join(relativeQueryExamplePath, "broadcast")

	file := filepath.Join(relativePath, "broadcast.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var broadcast method.Broadcast
	err = json.Unmarshal(buf, &broadcast)
	require.NoError(t, err)

	require.Error(t, electChannel.Broadcast(broadcast, nil))
}

// Tests that the channel works correctly when it receives a cast vote and
// end the election correctly
func Test_Publish_Cast_Vote_And_End_Election(t *testing.T) {

	// create election channel: election with one question
	electChannel, pkOrganizer := newFakeChannel(t)

	// create a fakeSocket that is listening to the channel
	fakeSock := &fakeSocket{id: "socket"}
	electChannel.sockets.Upsert(fakeSock)

	// check the created election has only one question
	require.Equal(t, 1, len(electChannel.questions))

	// get cast vote message data
	file := filepath.Join(relativeMsgDataExamplePath, "vote_cast_vote", "vote_cast_vote.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var castVote messagedata.VoteCastVote
	err = json.Unmarshal(buf, &castVote)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	// wrap the cast vote in a message
	m := message.Message{
		Data:              buf64,
		Sender:            pkOrganizer,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	// wrap the message in a publish
	relativePathCreatePub := filepath.Join(relativeQueryExamplePath, "publish")

	fileCreatePub := filepath.Join(relativePathCreatePub, "publish.json")
	bufCreatePub, err := os.ReadFile(fileCreatePub)
	require.NoError(t, err)

	var pub method.Publish

	err = json.Unmarshal(bufCreatePub, &pub)
	require.NoError(t, err)

	pub.Params.Message = m
	pub.Params.Channel = electChannel.channelID

	// publish the cast vote on the election channel
	require.NoError(t, electChannel.Publish(pub, socket.ClientSocket{}))

	// check new vote is in the valid votes map
	for _, question := range electChannel.questions {
		question.validVotesMu.RLock()

		// check the question has one valid vote
		require.Equal(t, 1, len(question.validVotes))

		// check that the valid vote was done by the organizer
		_, ok := question.validVotes[pkOrganizer]
		require.True(t, ok)

		question.validVotesMu.RUnlock()
	}

	// get end election message data
	file = filepath.Join(relativeMsgDataExamplePath, "election_end", "election_end.json")
	buf, err = os.ReadFile(file)
	require.NoError(t, err)

	var endElect messagedata.ElectionEnd
	err = json.Unmarshal(buf, &endElect)
	require.NoError(t, err)

	buf64 = base64.URLEncoding.EncodeToString(buf)

	// wrap the end election in a message
	m = message.Message{
		Data:              buf64,
		Sender:            pkOrganizer,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	// wrap the message in a publish
	pub.Params.Message = m

	// publish the end election on the election channel
	require.NoError(t, electChannel.Publish(pub, socket.ClientSocket{}))

	// check that the listening socket has received the election results
	var broad method.Broadcast
	err = json.Unmarshal(fakeSock.msg, &broad)
	require.NoError(t, err)

	dataBuf, err := base64.URLEncoding.DecodeString(broad.Params.Message.Data)
	require.NoError(t, err)

	var result messagedata.ElectionResult
	err = json.Unmarshal(dataBuf, &result)
	require.NoError(t, err)

	require.Equal(t, "result", result.Action)
	require.Equal(t, "election", result.Object)
}

// Tests that the channel gathers correctly the results
func Test_Cast_Vote_And_Gather_Result(t *testing.T) {

	// create election channel: election with one question
	electChannel, pkOrganizer := newFakeChannel(t)

	// check the created election has only one question
	require.Equal(t, 1, len(electChannel.questions))

	// get cast vote message data
	file := filepath.Join(relativeMsgDataExamplePath, "vote_cast_vote", "vote_cast_vote.json")
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

	// update vote with the valid cast vote data
	// this should add one vote made by the organizer for the question
	err = updateVote("123", pkOrganizer, castVote, electChannel.questions)
	require.NoError(t, err)

	// check new vote is in the valid votes map
	for _, question := range electChannel.questions {
		question.validVotesMu.RLock()

		// check the question has one valid vote
		require.Equal(t, 1, len(question.validVotes))

		// check that the valid vote was done by the organizer
		_, ok := question.validVotes[pkOrganizer]
		require.True(t, ok)

		question.validVotesMu.RUnlock()
	}

	// get election result message data
	file = filepath.Join(relativeMsgDataExamplePath, "election_result.json")
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

	// check that the result contains one question
	require.Equal(t, 1, len(results.Questions))

	// check that the size of the results is correct
	expectedRes := electionResult.Questions[0].Result
	res := results.Questions[0].Result
	require.Equal(t, len(expectedRes), len(res))

	// check the election result contain the same count for the same ballot options as expected
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

	file := filepath.Join(relativeMsgDataExamplePath, "election_setup", "election_setup.json")
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
	channel := NewChannel(channelPath, electionSetup.StartTime, electionSetup.EndTime,
		electionSetup.Questions, attendees, fakeHub, nolog, keypair.public)

	channelElec, ok := channel.(*Channel)
	require.True(t, ok)

	fakeHub.NotifyNewChannel(channelElec.channelID, channel, &fakeSocket{id: "socket"})

	return channelElec, pkOrganizer
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

	pubKeyOwner kyber.Point

	pubKeyServ kyber.Point
	secKeyServ kyber.Scalar

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	workers *semaphore.Weighted

	log zerolog.Logger

	laoFac channel.LaoFactory

	fakeSock fakeSocket
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
		pubKeyOwner:     publicOrg,
		pubKeyServ:      pubServ,
		secKeyServ:      secServ,
		schemaValidator: schemaValidator,
		stop:            make(chan struct{}),
		workers:         semaphore.NewWeighted(10),
		log:             log,
		laoFac:          laoFac,
		fakeSock:        fakeSocket{id: "hubSock"},
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

// GetPubKeyOwner implements channel.HubFunctionalities
func (h *fakeHub) GetPubKeyOwner() kyber.Point {
	return h.pubKeyOwner
}

// GetPubKeyServ implements channel.HubFunctionalities
func (h *fakeHub) GetPubKeyServ() kyber.Point {
	return h.pubKeyServ
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

func (h *fakeHub) GetServerNumber() int {
	return 1
}

func (h *fakeHub) SendAndHandleMessage(msg method.Broadcast) error {
	byteMsg, err := json.Marshal(msg)
	if err != nil {
		return err
	}

	h.fakeSock.msg = byteMsg

	return nil
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
