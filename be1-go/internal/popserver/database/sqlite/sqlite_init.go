package sqlite

import (
	"database/sql"
	database2 "popstellar/internal/popserver/database/repository"
)

// SQLite is a wrapper around the SQLite database.
type SQLite struct {
	database2.Repository
	database *sql.DB
}

//======================================================================================================================
// Database initialization
//======================================================================================================================

// NewSQLite returns a new SQLite instance.
func NewSQLite(path string, foreignKeyOn bool) (SQLite, error) {
	db, err := sql.Open("sqlite", path)
	if err != nil {
		return SQLite{}, err
	}

	if !foreignKeyOn {
		_, err = db.Exec("PRAGMA foreign_keys = OFF;")
		if err != nil {
			db.Close()
			return SQLite{}, err
		}
	}

	tx, err := db.Begin()
	if err != nil {
		db.Close()
		return SQLite{}, err
	}
	defer tx.Rollback()

	err = createMessage(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createChannelType(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createKey(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createChannel(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createChannelMessage(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createPendingSignatures(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = tx.Commit()
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	return SQLite{database: db}, nil
}

func createMessage(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS message (" +
		"messageID TEXT, " +
		"message TEXT, " +
		"messageData TEXT NULL, " +
		"storedTime BIGINT, " +
		"PRIMARY KEY (messageID) " +
		")")
	return err
}

func createChannel(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS channel (" +
		"channelPath TEXT, " +
		"typeID TEXT, " +
		"laoPath TEXT NULL, " +
		"FOREIGN KEY (laoPath) REFERENCES channel(channelPath), " +
		"FOREIGN KEY (typeID) REFERENCES channelType(ID), " +
		"PRIMARY KEY (channelPath) " +
		")")

	if err != nil {
		return err
	}

	_, err = tx.Exec("INSERT OR IGNORE INTO channel (channelPath, typeID) VALUES (?, ?)",
		"/root", channelTypeToID[RootType])
	return err
}

func createChannelMessage(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS channelMessage (" +
		"channelPath TEXT, " +
		"messageID TEXT, " +
		"isBaseChannel BOOLEAN, " +
		"FOREIGN KEY (messageID) REFERENCES message(messageID), " +
		"FOREIGN KEY (channelPath) REFERENCES channel(channelPath), " +
		"PRIMARY KEY (channelPath, messageID) " +
		")")
	return err
}

func createPendingSignatures(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS pendingSignatures (" +
		"messageID TEXT, " +
		"witness TEXT, " +
		"signature TEXT UNIQUE, " +
		"PRIMARY KEY (messageID, witness) " +
		")")
	return err
}

func createChannelType(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS channelType (" +
		"ID INTEGER PRIMARY KEY, " +
		"type TEXT" +
		")")

	for _, channelType := range channelTypes {
		_, err = tx.Exec("INSERT INTO channelType (type) VALUES (?)", channelType)
		if err != nil {
			return err
		}
	}
	return err
}

func createKey(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS key (" +
		"channelPath TEXT, " +
		"publicKey BLOB NULL, " +
		"secretKey BLOB NULL, " +
		"FOREIGN KEY (channelPath) REFERENCES channel(channelPath), " +
		"PRIMARY KEY (channelPath) " +
		")")
	return err

}

// Close closes the SQLite database.
func (s *SQLite) Close() error {
	return s.database.Close()
}
