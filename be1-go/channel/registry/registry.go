package registry

import (
	"encoding/base64"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"

	"golang.org/x/xerrors"
)

// MessageRegistry defines a standard message registry
type MessageRegistry struct {
	registry map[string]CallbackData
}

// CallbackData is the data needed to execute a callback
type CallbackData struct {
	Callback     func(message.Message, interface{}, socket.Socket) error
	ConcreteType messagedata.MessageData
}

// NewMessageRegistry returns a new initialized registry
func NewMessageRegistry() MessageRegistry {
	return MessageRegistry{
		registry: make(map[string]CallbackData),
	}
}

// Register registers a new action that will be associated to a callback
// function. The callback function will have to cast the interface it receives
// to the concrete type. For example:
//
//	// define a callback
//	func execElect(msg interface) error {
//	  m, ok := msg.(messagedata.ConsensusElect)
//	  if !ok {...}
//	  // ...
//	}
//
//	// register the "elect" type
//	registry.register(messagedata.ConsensusElect{}, execElect)
//
//	// when we need to process a message we call "processMsg"
//	err = registry.processMsg(msg)
func (m MessageRegistry) Register(msg messagedata.MessageData, f func(message.Message,
	interface{}, socket.Socket) error) {

	m.registry[msg.GetObject()+"#"+msg.GetAction()] = CallbackData{
		Callback:     f,
		ConcreteType: msg,
	}
}

// Process executes the callback associated to the message data
func (m MessageRegistry) Process(msg message.Message, socket socket.Socket) error {
	data := msg.Data

	jsonData, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)
	if err != nil {
		return xerrors.Errorf("failed to get object or action: %v", err)
	}

	key := object + "#" + action

	callbackData, found := m.registry[key]
	if !found {
		return xerrors.Errorf("action '%s' not found", key)
	}

	concreteType := callbackData.ConcreteType.NewEmpty()

	err = msg.UnmarshalData(&concreteType)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal data: %v", err)
	}

	err = callbackData.Callback(msg, concreteType, socket)
	if err != nil {
		return xerrors.Errorf("failed to process action '%s': %v", key, err)
	}

	return nil
}
