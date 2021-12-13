package register

import "popstellar/message/query/method/message"

type MessageRegistry struct {
	Registry map[string]CallbackData
}

type CallbackData struct {
	Callback     func(message.Message, interface{}) error
	ConcreteType interface{}
}

func NewMessageRegistry() MessageRegistry {
	return MessageRegistry{
		Registry: make(map[string]CallbackData),
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
func (m MessageRegistry) Register(action string, f func(message.Message, interface{}) error, c interface{}) {
	m.Registry[action] = CallbackData{
		Callback:     f,
		ConcreteType: c,
	}
}
