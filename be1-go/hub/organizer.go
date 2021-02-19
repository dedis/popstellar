package hub

import (
	"bytes"
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
		log.Printf("cannot handle broadcasts right now")
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

	encodedID := base64.StdEncoding.EncodeToString(data.ID)

	laoCh := laoChannel{
		channel: "/root/" + encodedID,
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

	// /root/<base64ID>
	channel string

	witnessMu sync.Mutex
	witnesses []message.PublicKey
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
		err = c.processLaoObject(*msg)
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

	c.broadcastToAllClients(*msg)
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

func (c *laoChannel) processLaoObject(msg message.Message) error {
	action := message.LaoDataAction(msg.Data.GetAction())
	msgIDEncoded := base64.StdEncoding.EncodeToString(msg.MessageID)

	switch action {
	case message.UpdateLaoAction:
		c.inboxMu.Lock()
		c.inbox[msgIDEncoded] = msg
		c.inboxMu.Unlock()
	case message.StateLaoAction:
		err := c.processLaoState(&msg)
		if err != nil {
			log.Printf("failed to process lao/state: %v", err)
			return xerrors.Errorf("failed to process lao/state: %v", err)
		}
	default:
		return &message.Error{
			Code:        -1,
			Description: fmt.Sprintf("invalid action: %s", action),
		}
	}

	return nil
}

func (c *laoChannel) processLaoState(msg *message.Message) error {
	// Check if we have the update message
	data := msg.Data.(*message.StateLAOData)
	updateMsgIDEncoded := base64.StdEncoding.EncodeToString(data.ModificationID)

	c.inboxMu.RLock()
	updateMsg, ok := c.inbox[updateMsgIDEncoded]
	c.inboxMu.RUnlock()

	if !ok {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("cannot find lao/update_properties with ID: %s", updateMsgIDEncoded),
		}
	}

	// Check if the signatures are from witnesses we need. We maintain
	// the current state of witnesses for a LAO in the channel instance
	// TODO: threshold signature verification

	c.witnessMu.Lock()
	match := 0
	expected := len(c.witnesses)
	// TODO: O(N^2), O(N) possible
	for i := 0; i < expected; i++ {
		for j := 0; j < len(data.ModificationSignatures); j++ {
			if bytes.Equal(c.witnesses[i], data.ModificationSignatures[j].Witness) {
				match++
				break
			}
		}
	}
	c.witnessMu.Unlock()

	if match != expected {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("not enough witness signatures provided. Needed %d got %d", expected, match),
		}
	}

	// Check if the signatures match
	for _, pair := range data.ModificationSignatures {
		err := schnorr.VerifyWithChecks(student20_pop.Suite, pair.Witness, data.ModificationID, pair.Signature)
		if err != nil {
			pk := base64.StdEncoding.EncodeToString(pair.Witness)
			return &message.Error{
				Code:        -4,
				Description: fmt.Sprintf("signature verification failed for witness %s", pk),
			}
		}
	}

	// Check if the updates are consistent with the update message
	updateMsgData, ok := updateMsg.Data.(*message.UpdateLAOData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("modification id %s refers to a message which is not lao/update_properties", updateMsgIDEncoded),
		}
	}

	err := compareLaoUpdateAndState(updateMsgData, data)
	if err != nil {
		return xerrors.Errorf("failure while comparing lao/update and lao/state")
	}

	return nil
}

func compareLaoUpdateAndState(update *message.UpdateLAOData, state *message.StateLAOData) error {
	if update.LastModified != state.LastModified {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between last modified: expected %d got %d", update.LastModified, state.LastModified),
		}
	}

	if update.Name != state.Name {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between name: expected %d got %d", update.LastModified, state.LastModified),
		}
	}

	if update.Name != state.Name {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between name: expected %d got %d", update.LastModified, state.LastModified),
		}
	}

	M := len(update.Witnesses)
	N := len(state.Witnesses)

	if M != N {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between witness count: expected %d got %d", M, N),
		}
	}

	match := 0

	for i := 0; i < M; i++ {
		for j := 0; j < N; j++ {
			if bytes.Equal(update.Witnesses[i], state.Witnesses[j]) {
				match++
				break
			}
		}
	}

	if match != M {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("mismatch between witness keys: expected %d keys to match but %d matched", M, match),
		}
	}

	return nil
}

func (c *laoChannel) broadcastToAllClients(msg message.Message) {
	c.clientsMu.RLock()
	defer c.clientsMu.RUnlock()

	query := message.Query{
		Broadcast: message.NewBroadcast(c.channel, &msg),
	}

	buf, err := json.Marshal(query)
	if err != nil {
		log.Fatalf("failed to marshal broadcast query: %v", err)
	}

	for client := range c.clients {
		client.Send(buf)
	}
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
