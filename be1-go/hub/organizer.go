package hub

import (
	"encoding/base64"
	"fmt"
	"log"
	"student20_pop/crypto"
	"student20_pop/message/answer"
	"student20_pop/message/messagedata"
	"student20_pop/message/query/method"
	"student20_pop/message/query/method/message"

	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
)

// organizerHub implements the Hub interface.
type organizerHub struct {
	*baseHub
}

// NewOrganizerHub returns a Organizer Hub.
func NewOrganizerHub(public kyber.Point, log zerolog.Logger) (*organizerHub, error) {
	baseHub, err := NewBaseHub(public, log)
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
func (c *laoChannel) Publish(publish method.Publish) error {
	err := c.baseChannel.VerifyPublishMessage(publish)
	if err != nil {
		return xerrors.Errorf("failed to verify publish message: %w", err)
	}

	msg := publish.Params.Message

	data := msg.Data

	jsonData, err := base64.URLEncoding.DecodeString(data)
	if err != nil {
		return xerrors.Errorf("failed to decode message data: %v", err)
	}

	object, action, err := messagedata.GetObjectAndAction(jsonData)

	switch object {
	case "lao":
		err = c.processLaoObject(action, msg)
	case "meeting":
		err = c.processMeetingObject(action, msg)
	case "message":
		err = c.processMessageObject(action, msg)
	case "roll_call":
		err = c.processRollCallObject(action, msg)
	case "election":
		err = c.processElectionObject(action, msg)
	}

	if err != nil {
		return xerrors.Errorf("failed to process %q object: %w", object, err)
	}

	c.broadcastToAllClients(msg)
	return nil
}

// processLaoObject processes a LAO object.
func (c *laoChannel) processLaoObject(action string, msg message.Message) error {
	switch action {
	case "update_properties":
	case "state":
		var laoState messagedata.LaoState

		err := msg.UnmarshalData(&laoState)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal lao#state: %v", err)
		}

		err = c.processLaoState(laoState)
		if err != nil {
			return xerrors.Errorf("failed to process state action: %w", err)
		}
	default:
		return answer.NewInvalidActionError(action)
	}

	c.inbox.storeMessage(msg)

	return nil
}

// processLaoState processes a lao state action.
func (c *laoChannel) processLaoState(data messagedata.LaoState) error {
	// Check if we have the update message
	msg, ok := c.inbox.getMessage(data.ModificationID)

	if !ok {
		return answer.NewErrorf(-4, "cannot find lao/update_properties with ID: %s", data.ModificationID)
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
			if c.witnesses[i] == data.ModificationSignatures[j].Witness {
				match++
				break
			}
		}
	}
	c.witnessMu.Unlock()

	if match != expected {
		return answer.NewErrorf(-4, "not enough witness signatures provided. Needed %d got %d", expected, match)
	}

	// Check if the signatures match
	for _, pair := range data.ModificationSignatures {
		err := schnorr.VerifyWithChecks(crypto.Suite, []byte(pair.Witness), []byte(data.ModificationID), []byte(pair.Signature))
		if err != nil {
			return answer.NewErrorf(-4, "signature verfication failed for witness: %s", pair.Witness)
		}
	}

	var updateMsgData messagedata.LaoUpdate

	err := msg.UnmarshalData(&updateMsgData)
	if err != nil {
		return &answer.Error{
			Code:        -4,
			Description: fmt.Sprintf("failed to unmarshal message from the inbox: %v", err),
		}
	}

	err = compareLaoUpdateAndState(updateMsgData, data)
	if err != nil {
		return xerrors.Errorf("failed to compare lao/update and existing state: %w", err)
	}

	return nil
}

func compareLaoUpdateAndState(update messagedata.LaoUpdate, state messagedata.LaoState) error {
	if update.LastModified != state.LastModified {
		return answer.NewErrorf(-4, "mismatch between last modified: expected %d got %d", update.LastModified, state.LastModified)
	}

	if update.Name != state.Name {
		return answer.NewErrorf(-4, "mismatch between name: expected %s got %s", update.Name, state.Name)
	}

	M := len(update.Witnesses)
	N := len(state.Witnesses)

	if M != N {
		return answer.NewErrorf(-4, "mismatch between witness count: expected %d got %d", M, N)
	}

	match := 0

	for i := 0; i < M; i++ {
		for j := 0; j < N; j++ {
			if update.Witnesses[i] == state.Witnesses[j] {
				match++
				break
			}
		}
	}

	if match != M {
		return answer.NewErrorf(-4, "mismatch between witness keys: expected %d keys to match but %d matched", M, match)
	}

	return nil
}

