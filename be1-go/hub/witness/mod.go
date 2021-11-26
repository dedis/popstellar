package witness

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/channel"
	"popstellar/crypto"
	"popstellar/hub"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
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

// rootPrefix denotes the prefix for the root channel
// used to keep an image of the laos
const rootPrefix = "/root/"

// Strings used to return error messages
const jsonNotValidError = "message is not valid against json schema"
const unmarshalError = "failed to unmarshal incoming message %v"
const unexpectedMethodError = "unexpected method: '%s'"
const failedMethodHandling = "failed to handle method: %v"

const (
	numWorkers = 10
)


// Hub represents a Witness and implements the Hub interface.
type Hub struct {
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

	// laoFac is there to allow a similar implementation to the organizer
	laoFac channel.LaoFactory
}

// NewHub returns a new Witness Hub.
func NewHub(publicWit kyber.Point, log zerolog.Logger, laoFac channel.LaoFactory) (*Hub, error) {

	schemaValidator, err := validation.NewSchemaValidator(log)
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	log = log.With().Str("role", "base hub").Logger()

	pubServ, secServ := generateKeys()

	witnessHub := Hub{
		messageChan:     make(chan socket.IncomingMessage),
		channelByID:     make(map[string]channel.Channel),
		closedSockets:   make(chan string),
		pubKeyOrg:       publicWit,
		pubKeyServ: 	 pubServ,
		secKeyServ: 	 secServ,
		schemaValidator: schemaValidator,
		stop:            make(chan struct{}),
		workers:         semaphore.NewWeighted(numWorkers),
		log:             log,
		laoFac:          laoFac,
	}

	return &witnessHub, nil
}

// tempHandleMessage lets a witness handle message.
// As the Witness for now imitate the organizer when getting message, this
// function is there while waiting a correct witness implementation. Function
// there to reduce code duplication
func (h *Hub) tempHandleMessage(incMsg *socket.IncomingMessage) error {
	socket := incMsg.Socket
	byteMessage := incMsg.Message

	// validate against json schema
	err := h.schemaValidator.VerifyJSON(byteMessage, validation.GenericMessage)
	if err != nil {
		h.log.Err(err).Msg(jsonNotValidError)
		socket.SendError(nil, xerrors.Errorf(jsonNotValidError+": %v", err))
		return err
	}

	var queryBase query.Base

	err = json.Unmarshal(byteMessage, &queryBase)
	if err != nil {
		err := answer.NewErrorf(-4, unmarshalError, err)
		h.log.Err(err)
		socket.SendError(nil, err)
		return err
	}

	var id int
	var msgs []message.Message
	var handlerErr error

	switch queryBase.Method {
	case query.MethodPublish:
		id, handlerErr = h.handlePublish(socket, byteMessage)
	case query.MethodSubscribe:
		id, handlerErr = h.handleSubscribe(socket, byteMessage)
	case query.MethodUnsubscribe:
		id, handlerErr = h.handleUnsubscribe(socket, byteMessage)
	case query.MethodCatchUp:
		msgs, id, handlerErr = h.handleCatchup(byteMessage)
	default:
		err = answer.NewErrorf(-2, unexpectedMethodError, queryBase.Method)
		h.log.Err(err)
		socket.SendError(nil, err)
		return err
	}

	if handlerErr != nil {
		err := answer.NewErrorf(-4, failedMethodHandling, handlerErr)
		h.log.Err(err)
		socket.SendError(&id, err)
		return err
	}

	if queryBase.Method == query.MethodCatchUp {
		socket.SendResult(id, msgs)
		return err
	}

	socket.SendResult(id, nil)
	return nil
}

//
func (h *Hub) handleMessageFromOrganizer(incMsg *socket.IncomingMessage) error {
	return h.tempHandleMessage(incMsg)
}

func (h *Hub) handleMessageFromClient(incMsg *socket.IncomingMessage) error {
	return h.tempHandleMessage(incMsg)
}

func (h *Hub) handleMessageFromWitness(incMsg *socket.IncomingMessage) error {
	return h.tempHandleMessage(incMsg)
}

