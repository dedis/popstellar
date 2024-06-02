package sqlite

import (
	"database/sql"
	"encoding/base64"
	"go.dedis.ch/kyber/v3"
	database2 "popstellar/internal/repository"
	"sync"
)

var dbLock sync.RWMutex

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
	dbLock.Lock()
	defer dbLock.Unlock()

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

	err = fillChannelTypes(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
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

	_, err = tx.Exec(insertOrIgnoreChannel, "/root", channelTypeToID[RootType], "")
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

	err = initRumorTables(tx)
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

func initRumorTables(tx *sql.Tx) error {
	_, err := tx.Exec(createRumor)
	if err != nil {
		return err
	}

	_, err = tx.Exec(createMessageRumor)
	if err != nil {
		return err
	}

	_, err = tx.Exec(createUnprocessedMessage)
	if err != nil {
		return err
	}

	_, err = tx.Exec(createUnprocessedMessageRumor)
	if err != nil {
		return err
	}

	return nil
}

// Close closes the SQLite database.
func (s *SQLite) Close() error {
	dbLock.Lock()
	defer dbLock.Unlock()

	return s.database.Close()
}

func (s *SQLite) StoreServerKeys(serverPubKey kyber.Point, serverSecretKey kyber.Scalar) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	serverPubBuf, err := serverPubKey.MarshalBinary()
	if err != nil {
		return err
	}
	serverSecBuf, err := serverSecretKey.MarshalBinary()
	if err != nil {
		return err
	}

	_, err = tx.Exec(insertKeys, serverKeysPath, base64.URLEncoding.EncodeToString(serverPubBuf),
		base64.URLEncoding.EncodeToString(serverSecBuf))
	if err != nil {
		return err
	}

	return tx.Commit()
}

func (s *SQLite) StoreFirstRumor() error {
	dbLock.Lock()
	defer dbLock.Unlock()
	_, err := s.database.Exec(insertFirstRumor, 0, serverKeysPath)
	return err
}

func fillChannelTypes(tx *sql.Tx) error {
	for _, channelType := range channelTypes {
		_, err := tx.Exec("INSERT INTO channelType (type) VALUES (?)", channelType)
		if err != nil {
			return err
		}
	}
	return nil
}