// processMeetingObject handles a meeting object.
func (c *laoChannel) processMeetingObject(action string, msg message.Message) error {

	// Nothing to do ...🤷‍♂️
	switch action {
	case "create":
	case "update_properties":
	case "state":
	}

	c.inbox.storeMessage(msg)

	return nil
}

// processMessageObject handles a message object.
func (c *laoChannel) processMessageObject(action string, msg message.Message) error {

	switch action {
	case "witness":
		var witnessData messagedata.MessageWitness

		err := msg.UnmarshalData(&witnessData)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal witness data: %v", err)
		}

		err = schnorr.VerifyWithChecks(crypto.Suite, []byte(msg.Sender), []byte(witnessData.MessageID), []byte(witnessData.Signature))
		if err != nil {
			return answer.NewError(-4, "invalid witness signature")
		}

		err = c.inbox.addWitnessSignature(witnessData.MessageID, msg.Sender, witnessData.Signature)
		if err != nil {
			return xerrors.Errorf("failed to add witness signature: %w", err)
		}
	default:
		return answer.NewInvalidActionError(action)
	}

	return nil
}

// processRollCallObject handles a roll call object.
func (c *laoChannel) processRollCallObject(action string, msg message.Message) error {
	sender := msg.Sender

	senderBuf, err := base64.URLEncoding.DecodeString(sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	// Check if the sender of the roll call message is the organizer
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewErrorf(-4, "failed to unmarshal public key of the sender: %v", err)
	}

	if !c.hub.public.Equal(senderPoint) {
		return answer.NewErrorf(-5, "sender's public key %q does not match the organizer's", msg.Sender)
	}

	switch action {
	case "create":
		var rollCallCreate messagedata.RollCallCreate

		err := msg.UnmarshalData(&rollCallCreate)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal roll call create: %v", err)
		}

		c.hub.log.Info().Msgf("processing a roll_call create: %v", rollCallCreate)

		err = c.processCreateRollCall(rollCallCreate)
		if err != nil {
			return xerrors.Errorf("failed to process roll call create: %v", err)
		}

	case "open", "reopen":
		err := c.processOpenRollCall(msg, action)
		if err != nil {
			return xerrors.Errorf("failed to process open roll call: %v", err)
		}

	case "close":
		var rollCallClose messagedata.RollCallClose

		err := msg.UnmarshalData(&rollCallClose)
		if err != nil {
			return xerrors.Errorf("failed to unmarshal roll call close: %v", err)
		}

		err = c.processCloseRollCall(rollCallClose)
		if err != nil {
			return xerrors.Errorf("failed to process close roll call: %v", err)
		}
	default:
		return answer.NewInvalidActionError(action)
	}

	if err != nil {
		return xerrors.Errorf("failed to process roll call action: %s %w", action, err)
	}

	c.inbox.storeMessage(msg)

	return nil
}

// processElectionObject handles an election object.
func (c *laoChannel) processElectionObject(action string, msg message.Message) error {
	if action != "setup" {
		return answer.NewErrorf(-4, "invalid action: %s", action)
	}

	sender := msg.Sender

	senderBuf, err := base64.URLEncoding.DecodeString(sender)
	if err != nil {
		return xerrors.Errorf("failed to decode sender key: %v", err)
	}

	// Check if the sender of election creation message is the organizer
	senderPoint := crypto.Suite.Point()
	err = senderPoint.UnmarshalBinary(senderBuf)
	if err != nil {
		return answer.NewErrorf(-4, "failed to unmarshal public key of the sender: %v", err)
	}

	if !c.hub.public.Equal(senderPoint) {
		return answer.NewError(-5, "The sender of the election setup message has a different public key from the organizer")
	}

	var electionSetup messagedata.ElectionSetup

	err = msg.UnmarshalData(&electionSetup)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal election setup: %v", err)
	}

	err = c.createElection(msg, electionSetup)
	if err != nil {
		return xerrors.Errorf("failed to create election: %w", err)
	}

	log.Printf("Election has created with success")
	return nil
}
