package storage

import (
	"database/sql"
	"encoding/json"
	"fmt"
	_ "modernc.org/sqlite"
	"popstellar/message/query/method/message"
	"strings"
	"time"
)

const (
	defaultPath = "sqlite.db"
)

// SQLite is a wrapper around the SQLite database.
type SQLite struct {
	Storage
	database *sql.DB
	path     string
}

// New returns a new SQLite instance.
func New(path string) (SQLite, error) {

	db, err := sql.Open("sqlite", path)
	if err != nil {
		return SQLite{}, err
	}

	tx, err := db.Begin()

	//TODO Delete database if error occurs
	if err != nil {
		return SQLite{}, err
	}
	defer tx.Rollback()

	_, err = tx.Exec("CREATE TABLE IF NOT EXISTS configuration (" +
		"pubKeyOwner BLOB NULL, " +
		"clientServerAddress TEXT NULL, " +
		"serverServerAddress TEXT NULL " +
		")")
	if err != nil {
		return SQLite{}, err
	}

	_, err = tx.Exec("CREATE TABLE IF NOT EXISTS inbox (" +
		"messageID TEXT, " +
		"message BLOB, " +
		"storedTime BIGINT, " +
		"baseChannel TEXT, " +
		"PRIMARY KEY (messageID) " +
		")")
	if err != nil {
		return SQLite{}, err
	}

	_, err = tx.Exec("CREATE TABLE IF NOT EXISTS channelMessage (" +
		"channel TEXT, " +
		"messageID TEXT, " +
		"FOREIGN KEY (messageID) REFERENCES inbox(messageID), " +
		"PRIMARY KEY (channel, messageID) " +
		")")
	if err != nil {
		return SQLite{}, err
	}

	_, err = tx.Exec("CREATE TABLE IF NOT EXISTS channels (" +
		"channel TEXT, " +
		"organizerPubKey TEXT, " +
		"pubElectionKey TEXT NULL, " +
		"secElectionKey TEXT NULL, " +
		"PRIMARY KEY (channel) " +
		")")
	if err != nil {
		return SQLite{}, err
	}

	_, err = tx.Exec("CREATE TABLE IF NOT EXISTS pendingSignatures (" +
		"messageID TEXT, " +
		"sendPubKey TEXT, " +
		"signature TEXT UNIQUE, " +
		"FOREIGN KEY (messageID) REFERENCES inbox(messageID), " +
		"PRIMARY KEY (messageID) " +
		")")
	if err != nil {
		return SQLite{}, err
	}

	err = tx.Commit()
	if err != nil {
		return SQLite{}, err
	}

	return SQLite{path: path, database: db}, nil
}

// Close closes the SQLite database.
func (s *SQLite) Close() error {
	return s.database.Close()
}

// GetMessagesByID returns a set of messages by their IDs.
func (s *SQLite) GetMessagesByID(IDs []string) (map[string]message.Message, error) {

	columns := "messageID, message"
	idStrings := make([]string, 0, len(IDs))

	for _, id := range IDs {
		idStrings = append(idStrings, fmt.Sprintf("'%s'", id))
	}

	query := "SELECT " + columns + " FROM inbox WHERE messageID IN (" + strings.Join(idStrings, ",") + ")"

	rows, err := s.database.Query(query)
	if err != nil {
		return nil, err
	}

	messagesByID := make(map[string]message.Message, len(IDs))

	for rows.Next() {

		var messageID string
		var messageByte []byte
		if err = rows.Scan(&messageID, &messageByte); err != nil {
			return nil, err
		}

		var msg message.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, err
		}

		messagesByID[messageID] = msg
	}

	return messagesByID, nil
}

// GetSortedMessages returns all messages sorted by stored time.
func (s *SQLite) GetSortedMessages(channel string) ([]message.Message, error) {

	tx, err := s.database.Begin()
	if err != nil {
		return nil, err
	}

	defer tx.Rollback()

	var count int
	err = tx.QueryRow("SELECT COUNT(*) FROM channelMessage "+
		"WHERE channelMessage.channel = ?", channel).Scan(&count)
	if err != nil {
		return nil, err
	}

	rows, err := tx.Query(" SELECT inbox.message "+
		"FROM inbox "+
		"JOIN channelMessage ON inbox.messageID = channelMessage.messageID "+
		"WHERE channelMessage.channel = ?"+
		"ORDER BY inbox.storedTime DESC ", channel)
	if err != nil {
		return nil, err
	}

	messages := make([]message.Message, 0, count)

	for rows.Next() {

		var messageByte []byte
		if err = rows.Scan(&messageByte); err != nil {
			return nil, err
		}
		var msg message.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, err
		}
		messages = append(messages, msg)
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}

	err = tx.Commit()
	if err != nil {
		return nil, err
	}

	return messages, nil
}

// GetMessageByID returns a message by its ID.
func (s *SQLite) GetMessageByID(ID string) (message.Message, error) {

	var messageByte []byte
	err := s.database.QueryRow("SELECT message FROM inbox WHERE messageID = ?", ID).Scan(&messageByte)
	if err != nil {
		return message.Message{}, err
	}

	var msg message.Message
	if err = json.Unmarshal(messageByte, &msg); err != nil {
		return message.Message{}, err
	}

	return msg, nil
}

// GetIDsTable returns the map of message IDs by channelID.
func (s *SQLite) GetIDsTable() (map[string][]string, error) {

	query := "SELECT baseChannel, GROUP_CONCAT(messageID) FROM inbox " +
		"GROUP BY baseChannel"
	rows, err := s.database.Query(query)
	if err != nil {
		return nil, err
	}

	IDsTable := make(map[string][]string)

	for rows.Next() {
		var channelID string
		var messageIDs string
		if err = rows.Scan(&channelID, &messageIDs); err != nil {
			return nil, err
		}
		IDsTable[channelID] = strings.Split(messageIDs, ",")
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}

	return IDsTable, nil
}

// StoreMessage stores a message inside the SQLite database.
func (s *SQLite) StoreMessage(channel string, msg message.Message) error {

	storedTime := time.Now().UnixNano()

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return err
	}

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}

	defer tx.Rollback()

	_, err = tx.Exec(`
        INSERT INTO inbox (messageID, message, storedTime, baseChannel)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(messageID) DO UPDATE SET
        storedTime = excluded.storedTime,
        baseChannel = excluded.baseChannel
        WHERE baseChannel LIKE excluded.baseChannel || '%'
    `, msg.MessageID, msgByte, storedTime, channel)
	if err != nil {
		return err
	}

	_, err = tx.Exec("INSERT OR IGNORE INTO channelMessage "+
		"(channel, messageID) VALUES "+
		"(?, ?)", channel, msg.MessageID)
	if err != nil {
		return err
	}

	err = tx.Commit()

	return err
}
