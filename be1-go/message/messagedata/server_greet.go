package messagedata

// ServerGreet defines a message data
type ServerGreet struct {
	Object        string `json:"object"`
	Action        string `json:"action"`
	PublicKey     string `json:"public_key"`
	ClientAddress string `json:"client_address"`
	ServerAddress string `json:"server_address"`
	Peers         []Peer `json:"peers"`
}

// GetObject implements MessageData
func (ServerGreet) GetObject() string {
	return ServerObject
}

// GetAction implements MessageData
func (ServerGreet) GetAction() string {
	return ServerActionGreet
}

// NewEmpty implements MessageData
func (ServerGreet) NewEmpty() MessageData {
	return &ServerGreet{}
}
