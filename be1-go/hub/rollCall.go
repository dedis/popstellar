package hub

import (
	"database/sql"
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
		return message.NewErrorf(-4, "The field `proposed_start` is greater than the field `proposed_end`: %d > %d", rollCallData.ProposedStart, rollCallData.ProposedEnd)
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
			return message.NewError(-1, "The roll call cannot be opened since it does not exist")
		}
	} else {
		// If the action is an RepenRollCallAction,
		// the previous roll call action should be a CloseRollCallAction
		if c.rollCall.state != Closed {
			return message.NewError(-1, "The roll call cannot be reopened since it has not been closed")
		}
	}

	rollCallData := data.(*message.OpenRollCallData)

	if !c.rollCall.checkPrevID(rollCallData.Opens) {
		return message.NewError(-1, "The field `opens` does not correspond to the id of the previous roll call message")
	}

	c.rollCall.id = string(rollCallData.UpdateID)
	c.rollCall.state = Open
	return nil
}

// processCloseRollCall processes a close roll call message.
func (c *laoChannel) processCloseRollCall(data message.Data) error {
	if c.rollCall.state != Open {
		return message.NewError(-1, "The roll call cannot be closed since it's not open")
	}

	rollCallData := data.(*message.CloseRollCallData)
	if !c.rollCall.checkPrevID(rollCallData.Closes) {
		return message.NewError(-4, "The field `closes` does not correspond to the id of the previous roll call message")
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
