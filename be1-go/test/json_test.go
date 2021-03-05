package test

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"flag"
	"log"
	"net/url"
	"os"
	"student20_pop"
	"student20_pop/cli/organizer"
	"student20_pop/message"
	"testing"
	"time"

	"github.com/gorilla/websocket"
	"github.com/stretchr/testify/require"
	"github.com/urfave/cli/v2"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

type keypair struct {
	public    kyber.Point
	publicBuf message.PublicKey
	private   kyber.Scalar
}

var organizerPK string
var organizerSecret kyber.Scalar

var answer = &message.Answer{}

var suite = student20_pop.Suite

var port string = "9000"
var client *websocket.Conn

func generateKeyPair() (kyber.Point, message.PublicKey, kyber.Scalar, error) {
	secret := suite.Scalar().Pick(suite.RandomStream())
	point := suite.Point()
	point = point.Mul(secret, point)

	pkbuf, err := point.MarshalBinary()
	if err != nil {
		return nil, nil, nil, xerrors.Errorf("failed to create keypair: %v", err)
	}

	return point, pkbuf, secret, nil
}

func hash(strs ...string) string {
	h := sha256.New()
	for _, str := range strs {
		h.Write([]byte(str))
	}
	return string(h.Sum(nil))
}

func createLao(t *testing.T, name string, creation string, queryID int, organizerPK string) string {
	// Data of the Lao
	log.Printf(creation)
	laoID := hash(organizerPK, creation, name)
	channel := "/root"

	// Creation of the data
	data := createLaoData(laoID, name, creation, organizerPK)

	// Signature of the data by the orgianizer
	signatureByte, err := schnorr.Sign(suite, organizerSecret, []byte(data))
	require.NoError(t, err)
	signature := string(signatureByte)

	// Construction of the query
	messageID := hash(data, signature)
	message := createMessage(data, organizerPK, signature, messageID)
	params := createParams(channel, message)
	query := createQuery("2.0", "publish", params, queryID)

	// Send the query to the hub
	err = client.WriteMessage(websocket.TextMessage, []byte(query))
	require.NoError(t, err)

	// Receive the answer from the hub
	_, answerMessage, err := client.ReadMessage()
	require.NoError(t, err)

	json.Unmarshal(answerMessage, answer)
	// Check the answer
	require.Equal(t, *answer.ID, queryID, "The ID of the query should be the same as the ID of the answer")
	require.Equal(t, *answer.Result.General, 0, "Result value should be 0")

	log.Printf("recv: %s", answerMessage)

	return encode(laoID)
}

func TestMain(m *testing.M) {
	command := flag.NewFlagSet("count", flag.ExitOnError)

	_, pkbuf, private, err := generateKeyPair()
	if err != nil {
		panic(err)
	}
	organizerSecret = private
	organizerPK = base64.StdEncoding.EncodeToString(pkbuf)

	command.String("public-key", organizerPK, "base64 encoded organizer's public key")
	command.String("port", port, "port to listen websocket connections on")

	context := cli.NewContext(nil, command, nil)
	go organizer.Serve(context)

	// client
	u := url.URL{
		Scheme: "ws",
		Host:   "localhost:" + port,
		Path:   "/",
	}
	client, _, err = websocket.DefaultDialer.Dial(u.String(), nil)
	if err != nil {
		panic(err)
	}

	res := m.Run()
	os.Exit(res)
}

func TestOrganizer_CreateLAO(t *testing.T) {
	name := "MyLao"
	creation := message.Timestamp(time.Now().Unix()).String()
	queryID := 0
	createLao(t, name, creation, queryID, organizerPK)
}

func Test_SetupElection(t *testing.T) {
	// create the lao
	nameLao := "laoElection"
	creationLao := message.Timestamp(time.Now().Unix()).String()
	queryID := 1
	laoID := createLao(t, nameLao, creationLao, queryID, organizerPK)

	// Create election
	channel := "/root/" + laoID
	nameElection := "My Election"
	version := "1.0.0"
	creationElection := message.Timestamp(time.Now().Unix())
	startTime := creationElection + 100
	endTime := startTime + 100

	electionID := encode(hash("Election", laoID, creationElection.String(), nameElection))

	questionAsked := "Do you like EPFL?"
	questionID := hash("Question", electionID, questionAsked)
	votingMethod := "plurality"
	ballotOptions := `"yes"`

	// Create the question
	question := createQuestion(questionID, questionAsked, votingMethod, ballotOptions, false)

	// Create the data
	data := createSetUpElectionData(electionID, laoID, nameElection, version, creationElection.String(), startTime.String(), endTime.String(), question)

	// Create the election message
	signatureByte, err := schnorr.Sign(suite, organizerSecret, []byte(data))
	require.NoError(t, err)
	signature := string(signatureByte)

	// Construction of the query
	messageID := hash(data, signature)
	message := createMessage(data, organizerPK, signature, messageID)
	params := createParams(channel, message)
	query := createQuery("2.0", "publish", params, queryID)
	log.Printf("query: " + query)

	// Send the query to the hub
	err = client.WriteMessage(websocket.TextMessage, []byte(query))
	require.NoError(t, err)

	// Receive the answer from the hub
	_, answerMessage, err := client.ReadMessage()
	require.NoError(t, err)
	log.Printf(string(answerMessage))

	json.Unmarshal(answerMessage, answer)
	// Check the answer
	require.Equal(t, *answer.ID, queryID, "The ID of the query should be the same as the ID of the answer")
	require.Equal(t, *answer.Result.General, 0, "Result value should be 0")

	log.Printf("recv: %s", answerMessage)
}
