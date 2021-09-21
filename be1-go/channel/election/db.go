package election

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
		election_channel(
			election_channel_id,
			start_timestamp,
			end_timestamp,
			terminated
		)
	VALUES(?, ?, ?, ?)`

	stmt, err := db.Prepare(query)
	if err != nil {
		return xerrors.Errorf(dbPrepareErr, err)
	}

	terminatedInt := 0
	if c.terminated {
		terminatedInt = 1
	}

	defer stmt.Close()

	_, err = stmt.Exec(c.channelID, c.start, c.end, terminatedInt)
	if err != nil {
		return xerrors.Errorf("failed to insert channel: %v", err)
	}

	err = c.saveAttendees(db)
	if err != nil {
		return xerrors.Errorf("failed to save attendees: %v", err)
	}

	err = c.saveQuestions(db)
	if err != nil {
		return xerrors.Errorf("failed to save questions: %v", err)
	}

	return nil
}

func (c *Channel) saveAttendees(db *sql.DB) error {
	for attendee := range c.attendees.store {
		query := `
		INSERT INTO
			election_attendee(
				attendee_key,
				election_channel_id
			)
		VALUES(?, ?)`

		stmt, err := db.Prepare(query)
		if err != nil {
			return xerrors.Errorf(dbPrepareErr, err)
		}

		_, err = stmt.Exec(attendee, c.channelID)

		stmt.Close()

		if err != nil {
			return xerrors.Errorf("failed to insert attendee: %v", err)
		}
	}

	return nil
}

func (c *Channel) saveQuestions(db *sql.DB) error {
	for _, question := range c.questions {
		query := `
		INSERT INTO
			election_question(
				question_id,
				method,
				election_channel_id
			)
		VALUES(?, ?, ?)`

		stmt, err := db.Prepare(query)
		if err != nil {
			return xerrors.Errorf(dbPrepareErr, err)
		}

		_, err = stmt.Exec(question.id, question.method, c.channelID)

		stmt.Close()

		if err != nil {
			return xerrors.Errorf("failed to insert question: %v", err)
		}

		err = saveQuestionBallotOption(db, question)
		if err != nil {
			return xerrors.Errorf("failed to save ballot option: %v", err)
		}

		err = saveValidVotes(db, question)
		if err != nil {
			return xerrors.Errorf("failed to save validVotes: %v", err)
		}
	}

	return nil
}

func saveQuestionBallotOption(db *sql.DB, question *question) error {
	for option := range question.ballotOptions {
		query := `
		INSERT INTO
			election_question_ballot_option(
				option_text,
				question_id
			)
		VALUES(?, ?)`

		stmt, err := db.Prepare(query)
		if err != nil {
			return xerrors.Errorf(dbPrepareErr, err)
		}

		_, err = stmt.Exec(option, question.id)

		stmt.Close()

		if err != nil {
			return xerrors.Errorf("failed to insert ballot option: %v", err)
		}
	}

	return nil
}

func saveValidVotes(db *sql.DB, question *question) error {
	for voter, vote := range question.validVotes {
		query := `
		INSERT INTO
			election_valid_vote(
				voter_id,
				vote_timestamp,
				question_id
			)
		VALUES(?, ?, ?)`

		stmt, err := db.Prepare(query)
		if err != nil {
			return xerrors.Errorf(dbPrepareErr, err)
		}

		_, err = stmt.Exec(voter, vote.voteTime, question.id)

		stmt.Close()

		if err != nil {
			return xerrors.Errorf("failed to insert valid vote: %v", err)
		}

		err = saveVoteIndex(db, voter, vote)
		if err != nil {
			return xerrors.Errorf("failed to save vote index: %v", err)
		}
	}

	return nil
}

func saveVoteIndex(db *sql.DB, voter string, vote validVote) error {
	for index := range vote.indexes {
		query := `
		INSERT INTO
			vote_index(
				vote_index,
				voter_id
			)
		VALUES(?, ?)`

		stmt, err := db.Prepare(query)
		if err != nil {
			return xerrors.Errorf(dbPrepareErr, err)
		}

		_, err = stmt.Exec(index, voter)

		stmt.Close()

		if err != nil {
			return xerrors.Errorf("failed to insert vote index: %v", err)
		}
	}

	return nil
}

// GetChannelsFromDB returns all the elections channels found in the DB
func GetChannelsFromDB(hub channel.HubFunctionalities) ([]channel.Channel, error) {
	db, err := sql.Open("sqlite3", sqlite.GetDBPath())
	if err != nil {
		return nil, xerrors.Errorf("failed to open connection: %v", err)
	}

	defer db.Close()

	query := `
	SELECT
		election_channel_id,
		start_timestamp,
		end_timestamp,
		terminated
	FROM
		election_channel`

	rows, err := db.Query(query)
	if err != nil {
		return nil, xerrors.Errorf("failed to query channels: %v", err)
	}

	defer rows.Close()

	result := []channel.Channel{}

	for rows.Next() {
		var channelPath string
		var start, end int64
		var terminated int

		err = rows.Scan(&channelPath, &start, &end, &terminated)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		channel, err := loadChannelFromDB(db, channelPath, start, end, terminated == 1, hub)
		if err != nil {
			return nil, xerrors.Errorf("failed to load channel: %v", err)
		}

		result = append(result, channel)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}

// loadChannelFromDB restores a channel from the db
func loadChannelFromDB(db *sql.DB, channelPath string, start, end int64,
	terminated bool, hub channel.HubFunctionalities) (channel.Channel, error) {

	inbox, err := inbox.CreateInboxFromDB(db, channelPath)
	if err != nil {
		return nil, xerrors.Errorf("failed to load inbox: %v", err)
	}

	attendees, err := getAttendeesFromDB(db, channelPath)
	if err != nil {
		return nil, xerrors.Errorf("failed to get attendees: %v", err)
	}

	questions, err := getQuestionsFromDB(db, channelPath)
	if err != nil {
		return nil, xerrors.Errorf("failed to get questions: %v", err)
	}

	return &Channel{
		sockets:   channel.NewSockets(),
		inbox:     inbox,
		channelID: channelPath,

		start:      start,
		end:        end,
		terminated: terminated,

		questions: questions,
		attendees: attendees,

		hub: hub,
	}, nil
}

func getAttendeesFromDB(db *sql.DB, channelPath string) (*attendees, error) {
	query := `
		SELECT
			attendee_key
		FROM
			election_attendee
		WHERE
			election_channel_id = ?`

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

	result := map[string]struct{}{}

	for rows.Next() {
		var attendeeKey string

		err = rows.Scan(&attendeeKey)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result[attendeeKey] = struct{}{}
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return &attendees{
		store: result,
	}, nil
}

func getQuestionsFromDB(db *sql.DB, channelPath string) (map[string]*question, error) {
	query := `
	SELECT
		question_id,
		method
	FROM
		election_question
	WHERE
		election_channel_id = ?`

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

	result := map[string]*question{}

	for rows.Next() {
		var questionID string
		var method string

		err = rows.Scan(&questionID, &method)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		options, err := getBallotOptions(db, questionID)
		if err != nil {
			return nil, xerrors.Errorf("failed to get options: %v", err)
		}

		validVotes, err := getValidVotes(db, questionID)
		if err != nil {
			return nil, xerrors.Errorf("failed to get valid votes: %v", err)
		}

		question := &question{
			id:            []byte(questionID),
			ballotOptions: options,
			validVotes:    validVotes,
			method:        method,
		}

		result[questionID] = question
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}

func getBallotOptions(db *sql.DB, questionID string) ([]string, error) {
	query := `
	SELECT
		option_text
	FROM
		election_question_ballot_option
	WHERE
		question_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(questionID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := []string{}

	for rows.Next() {
		var optionText string

		err = rows.Scan(&optionText)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, optionText)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}

func getValidVotes(db *sql.DB, questionID string) (map[string]validVote, error) {
	query := `
	SELECT
		voter_id,
		vote_timestamp
	FROM
		election_valid_vote
	WHERE
		question_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(questionID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := map[string]validVote{}

	for rows.Next() {
		var voterID string
		var voteTimestamp int64

		err = rows.Scan(&voterID, &voteTimestamp)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		indexes, err := getValidVoteIndexesFromDB(db, voterID)
		if err != nil {
			return nil, xerrors.Errorf("failed to get vote indexes: %v", err)
		}

		result[voterID] = validVote{
			voteTime: voteTimestamp,
			indexes:  indexes,
		}
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}

func getValidVoteIndexesFromDB(db *sql.DB, voterID string) ([]int, error) {
	query := `
	SELECT
		vote_index
	FROM
		vote_index
	WHERE
		voter_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(voterID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := []int{}

	for rows.Next() {
		var voteIndex int

		err = rows.Scan(&voteIndex)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, voteIndex)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}
