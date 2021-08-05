package hub

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"sync"

	"student20_pop/message"
	"student20_pop/validation"

	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
)

// rootPrefix denotes the prefix for the root channel
const rootPrefix = "/root/"

// baseHub implements hub.Hub interface
type baseHub struct {
	messageChan chan IncomingMessage

	sync.RWMutex
	channelByID map[string]Channel

	closedSockets chan string

	public kyber.Point

	schemaValidator *validation.SchemaValidator

	wg *sync.WaitGroup
}

// NewBaseHub returns a Base Hub.
func NewBaseHub(public kyber.Point, wg *sync.WaitGroup) (*baseHub, error) {

	schemaValidator, err := validation.NewSchemaValidator()
	if err != nil {
		return nil, xerrors.Errorf("failed to create the schema validator: %v", err)
	}

	return &baseHub{
		messageChan:     make(chan IncomingMessage),
		channelByID:     make(map[string]Channel),
		closedSockets:   make(chan string),
		public:          public,
		schemaValidator: schemaValidator,
		wg:              wg,
	}, nil
}

func (h *baseHub) Start(ctx context.Context) {
	h.wg.Add(1)
	defer h.wg.Done()

	log.Printf("started hub...")
	for {
		select {
		case incomingMessage := <-h.messageChan:
			h.handleIncomingMessage(&incomingMessage)
		case id := <-h.closedSockets:
			h.RLock()
			for _, channel := range h.channelByID {
				// dummy Unsubscribe message because it's only used for logging...
				channel.Unsubscribe(id, message.Unsubscribe{})
			}
			h.RUnlock()
		case <-ctx.Done():
			log.Println("closing the hub...")
			return
		}
	}
}

func (h *baseHub) Receiver() chan<- IncomingMessage {
	return h.messageChan
}

func (h *baseHub) OnSocketClose() chan<- string {
	return h.closedSockets
}

// handleRootChannelMesssage handles an incoming message on the root channel.
func (h *baseHub) handleRootChannelMesssage(id int, socket Socket, query *message.Query) {
	if query.Publish == nil {
		err := &message.Error{
			Code:        -4,
			Description: "only publish is allowed on /root",
		}

		socket.SendError(&id, err)
		return
	}

	// Check if the structure of the message is correct
	msg := query.Publish.Params.Message

	// Verify the data
	err := h.schemaValidator.VerifyJson(msg.RawData, validation.Data)
	if err != nil {
		err = message.NewError("failed to validate the data", err)
		socket.SendError(&id, err)
		return
	}

	// Unmarshal the data
	err = query.Publish.Params.Message.VerifyAndUnmarshalData()
	if err != nil {
		// Return a error of type "-4 request data is invalid" for all the verifications and unmarshalling problems of the data
		err = &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to verify and unmarshal data: %v", err),
		}
		socket.SendError(&id, err)
		return
	}

	if query.Publish.Params.Message.Data.GetAction() == message.DataAction(message.CreateLaoAction) &&
		query.Publish.Params.Message.Data.GetObject() == message.DataObject(message.LaoObject) {
		err := h.createLao(*query.Publish)
		if err != nil {
			err = message.NewError("failed to create lao", err)

			socket.SendError(&id, err)
			return
		}
	} else {
		log.Printf("invalid method: %s", query.GetMethod())
		socket.SendError(&id, &message.Error{
			Code:        -1,
			Description: "you may only invoke lao/create on /root",
		})
		return
	}

	status := 0
	result := message.Result{General: &status}

	log.Printf("sending result: %+v", result)
	socket.SendResult(id, result)
}

