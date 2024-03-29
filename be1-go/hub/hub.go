// Package hub defines an interface that is used for processing incoming
// JSON-RPC messages from the websocket connection and replying to them by
// either sending a Result, Error or broadcasting a message to other clients.
package hub

import (
	"encoding/json"
	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	state "popstellar/hub/standard_hub/hub_state"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"time"
)

const (
	// rootChannel denotes the id of the root channel
	rootChannel = "/root"

	// rootPrefix denotes the prefix for the root channel
	// used to keep an image of the laos
	rootPrefix = rootChannel + "/"

	// Strings used to return error messages
	rootChannelErr = "failed to handle root channel message: %v"
	getChannelErr  = "failed to get channel: %v"

	// numWorkers denote the number of worker go-routines
	// allowed to process requests concurrently.
	numWorkers = 10

	// heartbeatDelay represents the number of seconds
	// between heartbeat messages
	heartbeatDelay = 30 * time.Second
)

const (
	channelRoot         = "root"
	channelLao          = "lao"
	channelElection     = "election"
	channelGeneralChirp = "generalchirp"
	channelChirp        = "chirp"
	channelReaction     = "reaction"
	channelConsensus    = "consensus"
	channelPopCha       = "popcha"
	channelCoin         = "coin"
)

// Hub defines the methods a PoP server must implement to receive messages
// and handle clients.
type Hub interface {
	// NotifyNewServer add a socket for the hub to send message to other servers
	NotifyNewServer(socket.Socket)

	// Start invokes the processing loop for the hub.
	Start()

	// Stop closes the processing loop for the hub.
	Stop()

	// Receiver returns a channel that may be used to process incoming messages
	Receiver() chan<- socket.IncomingMessage

	// OnSocketClose returns a channel which accepts socket ids on connection
	// close events. This allows the hub to cleanup clients which close without
	// sending an unsubscribe message
	OnSocketClose() chan<- string

	// SendGreetServer sends a greet server message in the socket
	SendGreetServer(socket.Socket) error
}

type subscribers map[string]map[string]socket.Socket

func (s subscribers) addChannel(channel string) {
	s[channel] = make(map[string]socket.Socket)
}

func (s subscribers) removeChannel(channel string) {
	delete(s, channel)
}

func (s subscribers) subscribe(channel string, socket socket.Socket) error {
	_, ok := s[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot subscribe to unknown channel")
	}

	s[channel][socket.ID()] = socket

	return nil
}

func (s subscribers) unsubscribe(channel string, socket socket.Socket) error {
	_, ok := s[channel]
	if !ok {
		return answer.NewInvalidResourceError("cannot unsubscribe from unknown channel")
	}

	_, ok = s[channel][socket.ID()]
	if !ok {
		return answer.NewInvalidActionError("cannot unsubscribe from a channel not subscribed")
	}

	delete(s[channel], socket.ID())

	return nil
}

type handlerParameters struct {
	log                 zerolog.Logger
	socket              socket.Socket
	schemaValidator     validation.SchemaValidator
	db                  Repository
	subs                subscribers
	peers               *state.Peers
	queries             *state.Queries
	ownerPubKey         kyber.Point
	clientServerAddress string
	serverServerAddress string
}

// SendToAll sends a message to all sockets.
func SendToAll(subs subscribers, buf []byte, channel string) error {

	sockets, ok := subs[channel]
	if !ok {
		return xerrors.Errorf("channel %s not found", channel)
	}
	for _, v := range sockets {
		v.Send(buf)
	}
	return nil
}

func broadcastToAllClients(msg message.Message, params handlerParameters, channel string) error {
	rpcMessage := method.Broadcast{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "broadcast",
		},
		Params: struct {
			Channel string          `json:"channel"`
			Message message.Message `json:"message"`
		}{
			channel,
			msg,
		},
	}

	buf, err := json.Marshal(&rpcMessage)
	if err != nil {
		return xerrors.Errorf("failed to marshal broadcast query: %v", err)
	}

	err = SendToAll(params.subs, buf, channel)
	if err != nil {
		return xerrors.Errorf("failed to send broadcast message to all clients: %v", err)
	}

	return nil
}

func Sign(data []byte, params handlerParameters) ([]byte, error) {

	serverSecretBuf, err := params.db.GetServerSecretKey()
	if err != nil {
		return nil, xerrors.Errorf("failed to get the server secret key")
	}

	serverSecretKey := crypto.Suite.Scalar()
	err = serverSecretKey.UnmarshalBinary(serverSecretBuf)
	signatureBuf, err := schnorr.Sign(crypto.Suite, serverSecretKey, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}
	return signatureBuf, nil
}
