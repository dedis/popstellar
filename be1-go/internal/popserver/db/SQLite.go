package db

import (
	"database/sql"
	"encoding/json"
	"errors"
	"go.dedis.ch/kyber/v3"
	_ "modernc.org/sqlite"
	"popstellar/crypto"
	"popstellar/internal/popserver/repo"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strings"
	"time"
)

const (
	defaultPath = "sqlite.DB"
)

// SQLite is a wrapper around the SQLite database.
type SQLite struct {
	repo.Repository
	database *sql.DB
}

//======================================================================================================================
// Database initialization
//======================================================================================================================

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

	err = createChannel(tx)
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

	err = createKey(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createTokenLao(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createToken(tx)
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
		"ClientServerAddress TEXT NULL, " +
		"ServerServerAddress TEXT NULL " +
		"serverPubKey BLOB NULL, " +
		"serverSecretKey BLOB NULL, " +
		")")
	return err
}

func createInbox(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS inbox (" +
		"messageID TEXT, " +
		"object TEXT NULL," +
		"action TEXT NULL, " +
		"message jsonb, " +
		"storedTime BIGINT, " +
		"PRIMARY KEY (messageID) " +
		")")
	return err
}

func createChannelMessage(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS channelMessage (" +
		"channel TEXT, " +
		"messageID TEXT, " +
		"isBaseChannel BOOLEAN, " +
		"FOREIGN KEY (messageID) REFERENCES inbox(messageID), " +
		"FOREIGN KEY (channel) REFERENCES channels(channel), " +
		"PRIMARY KEY (channel, messageID) " +
		")")
	return err
}

func createChannel(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS channel (" +
		"channelID TEXT, " +
		"typeID TEXT, " +
		"laoID TEXT NULL, " +
		"FOREIGN KEY (laoID) REFERENCES channel(channelID), " +
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
		"ID TEX, " +
		"name TEXT, " +
		"PRIMARY KEY (ID) " +
		")")
	return err
}

func createKey(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS key (" +
		"channelID TEXT, " +
		"publicKey BLOB NULL, " +
		"privateKey BLOB NULL, " +
		"PRIMARY KEY (channelID) " +
		")")
	return err
}

func createTokenLao(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS tokenLao (" +
		"laoID TEXT, " +
		"token TEXT, " +
		"creationTime BIGINT, " +
		"PRIMARY KEY (laoID, token) " +
		")")
	return err
}

func createToken(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS token (" +
		"token TEXT, " +
		"PRIMARY KEY (token) " +
		")")
	return err
}

// Close closes the SQLite database.
func (s *SQLite) Close() error {
	return s.database.Close()
}

//======================================================================================================================
// Repository interface implementation
//======================================================================================================================

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

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO inbox "+
		"(messageID, message, storedTime) VALUES "+
		"(?, ?, ?)", msg.MessageID, msgByte, time.Now().UnixNano())

	_, err = tx.Exec("INSERT INTO channelMessage "+
		"(channel, messageID, isBaseChannel) VALUES "+
		"(?, ?, ?)", channel, msg.MessageID, false)
	return tx.Commit()
}

func (s *SQLite) StoreMessageWithObjectAction(channelID, object, action string, msg message.Message) error {
	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if err = addPendingSignatures(tx, &msg); err != nil {
		return err
	}

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO inbox "+
		"(messageID, object, action, message, storedTime) VALUES "+
		"(?, ?, ?, ?, ?)", msg.MessageID, object, action, msgByte, time.Now().UnixNano())

	_, err = tx.Exec("INSERT INTO channelMessage "+
		"(channel, messageID, isBaseChannel) VALUES "+
		"(?, ?, ?)", channelID, msg.MessageID, false)
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

