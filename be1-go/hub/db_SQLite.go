package hub

import (
	"database/sql"
	"encoding/json"
	"errors"
	"go.dedis.ch/kyber/v3"
	_ "modernc.org/sqlite"
	"popstellar/crypto"
	"popstellar/message/query/method/message"
	"strings"
	"time"
)

const (
	defaultPath = "sqlite.db"
)

// SQLite is a wrapper around the SQLite database.
type SQLite struct {
	Repository
	database *sql.DB
}

// NewSQLite returns a new SQLite instance.
func NewSQLite(path string) (SQLite, error) {
	db, err := sql.Open("sqlite", path)
	if err != nil {
		return SQLite{}, err
	}

	tx, err := db.Begin()
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	defer tx.Rollback()

	err = createConfiguration(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createInbox(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createChannelMessage(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createChannels(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createPendingSignatures(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createChannelType(tx)
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

func createConfiguration(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS configuration (" +
		"ownerPubKey BLOB NULL, " +
		"clientServerAddress TEXT NULL, " +
		"serverServerAddress TEXT NULL " +
		"serverPubKey BLOB NULL, " +
		"serverSecretKey BLOB NULL, " +
		")")
	return err
}

func createInbox(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS inbox (" +
		"messageID TEXT, " +
		"message jsonb, " +
		"storedTime BIGINT, " +
		"baseChannel TEXT, " +
		"PRIMARY KEY (messageID) " +
		")")
	return err
}

func createChannelMessage(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS channelMessage (" +
		"channel TEXT, " +
		"messageID TEXT, " +
		"FOREIGN KEY (messageID) REFERENCES inbox(messageID), " +
		"FOREIGN KEY (channel) REFERENCES channels(channel), " +
		"PRIMARY KEY (channel, messageID) " +
		")")
	return err
}

func createChannels(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS channels (" +
		"channel TEXT, " +
		"organizerPubKey TEXT NULL, " +
		"pubElectionKey TEXT NULL, " +
		"secElectionKey TEXT NULL, " +
		"PRIMARY KEY (channel) " +
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
		"ID INTEGER, " +
		"name TEXT, " +
		"PRIMARY KEY (ID) " +
		")")
	return err
}

// Close closes the SQLite database.
func (s *SQLite) Close() error {
	return s.database.Close()
}

func convertToInterfaceSlice(slice []string) []interface{} {
	s := make([]interface{}, len(slice))
	for i, v := range slice {
		s[i] = v
	}
	return s
}

// GetMessagesByID returns a set of messages by their IDs.
func (s *SQLite) GetMessagesByID(IDs []string) (map[string]message.Message, error) {
	rows, err := s.database.Query("SELECT messageID, message "+
		"FROM inbox "+
		"WHERE messageID IN ("+strings.Repeat("?,", len(IDs)-1)+"?"+")", convertToInterfaceSlice(IDs)...)
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

	rows, err := tx.Query(" SELECT inbox.message "+
		"FROM inbox "+
		"JOIN channelMessage ON inbox.messageID = channelMessage.messageID "+
		"WHERE channelMessage.channel = ?"+
		"ORDER BY inbox.storedTime DESC ", channel)
	if err != nil {
		return nil, err
	}

	messages := make([]message.Message, 0)

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

	if rows.Err() != nil || tx.Commit() != nil {
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
	rows, err := s.database.Query("SELECT baseChannel, GROUP_CONCAT(messageID) " +
		"FROM inbox " +
		"GROUP BY baseChannel")
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
	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if err = addPendingSignatures(tx, &msg); err != nil {
		return err
	}

	if err = storeInbox(tx, msg, channel); err != nil {
		return err

	}
	if err = storeChannelMessage(tx, channel, msg.MessageID); err != nil {
		return err
	}

	return tx.Commit()
}

func addPendingSignatures(tx *sql.Tx, msg *message.Message) error {
	rows, err := tx.Query("SELECT witness, signature "+
		"FROM pendingSignatures "+
		"WHERE messageID = ?", msg.MessageID)
	if err != nil {
		return err
	}

	for rows.Next() {
		var witness string
		var signature string
		if err = rows.Scan(&witness, &signature); err != nil {
			return err
		}
		msg.WitnessSignatures = append(msg.WitnessSignatures, message.WitnessSignature{
			Witness:   witness,
			Signature: signature,
		})
	}

	if err = rows.Err(); err != nil {
		return err
	}

	_, err = tx.Exec("DELETE "+
		"FROM pendingSignatures "+
		"WHERE messageID = ?", msg.MessageID)
	return err
}

func storeInbox(tx *sql.Tx, msg message.Message, channel string) error {
	storedTime := time.Now().UnixNano()

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return err
	}

	_, err = tx.Exec(`
        INSERT INTO inbox (messageID, message, storedTime, baseChannel)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(messageID) DO UPDATE SET
        storedTime = excluded.storedTime,
        baseChannel = excluded.baseChannel
        WHERE baseChannel LIKE excluded.baseChannel || '%'
    `, msg.MessageID, msgByte, storedTime, channel)
	return err
}

func storeChannelMessage(tx *sql.Tx, channel string, messageID string) error {
	_, err := tx.Exec("INSERT INTO channelMessage "+
		"(channel, messageID) VALUES "+
		"(?, ?)", channel, messageID)
	return err
}

// AddWitnessSignature stores a pending signature inside the SQLite database.
func (s *SQLite) AddWitnessSignature(messageID string, witness string, signature string) error {
	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	witnessSignature, err := json.Marshal(message.WitnessSignature{
		Witness:   witness,
		Signature: signature,
	})
	if err != nil {
		return err
	}

	res, err := tx.Exec("UPDATE OR IGNORE inbox "+
		"SET message = jsonb_insert(message,'$.WitnessSignature[#]', ?) "+
		"WHERE messageID = ?", witnessSignature, messageID)
	if err != nil {
		return err
	}
	changes, err := res.RowsAffected()
	if err != nil {
		return err
	}
	if changes == 0 {
		_, err := tx.Exec("INSERT INTO pendingSignatures "+
			"(messageID, witness, signature) VALUES "+
			"(?, ?, ?)", messageID, witness, signature)
		if err != nil {
			return err
		}
	}
	return tx.Commit()
}

func (s *SQLite) HasChannel(ChannelPath string) (bool, error) {
	var channel string
	err := s.database.QueryRow("SELECT channel from channels WHERE channelsID = ?)", ChannelPath).Scan(&channel)
	return !errors.Is(err, sql.ErrNoRows), err
}

func (s *SQLite) HasMessage(messageID string) (bool, error) {
	var msgID string
	err := s.database.QueryRow("SELECT messageID from messages WHERE messageID = ?)", messageID).Scan(&msgID)
	return !errors.Is(err, sql.ErrNoRows), err
}

func (s *SQLite) GetOwnerPubKey() (kyber.Point, error) {
	var pubKeyBuf []byte
	err := s.database.QueryRow("SELECT ownerPubKey from configuration").Scan(&pubKeyBuf)
	if err != nil {
		return nil, err
	}
	ownerPubKey := crypto.Suite.Point()
	err = ownerPubKey.UnmarshalBinary(pubKeyBuf)

	return ownerPubKey, err
}

// StoreChannel stores a channel that is not an election inside the SQLite database.
func (s *SQLite) StoreChannel(channel string, organizerPubKey []byte) error {
	_, err := s.database.Exec("INSERT INTO channels (channel, organizerPubKey) "+
		"VALUES (?,?)", channel, organizerPubKey)
	return err
}

// GetClientServerAddress returns the client address of the server.
func (s *SQLite) GetClientServerAddress() (string, error) {
	var address string
	err := s.database.QueryRow("SELECT clientServerAddress from configuration").Scan(&address)
	return address, err
}

// GetServerPubKey returns the public key of the server.
func (s *SQLite) GetServerPubKey() ([]byte, error) {
	var pubKeyBuf []byte
	err := s.database.QueryRow("SELECT serverPubKey from configuration").Scan(&pubKeyBuf)
	if err != nil {
		return nil, err
	}
	return pubKeyBuf, err
}

// GetServerSecretKey returns the public key of the server.
func (s *SQLite) GetServerSecretKey() ([]byte, error) {
	var secretKeyBuf []byte
	err := s.database.QueryRow("SELECT serverSecretKey from configuration").Scan(&secretKeyBuf)
	if err != nil {
		return nil, err
	}
	return secretKeyBuf, err
}

// GetChannelType returns the type of the channelPath.
func (s *SQLite) GetChannelType(channel string) (string, error) {
	var name string
	err := s.database.QueryRow("SELECT name FROM channelType "+
		"JOIN channels on channels.typeID = channelType.ID "+
		"WHERE channel = ?", channel).Scan(&name)
	return name, err
}
