package consensus

type messageRegistry struct {
	registry map[string]callbackData
}

type callbackData struct {
	callback     func(interface{}) error
	concreteType interface{}
}

// register registers a new action that will be associated to a callback
// function. c defines the concrete type the callback function will receive. c
// must NOT be a pointer. The callback function will have to cast the interface
// it receives to the concrete type. For example:
//
//   // define a callback
//   func execElect(msg interface) error {
//	   m, ok := msg.(messagedata.ConsensusElect)
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
func (m messageRegistry) register(action string, f func(interface{}) error, c interface{}) {
	m.registry[action] = callbackData{
		callback:     f,
		concreteType: c,
	}
}
