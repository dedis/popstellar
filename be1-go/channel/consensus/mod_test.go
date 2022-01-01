package consensus

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

const protocolRelativePath string = "../../../protocol"

// Tests that the channel works correctly when it receives a subscribe from a
// client
func Test_Consensus_Channel_Subscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	channel := NewChannel("channel0", fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath, "examples", "query", "subscribe", "subscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Subscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "socket", sockType: socket.ClientSocketType}

	err = channel.Subscribe(socket, message)
	require.NoError(t, err)

	require.True(t, consensusChannel.sockets.Delete("socket"))
}

// Tests that the channel works correctly when it receives an unsubscribe from a
// client
func Test_Consensus_Channel_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	channel := NewChannel("channel0", fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath, "examples", "query", "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	socket := &fakeSocket{id: "socket", sockType: socket.ClientSocketType}
	consensusChannel.sockets.Upsert(socket)

	err = channel.Unsubscribe("socket", message)
	require.NoError(t, err)

	require.False(t, consensusChannel.sockets.Delete("socket"))
}

// Test that the channel throws an error when it receives an unsubscribe from a
// non-subscribed source
func Test_Consensus_Channel_Wrong_Unsubscribe(t *testing.T) {
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	channel := NewChannel("channel0", fakeHub, nolog)

	file := filepath.Join(protocolRelativePath, "examples", "query", "unsubscribe", "unsubscribe.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Unsubscribe
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	err = channel.Unsubscribe("socket", message)
	require.Error(t, err, "client is not subscribed to this channel")
}

// Tests that the channel works correctly when it receives a catchup
func Test_Consensus_Channel_Catchup(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the messages
	numMessages := 5
	messages := make([]message.Message, numMessages)

	// Create the channel
	channel := NewChannel("channel0", fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	for i := 0; i < numMessages; i++ {
		// Create a new message containing only an id
		message := message.Message{MessageID: fmt.Sprintf("%d", i)}
		messages[i] = message

		// Store the message in the inbox
		consensusChannel.inbox.StoreMessage(message)

		// Wait before storing a new message to be able to have an unique
		// timestamp for each message
		time.Sleep(time.Millisecond)
	}

	// Compute the catchup method
	catchupAnswer := channel.Catchup(method.Catchup{ID: 0})

	// Check that the order of the messages is the same in `messages` and in
	// `catchupAnswer`
	for i := 0; i < numMessages; i++ {
		require.Equal(t, messages[i].MessageID, catchupAnswer[i].MessageID,
			catchupAnswer)
	}
}

// Tests that the channel throws an error when it receives a broadcast message
func Test_Consensus_Channel_Broadcast(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	channel := NewChannel("channel0", fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	file := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	var message method.Broadcast
	err = json.Unmarshal(buf, &message)
	require.NoError(t, err)

	err = consensusChannel.Broadcast(message)
	require.Error(t, err, "a consensus channel shouldn't need to broadcast a message")
}

// Tests that the channel works correctly when it receives an elect message
func Test_Consensus_Publish_Elect(t *testing.T) {
	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	consensusChanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo="
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a socket subscribed to the channel
	socket := &fakeSocket{id: "socket"}
	consensusChannel.sockets.Upsert(socket)

	// Create a consensus elect message
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	message := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = message

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = message
	messageBroadcast.Params.Channel = consensusChanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	err = channel.Publish(messagePublish, nil)
	require.NoError(t, err)
	require.Equal(t, byteBroad, socket.msg)
}

// Tests that the channel works correctly when it receives an elect_accept
// message
func Test_Consensus_Publish_Elect_Accept(t *testing.T) {
	consensusInstance := "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs="
	messageID := "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q="

	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/consensus"
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a client socket subscribed to the channel
	cliSocket := &fakeSocket{id: "cliSocket", sockType: socket.ClientSocketType}
	consensusChannel.sockets.Upsert(cliSocket)

	// Create a consensus elect message into the inbox of the channel
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	bufElect, err := os.ReadFile(file)
	require.NoError(t, err)

	bufbElect64 := base64.URLEncoding.EncodeToString(bufElect)

	electMessage := message.Message{
		Data:              bufbElect64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messageID,
		WitnessSignatures: []message.WitnessSignature{},
	}

	consensusChannel.inbox.StoreMessage(electMessage)

	// Add the state of the message to the channel
	consensusChannel.createMessageInstance(messageID, keypair.public)

	// Add a new consensus instance to the channel
	consensusChannel.createConsensusInstance(consensusInstance)

	// Create a consensus elect_accept message
	file = filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect_accept", "elect_accept.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	msg := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = msg

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = msg
	messageBroadcast.Params.Channel = chanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	require.NoError(t, channel.Publish(messagePublish, nil))
	require.Equal(t, byteBroad, cliSocket.msg)

	// Unmarshal the prepare message sent to other servers to verify its values
	var sentPublish method.Publish
	err = json.Unmarshal(fakeHub.fakeSock.msg, &sentPublish)
	require.NoError(t, err)

	sentMsg := sentPublish.Params.Message

	// Unmarshal the prepare message data to check its values
	jsonData, err := base64.RawStdEncoding.DecodeString(sentMsg.Data)
	require.NoError(t, err)
	var prepare messagedata.ConsensusPrepare
	err = json.Unmarshal(jsonData, &prepare)
	require.NoError(t, err)

	require.Equal(t, "consensus", prepare.Object)
	require.Equal(t, "prepare", prepare.Action)
	require.Equal(t, consensusInstance, prepare.InstanceID)
	require.Equal(t, messageID, prepare.MessageID)
	require.GreaterOrEqual(t, prepare.CreatedAt, int64(0))
	require.Equal(t, int64(1), prepare.Value.ProposedTry)
}

// Tests that the channel works correctly when it receives a prepare message
func Test_Consensus_Publish_Prepare(t *testing.T) {
	consensusInstance := "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs="
	messageID := "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q="

	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/consensus"
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a client socket subscribed to the channel
	cliSocket := &fakeSocket{id: "cliSocket", sockType: socket.ClientSocketType}
	consensusChannel.sockets.Upsert(cliSocket)

	// Create a consensus elect message into the inbox of the channel
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	bufElect, err := os.ReadFile(file)
	require.NoError(t, err)

	bufbElect64 := base64.URLEncoding.EncodeToString(bufElect)

	electMessage := message.Message{
		Data:              bufbElect64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messageID,
		WitnessSignatures: []message.WitnessSignature{},
	}

	consensusChannel.inbox.StoreMessage(electMessage)

	// Add the state of the message to the channel
	consensusChannel.createMessageInstance(messageID, keypair.public)

	// Add a new consensus instance to the channel
	consensusChannel.createConsensusInstance(consensusInstance)

	// Create a consensus prepare message
	file = filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_prepare", "prepare.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	msg := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = msg

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = msg
	messageBroadcast.Params.Channel = chanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	require.NoError(t, channel.Publish(messagePublish, nil))
	require.Equal(t, byteBroad, cliSocket.msg)

	// Unmarshal the prepare message sent to other servers to verify its values
	var sentPublish method.Publish
	err = json.Unmarshal(fakeHub.fakeSock.msg, &sentPublish)
	require.NoError(t, err)

	sentMsg := sentPublish.Params.Message

	// Unmarshal the promise message data to check its values
	jsonData, err := base64.URLEncoding.DecodeString(sentMsg.Data)
	require.NoError(t, err)
	var promise messagedata.ConsensusPromise
	err = json.Unmarshal(jsonData, &promise)
	require.NoError(t, err)

	require.Equal(t, "consensus", promise.Object)
	require.Equal(t, "promise", promise.Action)
	require.Equal(t, consensusInstance, promise.InstanceID)
	require.Equal(t, messageID, promise.MessageID)
	require.GreaterOrEqual(t, promise.CreatedAt, int64(0))
	require.Equal(t, int64(-1), promise.Value.AcceptedTry)
	require.False(t, promise.Value.AcceptedValue)
	require.Equal(t, int64(4), promise.Value.PromisedTry)
}

// Tests that the channel works correctly when it receives a promise message
func Test_Consensus_Publish_Promise(t *testing.T) {
	consensusInstance := "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs="
	messageID := "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q="

	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/consensus"
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a client socket subscribed to the channel
	cliSocket := &fakeSocket{id: "cliSocket", sockType: socket.ClientSocketType}
	consensusChannel.sockets.Upsert(cliSocket)

	// Create a consensus elect message into the inbox of the channel
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	bufElect, err := os.ReadFile(file)
	require.NoError(t, err)

	bufbElect64 := base64.URLEncoding.EncodeToString(bufElect)

	electMessage := message.Message{
		Data:              bufbElect64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messageID,
		WitnessSignatures: []message.WitnessSignature{},
	}

	consensusChannel.inbox.StoreMessage(electMessage)

	// Add the state of the message to the channel
	consensusChannel.createMessageInstance(messageID, keypair.public)

	// Add a new consensus instance to the channel
	consensusChannel.createConsensusInstance(consensusInstance)

	// Create a consensus promise message
	file = filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_promise", "promise.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	msg := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = msg

	// Error should be sent if not enough elect accept have been received
	require.Error(t, channel.Publish(messagePublish, nil), "consensus corresponding to"+
		"the message hasn't entered the promise phase")

	// Update the state of the message
	consensusChannel.messageStates[messageID].currentPhase = PromisePhase

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = msg
	messageBroadcast.Params.Channel = chanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	require.NoError(t, channel.Publish(messagePublish, nil))
	require.Equal(t, byteBroad, cliSocket.msg)

	// Unmarshal the prepare message sent to other servers to verify its values
	var sentPublish method.Publish
	err = json.Unmarshal(fakeHub.fakeSock.msg, &sentPublish)
	require.NoError(t, err)

	sentMsg := sentPublish.Params.Message

	// Unmarshal the promise message data to check its values
	jsonData, err := base64.URLEncoding.DecodeString(sentMsg.Data)
	require.NoError(t, err)
	var propose messagedata.ConsensusPropose
	err = json.Unmarshal(jsonData, &propose)
	require.NoError(t, err)

	require.Equal(t, "consensus", propose.Object)
	require.Equal(t, "propose", propose.Action)
	require.Equal(t, consensusInstance, propose.InstanceID)
	require.Equal(t, messageID, propose.MessageID)
	require.GreaterOrEqual(t, propose.CreatedAt, int64(0))
	require.Equal(t, int64(4), propose.Value.ProposedTry)
	require.True(t, propose.Value.ProposedValue)
}

// Tests that the channel works correctly when it receives a propose message
func Test_Consensus_Publish_Propose(t *testing.T) {
	consensusInstance := "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs="
	messageID := "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q="

	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/consensus"
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a client socket subscribed to the channel
	cliSocket := &fakeSocket{id: "cliSocket", sockType: socket.ClientSocketType}
	consensusChannel.sockets.Upsert(cliSocket)

	// Create a consensus elect message into the inbox of the channel
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	bufElect, err := os.ReadFile(file)
	require.NoError(t, err)

	bufbElect64 := base64.URLEncoding.EncodeToString(bufElect)

	electMessage := message.Message{
		Data:              bufbElect64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messageID,
		WitnessSignatures: []message.WitnessSignature{},
	}

	consensusChannel.inbox.StoreMessage(electMessage)

	// Add the state of the message to the channel
	consensusChannel.createMessageInstance(messageID, keypair.public)

	// Add a new consensus instance to the channel
	consensusChannel.createConsensusInstance(consensusInstance)

	// Create a consensus propose message
	file = filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_propose", "propose.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	msg := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = msg

	// Error should be sent if not enough elect accept have been received
	require.Error(t, channel.Publish(messagePublish, nil), "consensus corresponding to"+
		"the message hasn't entered the promise phase")

	// Update the state of the message
	consensusChannel.messageStates[messageID].currentPhase = PromisePhase

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = msg
	messageBroadcast.Params.Channel = chanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	require.NoError(t, channel.Publish(messagePublish, nil))
	require.Equal(t, byteBroad, cliSocket.msg)

	// Unmarshal the prepare message sent to other servers to verify its values
	var sentPublish method.Publish
	err = json.Unmarshal(fakeHub.fakeSock.msg, &sentPublish)
	require.NoError(t, err)

	sentMsg := sentPublish.Params.Message

	// Unmarshal the accept message data to check its values
	jsonData, err := base64.URLEncoding.DecodeString(sentMsg.Data)
	require.NoError(t, err)
	var accept messagedata.ConsensusAccept
	err = json.Unmarshal(jsonData, &accept)
	require.NoError(t, err)

	require.Equal(t, "consensus", accept.Object)
	require.Equal(t, "accept", accept.Action)
	require.Equal(t, consensusInstance, accept.InstanceID)
	require.Equal(t, messageID, accept.MessageID)
	require.GreaterOrEqual(t, accept.CreatedAt, int64(0))
	require.Equal(t, int64(4), accept.Value.AcceptedTry)
	require.True(t, accept.Value.AcceptedValue)
}

// Tests that the channel works correctly when it receives an accept message
func Test_Consensus_Publish_Accept(t *testing.T) {
	consensusInstance := "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs="
	messageID := "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q="

	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/consensus"
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a client socket subscribed to the channel
	cliSocket := &fakeSocket{id: "cliSocket", sockType: socket.ClientSocketType}
	consensusChannel.sockets.Upsert(cliSocket)

	// Create a consensus elect message into the inbox of the channel
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	bufElect, err := os.ReadFile(file)
	require.NoError(t, err)

	bufbElect64 := base64.URLEncoding.EncodeToString(bufElect)

	electMessage := message.Message{
		Data:              bufbElect64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messageID,
		WitnessSignatures: []message.WitnessSignature{},
	}

	consensusChannel.inbox.StoreMessage(electMessage)

	// Add the state of the message to the channel
	consensusChannel.createMessageInstance(messageID, keypair.public)

	// Add a new consensus instance to the channel
	consensusChannel.createConsensusInstance(consensusInstance)
	consensusChannel.consensusInstances[consensusInstance].proposed_try = 4
	consensusChannel.consensusInstances[consensusInstance].proposed_value = true

	// Create a consensus propose message
	file = filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_accept", "accept.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	msg := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = msg

	// Error should be sent if not enough elect accept have been received
	require.Error(t, channel.Publish(messagePublish, nil), "consensus corresponding to"+
		"the message hasn't entered the accept phase")

	// Update the state of the message
	consensusChannel.messageStates[messageID].currentPhase = AcceptPhase

	// Update the proposed value to true
	consensusChannel.consensusInstances[consensusInstance].proposed_value = true

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = msg
	messageBroadcast.Params.Channel = chanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	require.NoError(t, channel.Publish(messagePublish, nil))
	require.Equal(t, byteBroad, cliSocket.msg)

	// Unmarshal the learn message sent to other servers to verify its values
	var sentPublish method.Publish
	err = json.Unmarshal(fakeHub.fakeSock.msg, &sentPublish)
	require.NoError(t, err)

	sentMsg := sentPublish.Params.Message

	// Unmarshal the learn message data to check its values
	jsonData, err := base64.URLEncoding.DecodeString(sentMsg.Data)
	require.NoError(t, err)
	var learn messagedata.ConsensusLearn
	err = json.Unmarshal(jsonData, &learn)
	require.NoError(t, err)

	require.Equal(t, "consensus", learn.Object)
	require.Equal(t, "learn", learn.Action)
	require.Equal(t, consensusInstance, learn.InstanceID)
	require.Equal(t, messageID, learn.MessageID)
	require.GreaterOrEqual(t, learn.CreatedAt, int64(0))
	require.True(t, learn.Value.Decision)
}

// Tests that the channel works correctly when it receives a learn message
func Test_Consensus_Publish_Learn(t *testing.T) {
	consensusInstance := "6wCJZmUn0UwsdZGyJVy7iiAIiPEHwsBRmIsL_TxM4Cs="
	messageID := "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q="

	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/consensus"
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Create a client socket subscribed to the channel
	cliSocket := &fakeSocket{id: "cliSocket", sockType: socket.ClientSocketType}
	consensusChannel.sockets.Upsert(cliSocket)

	// Create a consensus elect message into the inbox of the channel
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_elect", "elect.json")
	bufElect, err := os.ReadFile(file)
	require.NoError(t, err)

	bufbElect64 := base64.URLEncoding.EncodeToString(bufElect)

	electMessage := message.Message{
		Data:              bufbElect64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         "7J0d6d8Bw28AJwB4ttOUiMgm_DUTHSYFXM30_8kmd1Q=",
		WitnessSignatures: []message.WitnessSignature{},
	}

	consensusChannel.inbox.StoreMessage(electMessage)

	// Add the state of the message to the channel
	consensusChannel.createMessageInstance(messageID, keypair.public)

	// Add a new consensus instance to the channel
	consensusChannel.createConsensusInstance(consensusInstance)

	// Create a consensus learn message
	file = filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_learn", "learn.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	bufb64 := base64.URLEncoding.EncodeToString(buf)

	message := message.Message{
		Data:              bufb64,
		Sender:            publicKey64,
		Signature:         "h",
		MessageID:         messagedata.Hash(bufb64, publicKey64),
		WitnessSignatures: []message.WitnessSignature{},
	}

	filePublish := filepath.Join(protocolRelativePath,
		"examples", "query", "publish", "publish.json")
	bufPub, err := os.ReadFile(filePublish)
	require.NoError(t, err)

	var messagePublish method.Publish

	err = json.Unmarshal(bufPub, &messagePublish)
	require.NoError(t, err)

	messagePublish.Params.Message = message

	// Create the broadcast message to check that it is sent
	fileBroadcast := filepath.Join(protocolRelativePath,
		"examples", "query", "broadcast", "broadcast.json")
	bufBroad, err := os.ReadFile(fileBroadcast)
	require.NoError(t, err)

	var messageBroadcast method.Broadcast

	err = json.Unmarshal(bufBroad, &messageBroadcast)
	require.NoError(t, err)

	messageBroadcast.Params.Message = message
	messageBroadcast.Params.Channel = chanName
	byteBroad, err := json.Marshal(&messageBroadcast)
	require.NoError(t, err)

	require.NoError(t, channel.Publish(messagePublish, nil))
	require.Equal(t, byteBroad, cliSocket.msg)
}

// Tests that a message sent is correct depending on the data
func Test_Publish_New_Message(t *testing.T) {

	// Create the hub
	keypair := generateKeyPair(t)
	publicKey64 := base64.URLEncoding.EncodeToString(keypair.publicBuf)

	fakeHub, err := NewfakeHub(keypair.public, nolog, nil)
	require.NoError(t, err)

	// Create the channel
	chanName := "fzJSZjKf-2cbXH7kds9H8NORuuFIRLkevJlN7qQemjo=/consensus"
	channel := NewChannel(chanName, fakeHub, nolog)
	consensusChannel, ok := channel.(*Channel)
	require.True(t, ok)

	// Get the data for the sent message
	file := filepath.Join(protocolRelativePath,
		"examples", "messageData", "consensus_learn", "learn.json")
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	// Create and send the new message
	err = consensusChannel.publishNewMessage(buf)
	require.NoError(t, err)

	// Create the expected sent message
	data64 := base64.URLEncoding.EncodeToString(buf)

	signatureBuf, err := fakeHub.Sign(buf)
	require.NoError(t, err)

	signature := base64.URLEncoding.EncodeToString(signatureBuf)

	expectedMessage := message.Message{
		Data:              data64,
		Sender:            publicKey64,
		Signature:         signature,
		MessageID:         messagedata.Hash(data64, signature),
		WitnessSignatures: make([]message.WitnessSignature, 0),
	}

	// Unmarshal the sent message
	var publish method.Publish
	err = json.Unmarshal(fakeHub.fakeSock.msg, &publish)
	require.NoError(t, err)

	sentMsg := publish.Params.Message

	// Doesn't compare the signature and the message id, as they have random
	// elements
	require.Equal(t, expectedMessage.Data, sentMsg.Data)
	require.Equal(t, expectedMessage.Sender, sentMsg.Sender)
	require.Equal(t, expectedMessage.WitnessSignatures, sentMsg.WitnessSignatures)
}

// -----------------------------------------------------------------------------
// Utility functions

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

func (h *fakeHub) RegisterNewChannel(channeID string, channel channel.Channel) {
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

func (h *fakeHub) SendAndHandleMessage(publishMsg method.Publish) error {
	byteMsg, err := json.Marshal(publishMsg)
	if err != nil {
		return err
	}

	h.fakeSock.msg = byteMsg

	return nil
}

func (h *fakeHub) NotifyNewChannel(channelID string, channel channel.Channel, socket socket.Socket) {}

func (h *fakeHub) SetMessageID(publish *method.Publish) {
	publish.ID = 0
}

// fakeSocket is a fake implementation of a socket
//
// - implements socket.Socket
type fakeSocket struct {
	socket.Socket

	sockType socket.SocketType

	resultID int
	res      []message.Message
	msg      []byte

	err error

	// the socket ID
	id string
}

// Get the type of the fake socket
func (f *fakeSocket) Type() socket.SocketType {
	return f.sockType
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
