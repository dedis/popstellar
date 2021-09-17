package hub

import (
	"bytes"
	"database/sql"
	"encoding/base64"
	"fmt"
	"log"
	"os"
	"student20_pop/message"

	_ "github.com/mattn/go-sqlite3"
	"golang.org/x/xerrors"
)

// processCreateRollCall processes a roll call creation object.
func (c *laoChannel) processCreateRollCall(data message.Data) error {

	rollCallData := data.(*message.CreateRollCallData)

	// Check that the ProposedEnd is greater than the ProposedStart
	if rollCallData.ProposedStart > rollCallData.ProposedEnd {
		return &message.Error{
			Code:        -4,
			Description: "The field `proposed_start` is greater than the field `proposed_end`",
		}
	}

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

// processOpenRollCall processes an open roll call object.
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

	opens := base64.URLEncoding.EncodeToString(rollCallData.Opens)
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

// processCloseRollCall processes a close roll call message.
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

	closes := base64.URLEncoding.EncodeToString(rollCallData.Closes)
	if !c.checkRollCallID(message.Stringer(closes), rollCallData.ClosedAt, rollCallData.UpdateID) {
		return &message.Error{
			Code:        -4,
			Description: "The id of the roll call does not correspond to SHA256(‘R’||lao_id||closes||closed_at)",
		}
	}

	c.rollCall.id = string(rollCallData.UpdateID)
	c.rollCall.state = Closed

	var db *sql.DB

	if os.Getenv("HUB_DB") != "" {
		db, err := sql.Open("sqlite3", os.Getenv("HUB_DB"))
		if err != nil {
			log.Printf("error: failed to connect to db: %v", err)
			db = nil
		} else {
			defer db.Close()
		}
	}

	for _, attendee := range rollCallData.Attendees {
		c.attendees.Add(attendee.String())

		if db != nil {
			log.Printf("inserting attendee into db")

			err := insertAttendee(db, attendee.String(), c.channelID)
			if err != nil {
				log.Printf("error: failed to insert attendee into db: %v", err)
			}
		}
	}

	return nil
}

func insertAttendee(db *sql.DB, key string, channelID string) error {
	stmt, err := db.Prepare("insert into lao_attendee(attendee_key, lao_channel_id) values(?, ?)")
	if err != nil {
		return xerrors.Errorf("failed to prepare query: %v", err)
	}

	defer stmt.Close()

	_, err = stmt.Exec(key, channelID)
	if err != nil {
		return xerrors.Errorf("failed to exec query: %v", err)
	}

	return nil
}

// checkPrevID is a helper method which validates the roll call ID.
func (r *rollCall) checkPrevID(prevID []byte) bool {
	return string(prevID) == r.id
}

// checkRollCallID checks if the id of the roll call corresponds to the hash
// of the correct parameters. Returns true if the hash corresponds to the
// id and false otherwise.
func (c *laoChannel) checkRollCallID(str1, str2 fmt.Stringer, id []byte) bool {
	laoID := c.channelID[6:]
	hash, err := message.Hash(message.Stringer('R'), message.Stringer(laoID), str1, str2)
	if err != nil {
		return false
	}

	return bytes.Equal(hash, id)
}
