package hub

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"student20_pop"
	"sync"

	"student20_pop/message"

	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

type organizerHub struct {
	messageChan chan IncomingMessage

	sync.RWMutex
	channelByID map[string]Channel

	public kyber.Point
}

func NewOrganizerHub(public kyber.Point) *organizerHub {
	return &organizerHub{
		messageChan: make(chan IncomingMessage),
		channelByID: make(map[string]Channel),
		public:      public,
	}
}

func (o *organizerHub) RemoveClient(client *Client) {
	o.RLock()
	defer o.RUnlock()

	for _, channel := range o.channelByID {
		channel.Unsubscribe(client, message.Unsubscribe{})
	}
}

func (o *organizerHub) Recv(msg IncomingMessage) {
	log.Printf("organizerHub::Recv")
	o.messageChan <- msg
}

func (o *organizerHub) handleIncomingMessage(incomingMessage *IncomingMessage) {
	log.Printf("organizerHub::handleIncomingMessage: %s", incomingMessage.Message)

	client := incomingMessage.Client

	// unmarshal the message
	genericMsg := &message.GenericMessage{}
	err := json.Unmarshal(incomingMessage.Message, genericMsg)
	if err != nil {
		log.Printf("failed to unmarshal incoming message: %v", err)
	}

	query := genericMsg.Query

	if query == nil {
		return
	}

	err = query.Verify(o.public)
	if err != nil {
		log.Printf("failed to verify signature on message: %v", err)
		client.SendError(query.GetID(), err)
	}

	channelID := query.GetChannel()
	log.Printf("channel: %s", channelID)

	id := query.GetID()

	if channelID == "/root" {
		if query.Publish != nil &&
			query.Publish.Params.Message.Data.GetAction() == message.DataAction(message.CreateLaoAction) &&
			query.Publish.Params.Message.Data.GetObject() == message.DataObject(message.LaoObject) {
			err := o.createLao(*query.Publish)
			if err != nil {
				log.Printf("failed to create lao: %v", err)
				client.SendError(query.Publish.ID, err)
			}
		} else {
			log.Printf("invalid method: %s", query.GetMethod())
			client.SendError(id, &message.Error{
				Code:        -1,
				Description: "you may only invoke lao/create on /root",
			})
		}

		return
	}

	if channelID[:6] != "/root/" {
		log.Printf("channel id must begin with /root/")
		client.SendError(id, &message.Error{
			Code:        -2,
			Description: "channel id must begin with /root/",
		})
		return
	}

	channelID = channelID[6:]
	o.RLock()
	channel, ok := o.channelByID[channelID]
	if !ok {
		log.Printf("invalid channel id: %s", channelID)
		client.SendError(id, &message.Error{
			Code:        -2,
			Description: fmt.Sprintf("channel with id %s does not exist", channelID),
		})
		return
	}
	o.RUnlock()

	method := query.GetMethod()
	log.Printf("method: %s", method)

	msg := []message.Message{}

	// TODO: use constants
	switch method {
	case "subscribe":
		err = channel.Subscribe(client, *query.Subscribe)
	case "unsubscribe":
		err = channel.Unsubscribe(client, *query.Unsubscribe)
	case "publish":
		err = channel.Publish(*query.Publish)
	case "message":
		//err = channel.Broadcast(client, *query.Broadcast)
		log.Printf("received a broadcast? Something's wrong")
	case "catchup":
		msg = channel.Catchup(*query.Catchup)
		// TODO send catchup response to client
	}

	if err != nil {
		log.Printf("failed to process query: %v", err)
		client.SendError(id, err)
		return
	}

	result := message.Result{}

	if method == "catchup" {
		result.Catchup = msg
	} else {
		general := 0
		result.General = &general
	}

	client.SendResult(id, result)
}

func (o *organizerHub) Start(done chan struct{}) {
	log.Printf("started organizer hub...")

	for {
		select {
		case incomingMessage := <-o.messageChan:
			o.handleIncomingMessage(&incomingMessage)
		case <-done:
			return
		}
	}
}

func (o *organizerHub) createLao(publish message.Publish) error {
	o.Lock()
	defer o.Unlock()

	data, ok := publish.Params.Message.Data.(*message.CreateLAOData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: "failed to cast data to CreateLAOData",
		}
	}

	laoCh := laoChannel{
		clients: make(map[*Client]struct{}),
		inbox:   make(map[string]message.Message),
	}

	messageID := base64.StdEncoding.EncodeToString(publish.Params.Message.MessageID)
	laoCh.inbox[messageID] = *publish.Params.Message

	id := base64.StdEncoding.EncodeToString(data.ID)
	o.channelByID[id] = &laoCh

	return nil
}

