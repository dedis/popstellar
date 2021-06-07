package hub

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"student20_pop/message"
)

func (c *laoChannel) processCreateRollCall(data message.Data) error {
	rollCallData := data.(*message.CreateRollCallData)
	if !c.checkRollCallID(rollCallData.Creation, message.Stringer(rollCallData.Name), rollCallData.ID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||creation||name)",
		}
	}

	id := base64.URLEncoding.EncodeToString(rollCallData.ID)
	_, ok := c.rollCalls[id]
	if ok {
		return &message.Error{
			Code:        -3,
			Description: "A Roll call with the same ID already exists",
		}
	}

	c.rollCalls[id] = Created
	return nil
}

func (c *laoChannel) processOpenRollCall(data message.Data, action message.RollCallAction) error {

	rollCallData := data.(*message.OpenRollCallData)

	state, err := c.getRollCallState(rollCallData.Opens)
	if err != nil {
		return message.NewError("The field `opens` does not correspond to any current roll call ID", err)
	}

	if action == message.RollCallAction(message.OpenRollCallAction) {
		// If the action is an OpenRollCallAction,
		// the previous roll call action should be a CreateRollCallAction
		if *state != Created {
			return &message.Error{
				Code:        -1,
				Description: "The roll call can not be opened since it has not been created previously",
			}
		}
	} else {
		// If the action is an RepenRollCallAction,
		// the previous roll call action should be a CloseRollCallAction
		if *state != Closed {
			return &message.Error{
				Code:        -1,
				Description: "The roll call can not be reopened since it has not been closed previously",
			}
		}
	}

	opens := base64.URLEncoding.EncodeToString(rollCallData.Opens)
	if !c.checkRollCallID(message.Stringer(opens), rollCallData.OpenedAt, rollCallData.UpdateID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||opens||opened_at)",
		}
	}

	c.updateRollCall(rollCallData.Opens, rollCallData.UpdateID, Open)
	return nil
}

func (c *laoChannel) processCloseRollCall(data message.Data) error {

	rollCallData := data.(*message.CloseRollCallData)

	state, err := c.getRollCallState(rollCallData.Closes)
	if err != nil {
		return message.NewError("The field `closes` does not correspond to any current roll call ID", err)
	}

	if *state != Open {
		return &message.Error{
			Code:        -1,
			Description: "The roll call can not be closed since it is not open",
		}
	}

	closes := base64.URLEncoding.EncodeToString(rollCallData.Closes)
	if !c.checkRollCallID(message.Stringer(closes), rollCallData.ClosedAt, rollCallData.UpdateID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||closes||closed_at)",
		}
	}

	c.updateRollCall(rollCallData.Closes, rollCallData.Closes, Closed)

	c.attendees = map[string]struct{}{}
	for i := 0; i < len(rollCallData.Attendees); i += 1 {
		c.attendees[string(rollCallData.Attendees[i])] = struct{}{}
	}

	return nil
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

// getRollCallState returns the state of the rollCall of ID `rollCallID`
// If no roll call has the ID `rollCallID`, returns an error
func (c *laoChannel) getRollCallState(rollCallID []byte) (*rollCallState, error) {
	id := base64.URLEncoding.EncodeToString(rollCallID)

	rollCallState, ok := c.rollCalls[id]
	if !ok {
		return nil, &message.Error{
			Code:        -4,
			Description: "The ID does not correspond to the id of any current roll call",
		}
	}

	return &rollCallState, nil
}

func (c *laoChannel) updateRollCall(oldID, newID []byte, newState rollCallState) {
	old := base64.URLEncoding.EncodeToString(oldID)
	new := base64.URLEncoding.EncodeToString(newID)

	// Delete the old roll call entry
	delete(c.rollCalls, old)

	// Set the new rollcall ID with the new state
	c.rollCalls[new] = newState
}
