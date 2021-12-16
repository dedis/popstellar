package registry

import (
	"popstellar/message/query/method/message"
	"reflect"

	"golang.org/x/xerrors"
)

type MessageRegistry struct {
	registry map[string]CallbackData
}

type CallbackData struct {
	Callback     func(message.Message, interface{}) error
	ConcreteType interface{}
}

func NewMessageRegistry() MessageRegistry {
	return MessageRegistry{
		registry: make(map[string]CallbackData),
	}
}

// register registers a new action that will be associated to a callback
// function. c defines the concrete type the callback function will receive. c
// must NOT be a pointer. The callback function will have to cast the interface
// it receives to the concrete type. For example:
//
//   // define a callback
//   func execElect(msg interface) error {
//	   m, ok := msg.(*messagedata.ConsensusElect)
//     if !ok {...}
//     // ...
//   }
//
//   // register the "elect" type
//   registry.register("elect", execElect, messagedata.ConsensusElect{})
//
//   // when we need to process a message we call "processMsg"
//   err = registry.processMsg(msg)
//
func (m MessageRegistry) Register(key string, f func(message.Message, interface{}) error, c interface{}) {
	m.registry[key] = CallbackData{
		Callback:     f,
		ConcreteType: c,
	}
}

func (m MessageRegistry) Process(key string, msg message.Message) error {

	data, found := m.registry[key]
	if !found {
		return xerrors.Errorf("action '%s' not found", key)
	}

	concreteType := reflect.New(reflect.ValueOf(data.ConcreteType).Type()).Interface()

	err := msg.UnmarshalData(&concreteType)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal data: %v", err)
	}

	err = data.Callback(msg, concreteType)
	if err != nil {
		return xerrors.Errorf("failed to process action '%s': %v", key, err)
	}

	return nil
}
