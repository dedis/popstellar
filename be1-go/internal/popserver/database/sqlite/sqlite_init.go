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

	_, err = tx.Exec(createMessage)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	_, err = tx.Exec(createChannelType)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	for _, channelType := range channelTypes {
		_, err = tx.Exec("INSERT INTO channelType (type) VALUES (?)", channelType)
		if err != nil {
			db.Close()
			return SQLite{}, err
		}
	}

	_, err = tx.Exec(createKey)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	_, err = tx.Exec(createChannel)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	_, err = tx.Exec(insertChannel, "/root", channelTypeToID[RootType], "")
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	_, err = tx.Exec(createChannelMessage)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	_, err = tx.Exec(createPendingSignatures)
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

// Close closes the SQLite database.
func (s *SQLite) Close() error {
	return s.database.Close()
}
