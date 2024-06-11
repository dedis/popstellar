package sqlite

import (
	"database/sql"
	"encoding/json"
	"errors"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/lao/mlao"
)

func (s *SQLite) IsAttendee(laoPath, poptoken string) (bool, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var rollCallCloseBytes []byte
	err := s.database.QueryRow(selectLastRollCallClose,
		laoPath,
		messagedata.RollCallObject,
		messagedata.RollCallActionClose).
		Scan(&rollCallCloseBytes)

	if err != nil {
		return false, poperrors.NewDatabaseSelectErrorMsg("last roll call close message data (%s, %s): %v", laoPath, poptoken, err)
	}

	var rollCallClose mlao.RollCallClose
	err = json.Unmarshal(rollCallCloseBytes, &rollCallClose)
	if err != nil {
		return false, poperrors.NewInternalServerError("failed to unmarshal last roll call close message: %v", err)
	}

	for _, attendee := range rollCallClose.Attendees {
		if attendee == poptoken {
			return true, nil
		}
	}

	return false, nil
}

func (s *SQLite) GetReactionSender(messageID string) (string, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var sender string
	var object string
	var action string
	err := s.database.QueryRow(selectSender, messageID).Scan(&sender, &object, &action)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return "", nil
	} else if err != nil {
		return "", poperrors.NewDatabaseSelectErrorMsg("sender of message ID %s: %v", messageID, err)

	}

	if object != messagedata.ReactionObject || action != messagedata.ReactionActionAdd {
		return "", poperrors.NewInternalServerError("message ID %s is not a reaction add message", messageID)
	}
	return sender, nil
}
