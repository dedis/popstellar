package sqlite

import (
	"database/sql"
	"encoding/base64"
	"go.dedis.ch/kyber/v3"
	poperrors "popstellar/internal/errors"
	"popstellar/internal/handler/message"
	"popstellar/internal/handler/messagedata/root"
	database "popstellar/internal/repository"
	"sync"
)

var dbLock sync.RWMutex

// SQLite is a wrapper around the SQLite database.
type SQLite struct {
	database.Repository
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
		return SQLite{}, poperrors.NewDatabaseInternalErrorMsg("open connection: %v", err)
	}

	if !foreignKeyOn {
		_, err = db.Exec(foreignKeyOff)
		if err != nil {
			db.Close()
			return SQLite{}, poperrors.NewDatabaseInternalErrorMsg("turn off foreign key constraints: %v", err)
		}
	}

	tx, err := db.Begin()
	if err != nil {
		db.Close()
		return SQLite{}, poperrors.NewDatabaseTransactionBeginErrorMsg("%v", err)
	}
	defer tx.Rollback()

	_, err = tx.Exec(createMessage)
	if err != nil {
		db.Close()
		return SQLite{}, poperrors.NewDatabaseCreateTableErrorMsg("message: %v", err)
	}

	_, err = tx.Exec(createChannelType)
	if err != nil {
		db.Close()
		return SQLite{}, poperrors.NewDatabaseCreateTableErrorMsg("channelType: %v", err)
	}

	err = fillChannelTypes(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	_, err = tx.Exec(createKey)
	if err != nil {
		db.Close()
		return SQLite{}, poperrors.NewDatabaseCreateTableErrorMsg("key: %v", err)
	}

	_, err = tx.Exec(createChannel)
	if err != nil {
		db.Close()
		return SQLite{}, poperrors.NewDatabaseCreateTableErrorMsg("channel: %v", err)
	}

	_, err = tx.Exec(insertOrIgnoreChannel, root.Root, channelTypeToID[message.RootType], "")
	if err != nil {
		db.Close()
		return SQLite{}, poperrors.NewDatabaseInsertErrorMsg("root channel: %v", err)
	}

	_, err = tx.Exec(createChannelMessage)
	if err != nil {
		db.Close()
		return SQLite{}, poperrors.NewDatabaseCreateTableErrorMsg("channelMessage: %v", err)
	}

	err = initRumorTables(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = tx.Commit()
	if err != nil {
		db.Close()
		return SQLite{}, poperrors.NewDatabaseTransactionCommitErrorMsg("%v", err)
	}

	return SQLite{database: db}, nil
}

func initRumorTables(tx *sql.Tx) error {
	_, err := tx.Exec(createRumor)
	if err != nil {
		return poperrors.NewDatabaseCreateTableErrorMsg("rumor: %v", err)
	}

	_, err = tx.Exec(createMessageRumor)
	if err != nil {
		return poperrors.NewDatabaseCreateTableErrorMsg("messageRumor: %v", err)
	}

	_, err = tx.Exec(createUnprocessedMessage)
	if err != nil {
		return poperrors.NewDatabaseCreateTableErrorMsg("unprocessedMessage: %v", err)
	}

	_, err = tx.Exec(createUnprocessedMessageRumor)
	if err != nil {
		return poperrors.NewDatabaseCreateTableErrorMsg("unprocessedMessageRumor: %v", err)
	}

	return nil
}

// Close closes the SQLite database.
func (s *SQLite) Close() error {
	dbLock.Lock()
	defer dbLock.Unlock()

	err := s.database.Close()
	if err != nil {
		return poperrors.NewDatabaseInternalErrorMsg("close connection: %v", err)
	}
	return nil
}

func (s *SQLite) StoreServerKeys(serverPubKey kyber.Point, serverSecretKey kyber.Scalar) error {
	dbLock.Lock()
	defer dbLock.Unlock()

	tx, err := s.database.Begin()
	if err != nil {
		return poperrors.NewDatabaseTransactionBeginErrorMsg("%v", err)
	}
	defer tx.Rollback()

	serverPubBuf, err := serverPubKey.MarshalBinary()
	if err != nil {
		return poperrors.NewKeyMarshalError("server public key: %v", err)
	}
	serverSecBuf, err := serverSecretKey.MarshalBinary()
	if err != nil {
		return poperrors.NewKeyMarshalError("server secret key: %v", err)
	}

	_, err = tx.Exec(insertKeys, serverKeysPath, base64.URLEncoding.EncodeToString(serverPubBuf),
		base64.URLEncoding.EncodeToString(serverSecBuf))
	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("server keys: %v", err)
	}

	err = tx.Commit()
	if err != nil {
		return poperrors.NewDatabaseTransactionCommitErrorMsg("%v", err)
	}
	return nil
}

func (s *SQLite) StoreFirstRumor() error {
	dbLock.Lock()
	defer dbLock.Unlock()
	_, err := s.database.Exec(insertFirstRumor, 0, serverKeysPath)

	if err != nil {
		return poperrors.NewDatabaseInsertErrorMsg("first rumor: %v", err)
	}

	return nil
}

func fillChannelTypes(tx *sql.Tx) error {
	for _, channelType := range channelTypes {
		_, err := tx.Exec(insertChannelType, channelType)
		if err != nil {
			return poperrors.NewDatabaseInsertErrorMsg("channelType %s: %v", channelType, err)
		}
	}
	return nil
}

func (s *SQLite) GetAllChannels() ([]string, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	rows, err := s.database.Query(selectAllChannels)
	if err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("all channels: %v", err)
	}
	defer rows.Close()

	var channels []string
	for rows.Next() {
		var channelPath string
		if err = rows.Scan(&channelPath); err != nil {
			return nil, poperrors.NewDatabaseScanErrorMsg("channel: %v", err)
		}
		channels = append(channels, channelPath)
	}

	if err = rows.Err(); err != nil {
		return nil, poperrors.NewDatabaseSelectErrorMsg("all channels: %v", err)
	}

	return channels, nil
}
