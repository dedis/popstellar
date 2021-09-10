package hub

import (
	"database/sql"
	"log"
	"os"
	"student20_pop/message"
	"student20_pop/message2/messagedata"
	messageX "student20_pop/message2/query/method/message"

	_ "github.com/mattn/go-sqlite3"
	"golang.org/x/xerrors"
)

// processCreateRollCall processes a roll call creation object.
func (c *laoChannel) processCreateRollCall(msg messagedata.RollCallCreate) error {
	// Check that the ProposedEnd is greater than the ProposedStart
	if msg.ProposedStart > msg.ProposedEnd {
		return message.NewErrorf(-4, "The field `proposed_start` is greater than the field `proposed_end`: %d > %d", msg.ProposedStart, msg.ProposedEnd)
	}

	c.rollCall.id = string(msg.ID)
	c.rollCall.state = Created
	return nil
}

// processOpenRollCall processes an open roll call object.
func (c *laoChannel) processOpenRollCall(msg messageX.Message, action string) error {
	if action == "open" {
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

	// Why not messagedata.RollCallReopen ? Maybe we should assume that Reopen
	// message is useless.
	var rollCallOpen messagedata.RollCallOpen

	err := msg.UnmarshalData(&rollCallOpen)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal roll call open: %v", err)
	}

	if !c.rollCall.checkPrevID([]byte(rollCallOpen.Opens)) {
		return message.NewError(-1, "The field `opens` does not correspond to the id of the previous roll call message")
	}

	c.rollCall.id = string(rollCallOpen.UpdateID)
	c.rollCall.state = Open
	return nil
}

// processCloseRollCall processes a close roll call message.
func (c *laoChannel) processCloseRollCall(msg messagedata.RollCallClose) error {
	if c.rollCall.state != Open {
		return message.NewError(-1, "The roll call cannot be closed since it's not open")
	}

	if !c.rollCall.checkPrevID([]byte(msg.Closes)) {
		return message.NewError(-4, "The field `closes` does not correspond to the id of the previous roll call message")
	}

	c.rollCall.id = msg.UpdateID
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

	for _, attendee := range msg.Attendees {
		c.attendees.Add(attendee)

		if db != nil {
			log.Printf("inserting attendee into db")

			err := insertAttendee(db, attendee, c.channelID)
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