// handleMessageFromClient handles an incoming message from an end user.
func (h *baseHub) handleMessageFromClient(incomingMessage *IncomingMessage) {
	socket := incomingMessage.Socket
	byteMessage := incomingMessage.Message

	// Check if the GenericMessage has a field "id"
	genericMsg := &message.GenericMessage{}
	id, ok := genericMsg.UnmarshalID(byteMessage)
	if !ok {
		err := &message.Error{
			Code:        -4,
			Description: "The message does not have a valid `id` field",
		}
		socket.SendError(nil, err)
		return
	}

	// Verify the message
	err := h.schemaValidator.VerifyJson(byteMessage, validation.GenericMessage)
	if err != nil {
		err = message.NewError("failed to verify incoming message", err)
		socket.SendError(&id, err)
		return
	}

	// Unmarshal the message
	err = json.Unmarshal(byteMessage, genericMsg)
	if err != nil {
		// Return a error of type "-4 request data is invalid" for all the unmarshalling problems of the incoming message
		err = &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to unmarshal incoming message: %v", err),
		}

		socket.SendError(&id, err)
		return
	}

	query := genericMsg.Query

	if query == nil {
		return
	}

	channelID := query.GetChannel()
	log.Printf("channel: %s", channelID)

	if channelID == "/root" {
		h.handleRootChannelMesssage(id, socket, query)
		return
	}

	if channelID[:6] != rootPrefix {
		log.Printf("channel id must begin with /root/")
		socket.SendError(&id, &message.Error{
			Code:        -2,
			Description: "channel id must begin with /root/",
		})
		return
	}

	channelID = channelID[6:]
	h.RLock()
	channel, ok := h.channelByID[channelID]
	if !ok {
		log.Printf("invalid channel id: %s", channelID)
		socket.SendError(&id, &message.Error{
			Code:        -2,
			Description: fmt.Sprintf("channel with id %s does not exist", channelID),
		})
		h.RUnlock()
		return
	}
	h.RUnlock()

	method := query.GetMethod()
	log.Printf("method: %s", method)

	msg := []message.Message{}

	// TODO: use constants
	switch method {
	case "subscribe":
		err = channel.Subscribe(socket, *query.Subscribe)
	case "unsubscribe":
		err = channel.Unsubscribe(socket.ID(), *query.Unsubscribe)
	case "publish":
		err = channel.Publish(*query.Publish)
	case "message":
		log.Printf("cannot handle broadcasts right now")
	case "catchup":
		msg = channel.Catchup(*query.Catchup)
		// TODO send catchup response to client
	}

	if err != nil {
		err = message.NewError("failed to process query", err)
		socket.SendError(&id, err)
		return
	}

	result := message.Result{}

	if method == "catchup" {
		result.Catchup = msg
	} else {
		general := 0
		result.General = &general
	}

	socket.SendResult(id, result)
}

// handleMessageFromWitness handles an incoming message from a witness server.
func (h *baseHub) handleMessageFromWitness(incomingMessage *IncomingMessage) {
	//TODO
}

// handleIncomingMessage handles an incoming message based on the socket it
// originates from.
func (h *baseHub) handleIncomingMessage(incomingMessage *IncomingMessage) {
	log.Printf("Hub::handleMessageFromClient: %s", incomingMessage.Message)

	switch incomingMessage.Socket.Type() {
	case ClientSocketType:
		h.handleMessageFromClient(incomingMessage)
	case WitnessSocketType:
		h.handleMessageFromWitness(incomingMessage)
	default:
		log.Printf("error: invalid socket type")
	}

}

// createLao creates a new LAO using the data in the publish parameter.
func (h *baseHub) createLao(publish message.Publish) error {
	h.Lock()
	defer h.Unlock()

	data, ok := publish.Params.Message.Data.(*message.CreateLAOData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: "failed to cast data to CreateLAOData",
		}
	}

	encodedID := base64.URLEncoding.EncodeToString(data.ID)
	if _, ok := h.channelByID[encodedID]; ok {
		return &message.Error{
			Code:        -3,
			Description: "failed to create lao: another one with the same ID exists",
		}
	}

	laoChannelID := rootPrefix + encodedID

	laoCh := laoChannel{
		rollCall:    rollCall{},
		attendees:   NewAttendees(),
		baseChannel: createBaseChannel(h, laoChannelID),
	}

	laoCh.inbox.storeMessage(*publish.Params.Message)

	h.channelByID[encodedID] = &laoCh

	return nil
}
