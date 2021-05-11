package hub

import (
	"fmt"
	"log"
	"path/filepath"
	"sync"

	"student20_pop/message"

	"github.com/xeipuuv/gojsonschema"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
)

type baseHub struct {
	messageChan chan IncomingMessage

	sync.RWMutex
	channelByID map[string]Channel

	public kyber.Point

	schemas map[string]*gojsonschema.Schema
}

const (
	GenericMsgSchema string = "genericMsgSchema"
	DataSchema       string = "dataSchema"
)

// NewBaseHub returns a Base Hub.
func NewBaseHub(public kyber.Point) (*baseHub, error) {
	// Import the Json schemas defined in the protocol section
	protocolPath, err := filepath.Abs("../protocol")
	if err != nil {
		return nil, xerrors.Errorf("failed to load the path for the json schemas: %v", err)
	}
	protocolPath = "file://" + protocolPath

	schemas := make(map[string]*gojsonschema.Schema)

	// Import the schema for generic messages
	genericMsgLoader := gojsonschema.NewReferenceLoader(protocolPath + "/genericMessage.json")
	genericMsgSchema, err := gojsonschema.NewSchema(genericMsgLoader)
	if err != nil {
		return nil, xerrors.Errorf("failed to load the json schema for generic messages: %v", err)
	}
	schemas[GenericMsgSchema] = genericMsgSchema

	// Impot the schema for data
	dataSchemaLoader := gojsonschema.NewReferenceLoader(protocolPath + "/query/method/message/data/data.json")
	dataSchema, err := gojsonschema.NewSchema(dataSchemaLoader)
	if err != nil {
		return nil, xerrors.Errorf("failed to load the json schema for data: %v", err)
	}
	schemas[DataSchema] = dataSchema
	return &baseHub{
		messageChan: make(chan IncomingMessage),
		channelByID: make(map[string]Channel),
		public:      public,
	}, nil
}

// RemoveClient removes the client from this hub.
func (h *baseHub) RemoveClientSocket(client *ClientSocket) {
	h.RLock()
	defer h.RUnlock()

	for _, channel := range h.channelByID {
		channel.Unsubscribe(client, message.Unsubscribe{})
	}
}

// Recv accepts a message and enques it for processing in the hub.
func (h *baseHub) Recv(msg IncomingMessage) {
	log.Printf("Hub::Recv")
	h.messageChan <- msg
}

func start(h Hub, done chan struct{}, messageChan chan IncomingMessage) {
	for {
		select {
		case incomingMessage := <-messageChan:
			h.handleIncomingMessage(&incomingMessage)
		case <-done:
			return
		}
	}
}

func (b *baseHub) verifyJson(byteMessage []byte, schemaName string) error {
	// Validate the Json "byteMessage" with a schema
	messageLoader := gojsonschema.NewBytesLoader(byteMessage)
	resultErrors, err := b.schemas[schemaName].Validate(messageLoader)
	if err != nil {
		return &message.Error{
			Code:        -1,
			Description: err.Error(),
		}
	}
	errorsList := resultErrors.Errors()
	descriptionErrors := ""
	// Concatenate all error descriptions
	for index, e := range errorsList {
		descriptionErrors += fmt.Sprintf(" (%d) %s", index+1, e.Description())
	}

	if len(errorsList) > 0 {
		return &message.Error{
			Code:        -1,
			Description: descriptionErrors,
		}
	}

	return nil
}
