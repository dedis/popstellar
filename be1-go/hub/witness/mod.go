package witness

import (
	"context"
	"fmt"
	"log"
	"popstellar/channel"
	"popstellar/hub"
	"popstellar/message/query/method"
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

	schemaValidator, err := validation.NewSchemaValidator()
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

func (h *Hub) handleMessageFromOrganizer(incMsg *socket.IncomingMessage) error {
	panic("handleMessageFromOrganizer not implemented")
}

func (h *Hub) handleMessageFromClient(incMsg *socket.IncomingMessage) error {
	panic("handleMessageFromClient not implemented")
}

func (h *Hub) handleMessageFromWitness(incMsg *socket.IncomingMessage) error {
	panic("handleMessageFromWitness not implemented")
}

func (h *Hub) handleIncomingMessage(incMsg *socket.IncomingMessage) error{
	defer h.workers.Release(1)

	h.log.Info().Str("msg", fmt.Sprintf("%v", incMsg.Message)).
		Str("from", incMsg.Socket.ID()).
		Msgf("handle incoming message")

	switch incMsg.Socket.Type() {
	case socket.OrganizerSocketType:
		return h.handleMessageFromOrganizer(incMsg)
	case socket.ClientSocketType:
		return h.handleMessageFromClient(incMsg)
	case socket.WitnessSocketType:
		return h.handleMessageFromWitness(incMsg)
	default:
		h.log.Error().Msg("invalid socket type")
		return xerrors.Errorf("invalid socket type")
	}
}

// Start implements hub.Hub
func (h *Hub) Start() {
	log.Printf("started witness...")

	go func() {
		for {
			select {
			case incMsg := <-h.messageChan:
				ok := h.workers.TryAcquire(1)
				if !ok {
					log.Print("warn: worker pool full, waiting...")
					h.workers.Acquire(context.Background(), 1)
				}

				err := h.handleIncomingMessage(&incMsg)
				if err != nil {
					h.log.Err(err).Msg("problem handling incoming message")
				}
			case id := <-h.closedSockets:
				h.RLock()
				for _, channel := range h.channelByID {
					// dummy Unsubscribe message because it's only used for logging...
					channel.Unsubscribe(id, method.Unsubscribe{})
				}
				h.RUnlock()
			case <-h.stop:
				log.Println("closing the hub...")
				return
			}
		}
	}()
}

// Stop implements hub.Hub
func (h *Hub) Stop() {
	close(h.stop)
	log.Println("Waiting for existing workers to finish...")
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
