package witness

import (
	"context"
	"encoding/json"
	"fmt"
	"popstellar/channel"
	"popstellar/hub"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"popstellar/validation"
	"sync"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/sync/semaphore"
	"golang.org/x/xerrors"
)

const (
	numWorkers = 10
)

// Hub represents a Witness and implements the Hub interface.
type Hub struct {
	messageChan chan socket.IncomingMessage

	sync.RWMutex
	channelByID map[string]channel.Channel

	closedSockets chan string

	public kyber.Point

	schemaValidator *validation.SchemaValidator

	stop chan struct{}

	workers *semaphore.Weighted

	log zerolog.Logger
}

// NewHub returns a new Witness Hub.
func NewHub(public kyber.Point, log zerolog.Logger) (*Hub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	witnessHub := Hub{
		messageChan:     make(chan socket.IncomingMessage),
		channelByID:     make(map[string]channel.Channel),
		closedSockets:   make(chan string),
		public:          public,
		schemaValidator: schemaValidator,
		stop:            make(chan struct{}),
		workers:         semaphore.NewWeighted(numWorkers),
		log:             log,
	}

	return &witnessHub, nil
}

func (h *Hub) handleMessageFromOrganizer(incMsg *socket.IncomingMessage) {
	socket := incMsg.Socket
	byteMessage := incMsg.Message

	// validate against json schema
	err := h.schemaValidator.VerifyJSON(byteMessage, validation.GenericMessage)
	if err != nil {
		h.log.Err(err).Msg("message is not valid against json schema")
		socket.SendError(nil, xerrors.Errorf("message is not valid against json schema: %v", err))
		return
	}

	var queryBase query.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := answer.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
		h.log.Err(err)
		socket.SendError(nil, err)
		return
	}

	var id int
	var msgs []message.Message
	var handlerErr error

	switch queryBase.Method {
	// TODO : treat the different message that the witness can get
	}

	if handlerErr != nil {
		err := answer.NewErrorf(-4, "failed to handle method: %v", handlerErr)
		h.log.Err(err)
		socket.SendError(nil, err)
		return
	}

	if queryBase.Method == query.MethodCatchUp {
		socket.SendResult(id, msgs)
		return
	}

	socket.SendResult(id, nil)
}

func (h *Hub) handleMessageFromClient(incMsg *socket.IncomingMessage) {
	socket := incMsg.Socket
	byteMessage := incMsg.Message

	// validate against json schema
	err := h.schemaValidator.VerifyJSON(byteMessage, validation.GenericMessage)
	if err != nil {
		h.log.Err(err).Msg("message is not valid against json schema")
		socket.SendError(nil, xerrors.Errorf("message is not valid against json schema: %v", err))
		return
	}

	var queryBase query.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := answer.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
		h.log.Err(err)
		socket.SendError(nil, err)
		return
	}

	var id int
	var msgs []message.Message
	var handlerErr error

	switch queryBase.Method {
	// TODO : treat the different message that the witness can get
	}

	if handlerErr != nil {
		err := answer.NewErrorf(-4, "failed to handle method: %v", handlerErr)
		h.log.Err(err)
		socket.SendError(nil, err)
		return
	}

	if queryBase.Method == query.MethodCatchUp {
		socket.SendResult(id, msgs)
		return
	}

	socket.SendResult(id, nil)
}

func (h *Hub) handleMessageFromWitness(incMsg *socket.IncomingMessage) {
	socket := incMsg.Socket
	byteMessage := incMsg.Message

	// validate against json schema
	err := h.schemaValidator.VerifyJSON(byteMessage, validation.GenericMessage)
	if err != nil {
		h.log.Err(err).Msg("message is not valid against json schema")
		socket.SendError(nil, xerrors.Errorf("message is not valid against json schema: %v", err))
		return
	}

	var queryBase query.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := answer.NewErrorf(-4, "failed to unmarshal incoming message: %v", err)
		h.log.Err(err)
		socket.SendError(nil, err)
		return
	}

	var id int
	var msgs []message.Message
	var handlerErr error

	switch queryBase.Method {
	// TODO : treat the different message that the witness can get
	}

	if handlerErr != nil {
		err := answer.NewErrorf(-4, "failed to handle method: %v", handlerErr)
		h.log.Err(err)
		socket.SendError(nil, err)
		return
	}

	if queryBase.Method == query.MethodCatchUp {
		socket.SendResult(id, msgs)
		return
	}

	socket.SendResult(id, nil)
}

func (h *Hub) handleIncomingMessage(incMsg *socket.IncomingMessage) {
	defer h.workers.Release(1)

	h.log.Info().Str("msg", fmt.Sprintf("%v", incMsg.Message)).
		Str("from", incMsg.Socket.ID()).
		Msgf("handle incoming message")

	switch incMsg.Socket.Type() {
	case socket.OrganizerSocketType:
		h.handleMessageFromOrganizer(incMsg)
		return
	case socket.ClientSocketType:
		h.handleMessageFromClient(incMsg)
		return
	case socket.WitnessSocketType:
		h.handleMessageFromWitness(incMsg)
		return
	}
}

// Start implements hub.Hub
func (h *Hub) Start() {
	h.log.Info().Msg("started witness...")

	go func() {
		for {
			select {
			case incMsg := <-h.messageChan:
				ok := h.workers.TryAcquire(1)
				if !ok {
					h.log.Warn().Msg("worker pool full, waiting...")
					h.workers.Acquire(context.Background(), 1)
				}

				h.handleIncomingMessage(&incMsg)
			case id := <-h.closedSockets:
				h.RLock()
				for _, channel := range h.channelByID {
					// dummy Unsubscribe message because it's only used for logging...
					channel.Unsubscribe(id, method.Unsubscribe{})
				}
				h.RUnlock()
			case <-h.stop:
				h.log.Info().Msg("closing the hub...")
				return
			}
		}
	}()
}

// Stop implements hub.Hub
func (h *Hub) Stop() {
	close(h.stop)
	h.log.Info().Msg("waiting for existing workers to finish...")
	h.workers.Acquire(context.Background(), numWorkers)
}

// Receiver implements hub.Hub
func (h *Hub) Receiver() chan<- socket.IncomingMessage {
	return h.messageChan
}

// OnSocketClose implements hub.Hub
func (h *Hub) OnSocketClose() chan<- string {
	return h.closedSockets
}

// Type implements hub.Hub
func (h *Hub) Type() hub.HubType {
	return hub.WitnessHubType
}