func (h *Hub) handleIncomingMessage(incMsg *socket.IncomingMessage) error {
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

// For now the witnesses work the same as organizer, all following functions are
// tied to this particular comportement.

// GetPubkeyOrg implements channel.HubFunctionalities
func (h *Hub) GetPubKeyOrg() kyber.Point {
	return h.pubKeyOrg
}

// GetPubKeyServ implements channel.HubFunctionalities
func (h *Hub) GetPubKeyServ() kyber.Point {
	return h.pubKeyServ
}

// Sign implements channel.HubFunctionalities
func (h *Hub) Sign(data []byte) ([]byte, error) {
	signatureBuf, err := schnorr.Sign(crypto.Suite, h.secKeyServ, data)
	if err != nil {
		return nil, xerrors.Errorf("failed to sign the data: %v", err)
	}
	return signatureBuf, nil
}

// GetSchemaValidator implements channel.HubFunctionalities
func (h *Hub) GetSchemaValidator() validation.SchemaValidator {
	return *h.schemaValidator
}

func generateKeys() (kyber.Point, kyber.Scalar) {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	return point, secret
}

// createLao creates a new LAO using the data in the publish parameter.
func (h *Hub) createLao(publish method.Publish, laoCreate messagedata.LaoCreate) error {

	laoChannelPath := rootPrefix + laoCreate.ID

	if _, ok := h.channelByID[laoChannelPath]; ok {
		h.log.Error().Msgf("failed to create lao: duplicate lao path: %q", laoChannelPath)
		return answer.NewErrorf(-3, "failed to create lao: duplicate lao path: %q", laoChannelPath)
	}

	laoCh := h.laoFac(laoChannelPath, h, publish.Params.Message, h.log)

	h.log.Info().Msgf("storing new channel '%s' %v", laoChannelPath, publish.Params.Message)

	h.RegisterNewChannel(laoChannelPath, laoCh)

	return nil
}

// RegisterNewChannel implements channel.HubFunctionalities
func (h *Hub) RegisterNewChannel(channeID string, channel channel.Channel) {
	h.Lock()
	h.channelByID[channeID] = channel
	h.Unlock()
}

// handleRootChannelMesssage handles an incoming message on the root channel.
func (h *Hub) handleRootChannelMesssage(socket socket.Socket, publish method.Publish) {
	jsonData, err := base64.URLEncoding.DecodeString(publish.Params.Message.Data)
	if err != nil {
		socket.SendError(&publish.ID, xerrors.Errorf("failed to decode message data: %v", err))
		return
	}

	// validate message data against the json schema
	err = h.schemaValidator.VerifyJSON(jsonData, validation.Data)
	if err != nil {
		socket.SendError(&publish.ID, err)
		return
	}

	// get object#action
	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		socket.SendError(&publish.ID, err)
		return
	}

	// must be "lao#create"
	if object != messagedata.LAOObject || action != messagedata.LAOActionCreate {
		err := answer.NewErrorf(-1, "only lao#create is allowed on root, "+
			"but found %s#%s", object, action)
		h.log.Err(err)
		socket.SendError(&publish.ID, err)
		return
	}

	var laoCreate messagedata.LaoCreate

	err = publish.Params.Message.UnmarshalData(&laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to unmarshal lao#create")
		socket.SendError(&publish.ID, err)
		return
	}

	err = h.createLao(publish, laoCreate)
	if err != nil {
		h.log.Err(err).Msg("failed to create lao")
		socket.SendError(&publish.ID, err)
		return
	}
}

// handlePublish let a witness handle a publish message
func (h *Hub) handlePublish(socket socket.Socket, byteMessage []byte) (int, error) {
	var publish method.Publish

	err := json.Unmarshal(byteMessage, &publish)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	if publish.Params.Channel == "/root" {
		h.handleRootChannelMesssage(socket, publish)
		return publish.ID, nil
	}

	channel, err := h.getChan(publish.Params.Channel)
	if err != nil {
		return publish.ID, xerrors.Errorf("failed to get channel: %v", err)
	}

	err = channel.Publish(publish)
	if err != nil {
		return publish.ID, xerrors.Errorf("failed to publish: %v", err)
	}

	return publish.ID, nil
}

// handleSubscribe let a witness handle a subscribe message
func (h *Hub) handleSubscribe(socket socket.Socket, byteMessage []byte) (int, error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(byteMessage, &subscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal subscribe message: %v", err)
	}

	channel, err := h.getChan(subscribe.Params.Channel)
	if err != nil {
		return subscribe.ID, xerrors.Errorf("failed to get subscribe channel: %v", err)
	}

	err = channel.Subscribe(socket, subscribe)
	if err != nil {
		return subscribe.ID, xerrors.Errorf("failed to publish: %v", err)
	}

	return subscribe.ID, nil
}

// handleUnsubscribe let a witness handle an unsubscribe message
func (h *Hub) handleUnsubscribe(socket socket.Socket, byteMessage []byte) (int, error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(byteMessage, &unsubscribe)
	if err != nil {
		return -1, xerrors.Errorf("failed to unmarshal unsubscribe message: %v", err)
	}

	channel, err := h.getChan(unsubscribe.Params.Channel)
	if err != nil {
		return unsubscribe.ID, xerrors.Errorf("failed to get unsubscribe channel: %v", err)
	}

	err = channel.Unsubscribe(socket.ID(), unsubscribe)
	if err != nil {
		return unsubscribe.ID, xerrors.Errorf("failed to unsubscribe: %v", err)
	}

	return unsubscribe.ID, nil
}

// handleCatchup let a witness handle a catchup message
func (h *Hub) handleCatchup(byteMessage []byte) ([]message.Message, int, error) {
	var catchup method.Catchup

	err := json.Unmarshal(byteMessage, &catchup)
	if err != nil {
		return nil, -1, xerrors.Errorf("failed to unmarshal catchup message: %v", err)
	}

	channel, err := h.getChan(catchup.Params.Channel)
	if err != nil {
		return nil, catchup.ID, xerrors.Errorf("failed to get catchup channel: %v", err)
	}

	msg := channel.Catchup(catchup)
	if err != nil {
		return nil, catchup.ID, xerrors.Errorf("failed to catchup: %v", err)
	}

	return msg, catchup.ID, nil
}

// getChan finds a channel based on its path
func (h *Hub) getChan(channelPath string) (channel.Channel, error) {
	if channelPath[:6] != rootPrefix {
		return nil, xerrors.Errorf("channel id must begin with \"/root/\", got: %q", channelPath[:6])
	}

	h.RLock()
	defer h.RUnlock()

	channel, ok := h.channelByID[channelPath]
	if !ok {
		return nil, xerrors.Errorf("channel %s does not exist", channelPath)
	}

	return channel, nil
}
