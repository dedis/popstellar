package hub

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"log"
	"student20_pop"

	"student20_pop/message"

	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
)

type organizerHub struct {
	*baseHub
}

// NewOrganizerHub returns a Organizer Hub.
func NewOrganizerHub(public kyber.Point) (Hub, error) {
	baseHub, err := NewBaseHub(public)
	return &organizerHub{
		baseHub,
	}, err
}

type laoChannel struct {
	rollCall  rollCall
	attendees map[string]struct{}
	*baseChannel
}

type rollCallState string

const (
	Open    rollCallState = "open"
	Closed  rollCallState = "closed"
	Created rollCallState = "created"
)

type rollCall struct {
	state rollCallState
	id    string
}

func (c *laoChannel) Publish(publish message.Publish) error {
	err := c.baseChannel.VerifyPublishMessage(publish)
	if err != nil {
		return message.NewError("failed to verify Publish message on a lao channel", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	object := data.GetObject()

	switch object {
	case message.LaoObject:
		err = c.processLaoObject(*msg)
	case message.MeetingObject:
		err = c.processMeetingObject(data)
	case message.MessageObject:
		err = c.processMessageObject(msg.Sender, data)
	case message.RollCallObject:
		err = c.processRollCallObject(*msg)
	}

	if err != nil {
		errorDescription := fmt.Sprintf("failed to process %s object", object)
		return message.NewError(errorDescription, err)
	}

	c.broadcastToAllClients(*msg)
	return nil
}

func (c *laoChannel) processLaoObject(msg message.Message) error {
	action := message.LaoDataAction(msg.Data.GetAction())
	msgIDEncoded := base64.StdEncoding.EncodeToString(msg.MessageID)

	switch action {
	case message.UpdateLaoAction:
	case message.StateLaoAction:
		err := c.processLaoState(msg.Data.(*message.StateLAOData))
		if err != nil {
			return message.NewError("failed to process lao/state", err)
		}
	default:
		return message.NewInvalidActionError(message.DataAction(action))
	}

	c.inboxMu.Lock()
	c.inbox[msgIDEncoded] = msg
	c.inboxMu.Unlock()

	return nil
}

func (c *laoChannel) processLaoState(data *message.StateLAOData) error {
	// Check if we have the update message
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
		return message.NewError("failure while comparing lao/update and lao/state", err)
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
		msg, ok := c.inbox[msgEncoded]
		if !ok {
			// TODO: We received a witness signature before the message itself.
			// We ignore it for now but it might be worth keeping it until we
			// actually receive the message
			log.Printf("failed to find message_id %s for witness message", msgEncoded)
			c.inboxMu.Unlock()
			return nil
		}
		msg.WitnessSignatures = append(msg.WitnessSignatures, message.PublicKeySignaturePair{
			Witness:   public,
			Signature: witnessData.Signature,
		})
		c.inboxMu.Unlock()
	default:
		return message.NewInvalidActionError(message.DataAction(action))
	}

	return nil
}

func (c *laoChannel) processRollCallObject(msg message.Message) error {
	sender := msg.Sender
	data := msg.Data

	// Check if the sender of the roll call message is the organizer
	senderPoint := student20_pop.Suite.Point()
	err := senderPoint.UnmarshalBinary(sender)
	if err != nil {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to unmarshal public key of the sender: %v", err),
		}
	}

	if !c.hub.public.Equal(senderPoint) {
		return &message.Error{
			Code:        -5,
			Description: "The sender of the roll call message has a different public key from the organizer",
		}
	}

	action := message.RollCallAction(data.GetAction())

	switch action {
	case message.CreateRollCallAction:
		err = c.processCreateRollCall(data)
	case message.RollCallAction(message.OpenRollCallAction), message.RollCallAction(message.ReopenRollCallAction):
		err = c.processOpenRollCall(data, action)
	case message.CloseRollCallAction:
		err = c.processCloseRollCall(data)
	default:
		return message.NewInvalidActionError(message.DataAction(action))
	}

	if err != nil {
		errorDescription := fmt.Sprintf("failed to process %v roll-call action", action)
		return message.NewError(errorDescription, err)
	}

	msgIDEncoded := base64.StdEncoding.EncodeToString(msg.MessageID)
	c.inboxMu.Lock()
	c.inbox[msgIDEncoded] = msg
	c.inboxMu.Unlock()

	return nil
}

func (c *laoChannel) processCreateRollCall(data message.Data) error {
	rollCallData := data.(*message.CreateRollCallData)
	if !c.checkRollCallID(rollCallData.Creation, message.Stringer(rollCallData.Name), rollCallData.ID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||creation||name)",
		}
	}

	c.rollCall.id = string(rollCallData.ID)
	c.rollCall.state = Created
	return nil
}

func (c *laoChannel) processOpenRollCall(data message.Data, action message.RollCallAction) error {
	if action == message.RollCallAction(message.OpenRollCallAction) {
		// If the action is an OpenRollCallAction,
		// the previous roll call action should be a CreateRollCallAction
		if c.rollCall.state != Created {
			return &message.Error{
				Code:        -1,
				Description: "The roll call can not be opened since it has not been created previously",
			}
		}
	} else {
		// If the action is an RepenRollCallAction,
		// the previous roll call action should be a CloseRollCallAction
		if c.rollCall.state != Closed {
			return &message.Error{
				Code:        -1,
				Description: "The roll call can not be reopened since it has not been closed previously",
			}
		}
	}

	rollCallData := data.(*message.OpenRollCallData)

	if !c.rollCall.checkPrevID(rollCallData.Opens) {
		return &message.Error{
			Code:        -4,
			Description: "The field `opens` does not correspond to the id of the previous roll call message",
		}
	}

	opens := base64.StdEncoding.EncodeToString(rollCallData.Opens)
	if !c.checkRollCallID(message.Stringer(opens), rollCallData.OpenedAt, rollCallData.UpdateID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||opens||opened_at)",
		}
	}

	c.rollCall.id = string(rollCallData.UpdateID)
	c.rollCall.state = Open
	return nil
}

func (c *laoChannel) processCloseRollCall(data message.Data) error {
	if c.rollCall.state != Open {
		return &message.Error{
			Code:        -1,
			Description: "The roll call can not be closed since it is not open",
		}
	}

	rollCallData := data.(*message.CloseRollCallData)
	if !c.rollCall.checkPrevID(rollCallData.Closes) {
		return &message.Error{
			Code:        -4,
			Description: "The field `closes` does not correspond to the id of the previous roll call message",
		}
	}

	closes := base64.StdEncoding.EncodeToString(rollCallData.Closes)
	if !c.checkRollCallID(message.Stringer(closes), rollCallData.ClosedAt, rollCallData.UpdateID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||closes||closed_at)",
		}
	}

	c.rollCall.id = string(rollCallData.UpdateID)
	c.rollCall.state = Closed
	c.attendees = map[string]struct{}{}
	for i := 0; i < len(rollCallData.Attendees); i += 1 {
		c.attendees[string(rollCallData.Attendees[i])] = struct{}{}
	}

	return nil
}

func (r *rollCall) checkPrevID(prevID []byte) bool {
	return string(prevID) == r.id
}

// Check if the id of the roll call corresponds to the hash of the correct parameters
// Return true if the hash corresponds to the id and false otherwise
func (c *laoChannel) checkRollCallID(str1, str2 fmt.Stringer, id []byte) bool {
	laoID := c.channelID[6:]
	hash, err := message.Hash(message.Stringer('R'), message.Stringer(laoID), str1, str2)
	if err != nil {
		return false
	}

	return bytes.Equal(hash, id)
}