// GetMessagesByID returns a set of messages by their IDs.
func (s *SQLite) GetMessagesByID(IDs []string) (map[string]message.Message, error) {

	IDsInterface := make([]interface{}, len(IDs))
	for i, v := range IDs {
		IDsInterface[i] = v
	}
	rows, err := s.database.Query("SELECT messageID, message "+
		"FROM inbox "+
		"WHERE messageID IN ("+strings.Repeat("?,", len(IDs)-1)+"?"+")", IDsInterface...)
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

// StoreChannel stores a channel that is not an election inside the SQLite database.
func (s *SQLite) StoreChannel(channel string, organizerPubKey []byte) error {
	_, err := s.database.Exec("INSERT INTO channels (channel, organizerPubKey) "+
		"VALUES (?,?)", channel, organizerPubKey)
	return err
}

// GetChannelType returns the type of the channelPath.
func (s *SQLite) GetChannelType(channel string) (string, error) {
	var name string
	err := s.database.QueryRow("SELECT name FROM channelType "+
		"JOIN channels on channels.typeID = channelType.ID "+
		"WHERE channel = ?", channel).Scan(&name)
	return name, err
}

//======================================================================================================================
// QueryRepository interface implementation
//======================================================================================================================

//======================================================================================================================
// ChannelRepository interface implementation
//======================================================================================================================

func (s *SQLite) HasChannel(channel string) (bool, error) {
	var c string
	err := s.database.QueryRow("SELECT channel from channels WHERE channelID = ?)", channel).Scan(&c)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return false, err
	} else {
		return true, nil
	}
}

func (s *SQLite) HasMessage(messageID string) (bool, error) {
	var msgID string
	err := s.database.QueryRow("SELECT messageID from inbox WHERE messageID = ?)", messageID).Scan(&msgID)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return false, err
	} else {
		return true, nil
	}
}

//======================================================================================================================
// RootRepository interface implementation
//======================================================================================================================

// ChannelExists returns true if the channel already exists.
func (s *SQLite) channelExists(channel string) (bool, error) {
	var channelID string
	err := s.database.QueryRow("SELECT channelID FROM channel WHERE channelID = ?", channel).Scan(&channelID)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return false, nil
	} else if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return false, err
	} else {
		return true, nil
	}
}

func (s *SQLite) StoreChannelsAndMessageWithLaoGreet(
	channels map[string]string,
	laoID string,
	organizerPubBuf []byte,
	msg, laoGreetMsg message.Message) error {

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO INBOX (messageID, message, storedTime) VALUES (?, ?, ?)", msg.MessageID, msg, time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channel, messageID, isBaseChannel) VALUES (?, ?, ?)", "/root", msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channel (channelID, typeID, laoID) VALUES (?, ?, ?)", laoID, "lao", laoID)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channel, messageID, isBaseChannel) VALUES (?, ?)", laoID, msg.MessageID, false)
	if err != nil {
		return err
	}
	for channel, channelType := range channels {
		_, err = tx.Exec("INSERT INTO channel (channelID, typeID, laoID) VALUES (?, ?, ?)", channel, channelType, laoID)
		if err != nil {
			return err
		}
	}
	_, err = tx.Exec("INSERT INTO key (channelID, publicKey) VALUES (?, ?)", laoID, organizerPubBuf)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO INBOX (messageID, message, storedTime) VALUES (?, ?, ?)", laoGreetMsg.MessageID, laoGreetMsg, time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channel, messageID) VALUES (?, ?)", laoID, laoGreetMsg.MessageID)
	if err != nil {
		return err
	}

	err = tx.Commit()
	if err != nil {
		return err
	}

	defer tx.Rollback()
	return nil
}

//======================================================================================================================
// LaoRepository interface implementation
//======================================================================================================================

// GetLaoWitnesses returns the witnesses of the LAO.
func (s *SQLite) GetLaoWitnesses(laoID string) (map[string]struct{}, error) {
	tx, err := s.database.Begin()
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	rows, err := tx.Query("SELECT token FROM tokenLao WHERE laoID = ? AND "+
		"creationTime = SELECT MAX(storedTime) FROM tokenLao WHERE laoID = ?", laoID, laoID)
	if err != nil {
		return nil, err
	}

	witnesses := make(map[string]struct{})
	for rows.Next() {
		var token string
		if err = rows.Scan(&token); err != nil {
			return nil, err
		}
		witnesses[token] = struct{}{}
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}
	return witnesses, nil
}

