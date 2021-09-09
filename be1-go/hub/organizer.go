package hub

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"log"
	"student20_pop/crypto"

	"student20_pop/message"

	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

// organizerHub implements the Hub interface.
type organizerHub struct {
	*baseHub
}

// NewOrganizerHub returns a Organizer Hub.
func NewOrganizerHub(public kyber.Point) (*organizerHub, error) {
	baseHub, err := NewBaseHub(public)
	return &organizerHub{
		baseHub: baseHub,
	}, err
}

func (o *organizerHub) Type() HubType {
	return OrganizerHubType
}

// laoChannel implements a channel. It is used to handle messages
// on the LAO root channel.
type laoChannel struct {
	rollCall  rollCall
	attendees *Attendees
	*baseChannel
}

// rollCallState denotes the state of the roll call.
type rollCallState string

const (
	// Open represents the open roll call state.
	Open rollCallState = "open"

	// Closed represents the closed roll call state.
	Closed rollCallState = "closed"

	// Created represents the created roll call state.
	Created rollCallState = "created"
)

// rollCall represents a roll call.
type rollCall struct {
	state rollCallState
	id    string
}

// Publish handles publish messages for the LAO channel.
func (c *laoChannel) Publish(publish message.Publish) error {
	err := c.baseChannel.VerifyPublishMessage(publish)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message: %w", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	object := data.GetObject()

	switch object {
	case message.LaoObject:
		err = c.processLaoObject(*msg)
	case message.MeetingObject:
		err = c.processMeetingObject(data, *msg)
	case message.MessageObject:
		err = c.processMessageObject(msg.Sender, data)
	case message.RollCallObject:
		err = c.processRollCallObject(*msg)
	case message.ElectionObject:
		err = c.processElectionObject(*msg)
	}

	if err != nil {
		return xerrors.Errorf("failed to process %q object: %w", object, err)
	}

	c.broadcastToAllClients(*msg)
	return nil
}

// processLaoObject processes a LAO object.
func (c *laoChannel) processLaoObject(msg message.Message) error {
	action := message.LaoDataAction(msg.Data.GetAction())

	switch action {
	case message.UpdateLaoAction:
	case message.StateLaoAction:
		err := c.processLaoState(msg.Data.(*message.StateLAOData))
		if err != nil {
			return xerrors.Errorf("failed to process %q action: %w", message.StateLaoAction, err)
		}
	default:
		return message.NewInvalidActionError(message.DataAction(action))
	}

	c.inbox.storeMessage(msg)

	return nil
}

// processLaoState processes a lao state action.
func (c *laoChannel) processLaoState(data *message.StateLAOData) error {
	// Check if we have the update message
	msg, ok := c.inbox.getMessage(data.ModificationID)

	updateMsgIDEncoded := base64.URLEncoding.EncodeToString(data.ModificationID)

	if !ok {
		return message.NewErrorf(-4, "cannot find lao/update_properties with ID: %s", updateMsgIDEncoded)
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
		return message.NewErrorf(-4, "not enough witness signatures provided. Needed %d got %d", expected, match)
	}

	// Check if the signatures match
	for _, pair := range data.ModificationSignatures {
		err := schnorr.VerifyWithChecks(crypto.Suite, pair.Witness, data.ModificationID, pair.Signature)
		if err != nil {
			pk := base64.URLEncoding.EncodeToString(pair.Witness)
			return message.NewErrorf(-4, "signature verfication failed for witness: %s", pk)
		}
	}

	// Check if the updates are consistent with the update message
	updateMsgData, ok := msg.Data.(*message.UpdateLAOData)
	if !ok {
		return &message.Error{
			Code:        -4,
			Description: fmt.Sprintf("modification id %s refers to a message which is not lao/update_properties", updateMsgIDEncoded),
		}
	}

	err := compareLaoUpdateAndState(updateMsgData, data)
	if err != nil {
		return xerrors.Errorf("failed to compare lao/update and existing state: %w", err)
	}

	return nil
}

func compareLaoUpdateAndState(update *message.UpdateLAOData, state *message.StateLAOData) error {
	if update.LastModified != state.LastModified {
		return message.NewErrorf(-4, "mismatch between last modified: expected %d got %d", update.LastModified, state.LastModified)
	}

	if update.Name != state.Name {
		return message.NewErrorf(-4, "mismatch between name: expected %s got %s", update.Name, state.Name)
	}

	M := len(update.Witnesses)
	N := len(state.Witnesses)

	if M != N {
		return message.NewErrorf(-4, "mismatch between witness count: expected %d got %d", M, N)
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
		return message.NewErrorf(-4, "mismatch between witness keys: expected %d keys to match but %d matched", M, match)
	}

	return nil
}

// processMeetingObject handles a meeting object.
func (c *laoChannel) processMeetingObject(data message.Data, msg message.Message) error {
	action := message.MeetingDataAction(data.GetAction())

	switch action {
	case message.CreateMeetingAction:
	case message.UpdateMeetingAction:
	case message.StateMeetingAction:
	}

	c.inbox.storeMessage(msg)

	return nil
}

// processMessageObject handles a message object.
func (c *laoChannel) processMessageObject(public message.PublicKey, data message.Data) error {
	action := message.MessageDataAction(data.GetAction())

	switch action {
	case message.WitnessAction:
		witnessData := data.(*message.WitnessMessageData)

		err := schnorr.VerifyWithChecks(crypto.Suite, public, witnessData.MessageID, witnessData.Signature)
		if err != nil {
			return message.NewError(-4, "invalid witness signature")
		}

		err = c.inbox.addWitnessSignature(witnessData.MessageID, public, witnessData.Signature)
		if err != nil {
			return xerrors.Errorf("failed to add witness signature: %w", err)
		}
	default:
		return message.NewInvalidActionError(message.DataAction(action))
	}

	return nil
}

// processRollCallObject handles a roll call object.
func (c *laoChannel) processRollCallObject(msg message.Message) error {
	sender := msg.Sender
	data := msg.Data

	// Check if the sender of the roll call message is the organizer
	senderPoint := crypto.Suite.Point()
	err := senderPoint.UnmarshalBinary(sender)
	if err != nil {
		return message.NewErrorf(-4, "failed to unmarshal public key of the sender: %v", err)
	}

	if !c.hub.public.Equal(senderPoint) {
		return message.NewErrorf(-5, "sender's public key %q does not match the organizer's", msg.Sender.String())
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
		return xerrors.Errorf("failed to process roll call action: %s %w", action, err)
	}

	c.inbox.storeMessage(msg)

	return nil
}

// processElectionObject handles an election object.
func (c *laoChannel) processElectionObject(msg message.Message) error {
	action := message.ElectionAction(msg.Data.GetAction())

	if action != message.ElectionSetupAction {
		return message.NewErrorf(-4, "invalid action: %s", action)
	}

	sender := msg.Sender

	// Check if the sender of election creation message is the organizer
	senderPoint := crypto.Suite.Point()
	err := senderPoint.UnmarshalBinary(sender)
	if err != nil {
		return message.NewErrorf(-4, "failed to unmarshal public key of the sender: %v", err)
	}

	if !c.hub.public.Equal(senderPoint) {
		return message.NewError(-5, "The sender of the election setup message has a different public key from the organizer")
	}

	err = c.createElection(msg)
	if err != nil {
		return xerrors.Errorf("failed to create election: %w", err)
	}

	log.Printf("Election has created with success")
	return nil
}
