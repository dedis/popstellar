package lao

import (
	"database/sql"
	"log"
	"popstellar/channel"
	"popstellar/channel/inbox"
	"popstellar/db/sqlite"

	"golang.org/x/xerrors"
)

const (
	dbPrepareErr  = "failed to prepare query: %v"
	dbParseRowErr = "failed to parse row: %v"
	dbRowIterErr  = "error in row iteration: %v"
	dbQueryRowErr = "failed to query rows: %v"
)

func (c *Channel) saveChannel() error {
	log.Printf("trying to save the channel in db at %s", sqlite.GetDBPath())

	db, err := sql.Open("sqlite3", sqlite.GetDBPath())
	if err != nil {
		return xerrors.Errorf("failed to open connection: %v", err)
	}

	defer db.Close()

	query := `
	INSERT INTO
		lao_channel ( lao_channel_id )
	VALUES(?)`

	stmt, err := db.Prepare(query)
	if err != nil {
		return xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	_, err = stmt.Exec(c.channelID)
	if err != nil {
		return xerrors.Errorf("failed to insert channel: %v", err)
	}

	return nil
}

func insertAttendee(db *sql.DB, key string, channelID string) error {
	query := `
	INSERT INTO
		lao_attendee(
			attendee_key,
			lao_channel_id
		)
	VALUES(?, ?)`

	stmt, err := db.Prepare(query)
	if err != nil {
		return xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	_, err = stmt.Exec(key, channelID)
	if err != nil {
		return xerrors.Errorf("failed to exec query: %v", err)
	}

	return nil
}

// GetChannelsFromDB returns all the lao channels found in the DB
func GetChannelsFromDB(hub channel.HubFunctionalities) ([]channel.Channel, error) {
	db, err := sql.Open("sqlite3", sqlite.GetDBPath())
	if err != nil {
		return nil, xerrors.Errorf("failed to open connection: %v", err)
	}

	defer db.Close()

	query := `
		SELECT
			lao_channel_id
		FROM
			lao_channel`

	rows, err := db.Query(query)
	if err != nil {
		return nil, xerrors.Errorf("failed to query channels: %v", err)
	}

	defer rows.Close()

	result := []channel.Channel{}

	for rows.Next() {
		var channelPath string

		err = rows.Scan(&channelPath)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		channel, err := createChannelFromDB(db, channelPath, hub)
		if err != nil {
			return nil, xerrors.Errorf("failed to create channel from db: %v", err)
		}

		result = append(result, channel)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}

// createChannelFromDB restores a channel from the db
func createChannelFromDB(db *sql.DB, channelPath string, hub channel.HubFunctionalities) (channel.Channel, error) {
	channel := Channel{
		channelID: channelPath,
		sockets:   channel.NewSockets(),
		inbox:     inbox.NewInbox(channelPath),
		hub:       hub,
		rollCall:  rollCall{},
		attendees: make(map[string]struct{}),
	}

	attendees, err := getAttendeesChannelFromDB(db, channelPath)
	if err != nil {
		return nil, xerrors.Errorf("failed to get attendees: %v", err)
	}

	for _, attendee := range attendees {
		channel.attendees[attendee] = struct{}{}
	}

	witnesses, err := getWitnessChannelFromDB(db, channelPath)
	if err != nil {
		return nil, xerrors.Errorf("failed to get witnesses: %v", err)
	}

	channel.witnesses = witnesses

	inbox, err := inbox.CreateInboxFromDB(db, channelPath)
	if err != nil {
		return nil, xerrors.Errorf("failed to load inbox: %v", err)
	}

	channel.inbox = inbox

	return &channel, nil
}

func getAttendeesChannelFromDB(db *sql.DB, channelPath string) ([]string, error) {
	query := `
		SELECT
			attendee_key
		FROM
			lao_attendee
		WHERE
			lao_channel_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelPath)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := make([]string, 0)

	for rows.Next() {
		var attendeeKey string

		err = rows.Scan(&attendeeKey)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, attendeeKey)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}

func getWitnessChannelFromDB(db *sql.DB, channelPath string) ([]string, error) {
	query := `
		SELECT
			pub_key
		FROM
			lao_witness
		WHERE
			lao_channel_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelPath)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := make([]string, 0)

	for rows.Next() {
		var pubKey string

		err = rows.Scan(&pubKey)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, pubKey)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}