func (s *SQLite) GetOrganizerPubKey(laoID string) (kyber.Point, error) {
	var organizerPubBuf []byte
	err := s.database.QueryRow("SELECT publicKey FROM key WHERE channelID = ?", laoID).Scan(&organizerPubBuf)
	organizerPubKey := crypto.Suite.Point()
	err = organizerPubKey.UnmarshalBinary(organizerPubBuf)
	if err != nil {
		return nil, err
	}
	return organizerPubKey, nil
}

func (s *SQLite) GetRollCallState(channel string) (string, error) {
	var state string
	err := s.database.QueryRow("SELECT action FROM inbox"+
		" JOIN channelMessage ON inbox.messageID = channelMessage.messageID WHERE channelID = ?)"+
		" WHERE storedTime= SELECT MAX(storedTime) FROM inbox WHERE object= ?",
		channel, messagedata.RollCallObject).Scan(&state)
	if err != nil {
		return "", err
	}
	return state, nil
}

func (s *SQLite) CheckPrevID(channel string, nextID string) (bool, error) {
	var lastMsg []byte
	var object string
	var action string
	err := s.database.QueryRow("SELECT message, object, action FROM inbox"+
		" JOIN channelMessage ON inbox.messageID = channelMessage.messageID WHERE channelID = ?)"+
		" WHERE storedTime= SELECT MAX(storedTime) FROM inbox WHERE object= ?",
		channel, messagedata.RollCallObject).Scan(&lastMsg, object, action)
	if err != nil {
		return false, err
	}

	var prevID struct {
		PrevID string `json:"update_id"`
	}
	var ID struct {
		ID string `json:"id"`
	}
	err = json.Unmarshal(lastMsg, &prevID)
	if err != nil {
		err = json.Unmarshal(lastMsg, &ID)
		if err != nil {
			return false, nil
		}
		return ID.ID == nextID, nil
	}
	return prevID.PrevID == nextID, nil
}

func (s *SQLite) StoreChannelsAndMessage(channels []string, laoID string, msg message.Message) error {
	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	_, err = tx.Exec("INSERT INTO INBOX (messageID, message, storedTime) VALUES (?, ?, ?)",
		msg.MessageID, msg, time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channel, messageID, isBaseChannel) VALUES (?, ?, ?)",
		laoID, msg.MessageID, true)
	if err != nil {
		return err
	}
	for _, channel := range channels {
		_, err = tx.Exec("INSERT INTO channel (channelID, typeID, laoID) VALUES (?, ?, ?)",
			channel, "channel", laoID)
		if err != nil {
			return err
		}
	}
	err = tx.Commit()
	return nil

}

func (s *SQLite) StoreMessageWithElectionKey(
	laoID, electionID string,
	electionPubKey kyber.Point,
	electionSecretKey kyber.Scalar,
	msg, electionKeyMsg message.Message) error {

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	_, err = tx.Exec("INSERT INTO INBOX (messageID, message, storedTime) VALUES (?, ?, ?)",
		msg.MessageID, msg, time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channel, messageID, isBaseChannel) VALUES (?, ?, ?)",
		laoID, msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channel (channelID, typeID, laoID) VALUES (?, ?, ?)",
		electionID, "election", laoID)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channel, messageID, isBaseChannel) VALUES (?, ?, ?)",
		electionID, msg.MessageID, false)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO key (channelID, publicKey, privateKey) VALUES (?, ?)",
		electionID, electionPubKey, electionSecretKey)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO INBOX (messageID, message, storedTime) VALUES (?, ?, ?)",
		electionKeyMsg.MessageID, electionKeyMsg, time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channel, messageID) VALUES (?, ?)",
		electionID, electionKeyMsg.MessageID)
	if err != nil {
		return err
	}
	err = tx.Commit()
	return nil
}

//======================================================================================================================
// ElectionRepository interface implementation
//======================================================================================================================