type laoChannel struct {
	clientsMu sync.RWMutex
	clients   map[*Client]struct{}

	inboxMu sync.RWMutex
	inbox   map[string]message.Message
}

func (c *laoChannel) Subscribe(client *Client, msg message.Subscribe) error {
	log.Printf("received a subscribe with id: %d", msg.ID)
	c.clientsMu.Lock()
	defer c.clientsMu.Unlock()

	c.clients[client] = struct{}{}

	return nil
}

func (c *laoChannel) Unsubscribe(client *Client, msg message.Unsubscribe) error {
	log.Printf("received an unsubscribe with id: %d", msg.ID)

	c.clientsMu.Lock()
	defer c.clientsMu.Unlock()

	if _, ok := c.clients[client]; !ok {
		return &message.Error{
			Code:        -2,
			Description: "client is not subscribed to this channel",
		}
	}

	delete(c.clients, client)
	return nil
}

func (c *laoChannel) Publish(publish message.Publish) error {
	log.Printf("received a publish with id: %d", publish.ID)

	msg := publish.Params.Message
	data := msg.Data

	msgIDEncoded := base64.StdEncoding.EncodeToString(msg.MessageID)

	c.inboxMu.RLock()
	if _, ok := c.inbox[msgIDEncoded]; ok {
		c.inboxMu.RUnlock()
		return &message.Error{
			Code:        -3,
			Description: "message already exists",
		}
	}
	c.inboxMu.RUnlock()

	object := data.GetObject()
	var err error

	switch object {
	case message.LaoObject:
		err = c.processLaoObject(data)
	case message.MeetingObject:
		err = c.processMeetingObject(data)
	case message.MessageObject:
		err = c.processMessageObject(msg.Sender, data)
	case message.RollCallObject:
		err = c.processRollCallObject(data)
	}

	if err != nil {
		log.Printf("failed to process %s object: %v", object, err)
		return xerrors.Errorf("failed to process %s object: %v", object, err)
	}

	return nil
}

func (c *laoChannel) Catchup(catchup message.Catchup) []message.Message {
	log.Printf("received a catchup with id: %d", catchup.ID)

	c.inboxMu.RLock()
	defer c.inboxMu.RUnlock()

	result := make([]message.Message, len(c.inbox))
	for _, msg := range c.inbox {
		result = append(result, msg)
	}

	// TODO: define order for Roll call
	//sort.Slice(result, func(i, j int) bool {
	//return result[i].Data.GetTimestamp() < result[j].GetTimestamp()
	//})

	return result
}

func (c *laoChannel) processLaoObject(data message.Data) error {
	action := message.LaoDataAction(data.GetAction())

	switch action {
	case message.UpdateLaoAction:
	case message.StateLaoAction:
	}
	return nil
}

func (c *laoChannel) processMeetingObject(data message.Data) error {
	action := message.MeetingDataAction(data.GetAction())

	switch action {
	case message.CreateMeetingAction:
	case message.UpdateMeetingAction:
	case message.StateMeetingAction:
	}

	return nil
}

func (c *laoChannel) processMessageObject(public message.PublicKey, data message.Data) error {
	action := message.MessageDataAction(data.GetAction())

	switch action {
	case message.WitnessAction:
		witnessData := data.(*message.WitnessMessageData)

		msgEncoded := base64.StdEncoding.EncodeToString(witnessData.MessageID)

		err := schnorr.VerifyWithChecks(student20_pop.Suite, public, witnessData.MessageID, witnessData.Signature)
		if err != nil {
			return &message.Error{
				Code:        -4,
				Description: "invalid witness signature",
			}
		}

		c.inboxMu.Lock()
		msg := c.inbox[msgEncoded]
		msg.WitnessSignatures = append(msg.WitnessSignatures, message.PublicKeySignaturePair{
			Witness:   public,
			Signature: witnessData.Signature,
		})
		c.inboxMu.Unlock()
	default:
		return &message.Error{
			Code:        -1,
			Description: fmt.Sprintf("invalid action: %s", action),
		}
	}

	return nil
}

func (c *laoChannel) processRollCallObject(data message.Data) error {
	action := message.RollCallAction(data.GetAction())

	switch action {
	case message.CreateRollCallAction:
	case message.RollCallAction(message.OpenRollCallAction):
	case message.RollCallAction(message.ReopenRollCallAction):
	case message.CloseRollCallAction:
	}

	return nil
}
