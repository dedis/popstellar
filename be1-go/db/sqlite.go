package db

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"log"
	"math"
	"os"
	"student20_pop/message"

	_ "github.com/mattn/go-sqlite3"
	"golang.org/x/xerrors"
)

type sqlite struct {
	conn *sql.DB
}

var (
	DDL = `
	CREATE TABLE messages (
		channel_id text, message_id text, timestamp integer, message text,
		PRIMARY KEY (channel_id, message_id)
	);
	CREATE INDEX idx_timestamp ON messages(channel_id, timestamp);
	`
)

// NewSQLiteRepository instantiates an sqlite repository.
// TODO: Add support for schema migrations.
func NewSQLiteRepository(path string) (Repository, error) {
	if _, err := os.Stat(path); xerrors.Is(err, os.ErrNotExist) {
		// create database
		err := createDatabase(path)
		if err != nil {
			return nil, xerrors.Errorf("failed to create database: %v", err)
		}
	}

	conn, err := sql.Open("sqlite3", path)
	if err != nil {
		return nil, xerrors.Errorf("failed to open database at %s: %v", path, err)
	}

	conn.SetMaxOpenConns(1)

	return &sqlite{
		conn: conn,
	}, nil
}

func createDatabase(path string) error {
	conn, err := sql.Open("sqlite3", path)
	if err != nil {
		return xerrors.Errorf("failed to create database at %s: %v", path, err)
	}

	defer conn.Close()

	_, err = conn.Exec(DDL)
	if err != nil {
		return xerrors.Errorf("failed to execute DDL statements: %v", err)
	}

	return nil
}

// GetMessages returns all the messages for the given `channelID` sorted
// in ascending order of their timestamp.
func (s *sqlite) GetMessages(channelID string) ([]message.Message, error) {
	return s.GetMessagesInRange(channelID, 0, math.MaxInt64)
}

// GetMessagesInRange returns all the messages for the given `channelID`
// in the given timestamp range (inclusive).
func (s *sqlite) GetMessagesInRange(channelID string, start, end message.Timestamp) ([]message.Message, error) {
	rows, err := s.conn.Query("select message from messages where channel_id = ? and (timestamp >= ? AND timestamp <= ?) order by timestamp asc", channelID, start, end)
	if err != nil {
		return nil, xerrors.Errorf("failed to execute query: %v", err)
	}

	messages := []message.Message{}

	defer rows.Close()

	for rows.Next() {
		var (
			messageJson string
		)
		err := rows.Scan(&messageJson)
		if err != nil {
			log.Printf("failed to read message column for channelID=%s", channelID)
			continue
		}

		var message message.Message
		err = json.Unmarshal([]byte(messageJson), &message)
		if err != nil {
			log.Printf("failed to parse message: %s", messageJson)
			continue
		}

		messages = append(messages, message)
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf("failed to read rows: %v", err)
	}

	return messages, nil
}

// AddWitnessToMessage adds a witness signature and public key pair to the
// message with `messageID` as the identifier. The update occurs inside a
// transaction to avoid a dirty read of the message while a concurrent
// update is in progress. Please note that SQLite holds a database level lock
// while a write is in progress and it times out after a default value of
// 5 seconds.
func (s *sqlite) AddWitnessToMessage(messageID string, keyAndSignature message.PublicKeySignaturePair) error {
	tx, err := s.conn.Begin()
	if err != nil {
		return xerrors.Errorf("failed to create transaction: %v", err)
	}

	row := tx.QueryRow("SELECT message from messages where message_id = ?", messageID)

	var marshaledMsg string
	err = row.Scan(&marshaledMsg)
	if err != nil {
		tx.Rollback()
		return xerrors.Errorf("failed to read message from row: %v", err)
	}

	msg := message.Message{}
	err = json.Unmarshal([]byte(marshaledMsg), &msg)
	if err != nil {
		tx.Rollback()
		return xerrors.Errorf("failed to unmarshal message: %v", err)
	}

	msg.WitnessSignatures = append(msg.WitnessSignatures, keyAndSignature)

	msgBuf, err := json.Marshal(msg)
	if err != nil {
		tx.Rollback()
		return xerrors.Errorf("failed to marshal msg: %v", err)
	}

	_, err = tx.Exec("UPDATE messages SET message = ? WHERE message_id = ?", string(msgBuf), messageID)
	if err != nil {
		tx.Rollback()
		return xerrors.Errorf("failed to create update statement: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return xerrors.Errorf("failed to commit transaction: %v", err)
	}

	return nil
}

// AddMessage adds a new message to the channel specified by `channelID`
func (s *sqlite) AddMessage(channelID string, msg message.Message, timestamp message.Timestamp) error {
	messageID := base64.StdEncoding.EncodeToString(msg.MessageID)
	msgBuf, err := json.Marshal(msg)
	if err != nil {
		return xerrors.Errorf("failed to marshal message: %v", err)
	}

	tx, err := s.conn.Begin()
	if err != nil {
		return xerrors.Errorf("failed to create transaction: %v", err)
	}

	_, err = tx.Exec("INSERT INTO messages(channel_id, message_id, timestamp, message) VALUES (?, ?, ?, ?)", channelID, messageID, timestamp, string(msgBuf))
	if err != nil {
		tx.Rollback()
		return xerrors.Errorf("failed to execute insert: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return xerrors.Errorf("failed to commit transaction: %v", err)
	}

	return nil
}

func (s *sqlite) Close() error {
	err := s.conn.Close()
	if err != nil {
		return xerrors.Errorf("failed to close database: %v", err)
	}

	return nil
}
