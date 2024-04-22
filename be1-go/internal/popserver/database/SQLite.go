package database

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"errors"
	"go.dedis.ch/kyber/v3"
	_ "modernc.org/sqlite"
	"popstellar/crypto"
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
	Repository
	database *sql.DB
}

var channelTypeNameToID = map[string]string{
	"root":         "1",
	"lao":          "2",
	"election":     "3",
	"generalchirp": "4",
	"chirp":        "5",
	"reaction":     "6",
	"consensus":    "7",
	"popcha":       "8",
	"coin":         "9",
	"auth":         "10",
}

//======================================================================================================================
// Database initialization
//======================================================================================================================

// NewSQLite returns a new SQLite instance.
func NewSQLite(path string, foreignKeyOff bool) (SQLite, error) {
	db, err := sql.Open("sqlite", path)
	if err != nil {
		return SQLite{}, err
	}

	if foreignKeyOff {
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

	err = createInbox(tx)
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
	err = createToken(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createChannel(tx)
	if err != nil {
		db.Close()
		return SQLite{}, err
	}

	err = createTokenLao(tx)
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

func createInbox(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS inbox (" +
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
		"channelID TEXT, " +
		"typeID TEXT, " +
		"laoID TEXT NULL, " +
		"FOREIGN KEY (laoID) REFERENCES channel(channelID), " +
		"FOREIGN KEY (typeID) REFERENCES channelType(ID), " +
		"PRIMARY KEY (channelID) " +
		")")
	return err
}

func createChannelMessage(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS channelMessage (" +
		"channelID TEXT, " +
		"messageID TEXT, " +
		"isBaseChannel BOOLEAN, " +
		"FOREIGN KEY (messageID) REFERENCES inbox(messageID), " +
		"FOREIGN KEY (channelID) REFERENCES channel(channelID), " +
		"PRIMARY KEY (channelID, messageID) " +
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
		"name TEXT" +
		")")
	channelTypes := []string{"root",
		"lao",
		"election",
		"generalchirp",
		"chirp",
		"reaction",
		"consensus",
		"popcha",
		"coin",
		"auth"}
	for _, channelType := range channelTypes {
		_, err = tx.Exec("INSERT INTO channelType (name) VALUES (?)", channelType)
		if err != nil {
			return err
		}
	}
	return err
}

func createKey(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS key (" +
		"channelID TEXT, " +
		"publicKey BLOB NULL, " +
		"privateKey BLOB NULL, " +
		"FOREIGN KEY (channelID) REFERENCES channel(channelID), " +
		"PRIMARY KEY (channelID) " +
		")")
	return err
}

func createTokenLao(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS tokenLao (" +
		"laoID TEXT, " +
		"token TEXT, " +
		"creationTime BIGINT, " +
		"FOREIGN KEY (laoID) REFERENCES channel(channelID), " +
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
		"(?, ?, ?)", msg.MessageID, string(msgByte), time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage "+
		"(channelID, messageID, isBaseChannel) VALUES "+
		"(?, ?, ?)", channel, msg.MessageID, true)
	if err != nil {
		return err
	}
	return tx.Commit()
}

func (s *SQLite) StoreMessageAndData(channelID string, msg message.Message) error {
	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if err = addPendingSignatures(tx, &msg); err != nil {
		return err
	}

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return err
	}

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO inbox "+
		"(messageID, message, messageData, storedTime) VALUES "+
		"(?, ?, ?, ?)", msg.MessageID, string(msgByte), string(messageData), time.Now().UnixNano())
	if err != nil {
		return err

	}
	_, err = tx.Exec("INSERT INTO channelMessage "+
		"(channelID, messageID, isBaseChannel) VALUES "+
		"(?, ?, ?)", channelID, msg.MessageID, true)
	if err != nil {
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

	if err = rows.Err(); err != nil {
		return nil, err
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
		"SET message = json_insert(message,'$.witness_signatures[#]', json(?)) "+
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

// StoreChannel mainly used for testing and storing the root channel
func (s *SQLite) StoreChannel(channelID, typeName, laoID string) error {
	_, err := s.database.Exec("INSERT INTO channel (channelID, typeID, laoID) VALUES (?, ?, ?)",
		channelID, channelTypeNameToID[typeName], laoID)
	return err
}

//======================================================================================================================
// QueryRepository interface implementation
//======================================================================================================================

// GetChannelType returns the type of the channelPath.
func (s *SQLite) GetChannelType(channel string) (string, error) {
	var name string
	err := s.database.QueryRow("SELECT name FROM channelType "+
		"JOIN channel on channel.typeID = channelType.ID "+
		"WHERE channelID = ?", channel).Scan(&name)
	return name, err
}

// GetAllMessagesFromChannel returns all the messages received + sent on a channel sorted by stored time.
func (s *SQLite) GetAllMessagesFromChannel(channelID string) ([]message.Message, error) {

	rows, err := s.database.Query("SELECT inbox.message "+
		"FROM inbox "+
		"JOIN channelMessage ON inbox.messageID = channelMessage.messageID "+
		"WHERE channelMessage.channelID = ? "+
		"ORDER BY inbox.storedTime DESC", channelID)
	if err != nil {
		return nil, err
	}

	var messages []message.Message
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

	if rows.Err() != nil {
		return nil, err
	}

	return messages, nil
}

func (s *SQLite) GetResultForGetMessagesByID(params map[string][]string) (map[string][]message.Message, error) {
	var interfaces []interface{}
	// isBaseChannel must be true
	interfaces = append(interfaces, true)
	for _, value := range params {
		for _, v := range value {
			interfaces = append(interfaces, v)
		}
	}

	rows, err := s.database.Query("SELECT message, channelID "+
		"FROM inbox JOIN channelMessage on inbox.messageID = channelMessage.messageID "+
		"WHERE isBaseChannel = ? "+
		"AND inbox.messageID IN ("+strings.Repeat("?,", len(interfaces)-2)+"?"+") ", interfaces...)
	if err != nil {
		return nil, err
	}

	result := make(map[string][]message.Message)
	for rows.Next() {
		var messageByte []byte
		var channel string
		if err = rows.Scan(&messageByte, &channel); err != nil {
			return nil, err
		}
		var msg message.Message
		if err = json.Unmarshal(messageByte, &msg); err != nil {
			return nil, err
		}
		result[channel] = append(result[channel], msg)
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}

	return result, nil
}

func (s *SQLite) GetParamsForGetMessageByID(params map[string][]string) (map[string][]string, error) {
	var interfaces []interface{}
	// isBaseChannel must be true
	interfaces = append(interfaces, true)
	for _, value := range params {
		for _, v := range value {
			interfaces = append(interfaces, v)
		}
	}

	rows, err := s.database.Query("SELECT inbox.messageID, channelID "+
		"FROM inbox JOIN channelMessage on inbox.messageID = channelMessage.messageID "+
		"WHERE isBaseChannel = ? "+
		"AND inbox.messageID IN ("+strings.Repeat("?,", len(interfaces)-2)+"?"+") ", interfaces...)
	if err != nil {
		return nil, err
	}

	result := make(map[string]struct{})
	for rows.Next() {
		var messageID string
		var channel string
		if err = rows.Scan(&messageID, &channel); err != nil {
			return nil, err
		}
		result[messageID] = struct{}{}
	}

	if err = rows.Err(); err != nil {
		return nil, err
	}

	missingIDs := make(map[string][]string)
	for channel, messageIDs := range params {
		for _, messageID := range messageIDs {
			if _, ok := result[messageID]; !ok {
				missingIDs[channel] = append(missingIDs[channel], messageID)
			}
		}
	}
	return missingIDs, nil
}

//======================================================================================================================
// ChannelRepository interface implementation
//======================================================================================================================

func (s *SQLite) HasChannel(channel string) (bool, error) {
	var c string
	err := s.database.QueryRow("SELECT channelID from channel WHERE channelID = ?", channel).Scan(&c)
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
	err := s.database.QueryRow("SELECT messageID from inbox WHERE messageID = ?", messageID).Scan(&msgID)
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

func (s *SQLite) StoreChannelsAndMessageWithLaoGreet(
	channels map[string]string,
	laoID string,
	organizerPubBuf []byte,
	msg, laoGreetMsg message.Message) error {

	tx, err := s.database.Begin()
	if err != nil {
		return err
	}

	msgByte, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	laoGreetMsgByte, err := json.Marshal(laoGreetMsg)
	if err != nil {
		return err
	}

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return err
	}
	laoGreetData, err := base64.URLEncoding.DecodeString(laoGreetMsg.Data)

	storedTime := time.Now().UnixNano()
	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)", msg.MessageID, msgByte, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelID, messageID, isBaseChannel) VALUES (?, ?, ?)", "/root", msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channel (channelID, typeID, laoID) VALUES (?, ?, ?)", laoID, "lao", laoID)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelID, messageID, isBaseChannel) VALUES (?, ?, ?)", laoID, msg.MessageID, false)
	if err != nil {
		return err
	}
	for channel, channelType := range channels {
		_, err = tx.Exec("INSERT INTO channel (channelID, typeID, laoID) VALUES (?, ?, ?)", channel, channelTypeNameToID[channelType], laoID)
		if err != nil {
			return err
		}
	}
	_, err = tx.Exec("INSERT INTO key (channelID, publicKey) VALUES (?, ?)", laoID, organizerPubBuf)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)", laoGreetMsg.MessageID, laoGreetMsgByte, laoGreetData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelID, messageID, isBaseChannel) VALUES (?, ?, ?)", laoID, laoGreetMsg.MessageID, true)
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
	err := s.database.QueryRow(
		"SELECT json_extract(messageData, '$.action')"+
			" FROM inbox"+
			" WHERE storedTime = (SELECT MAX(storedTime)"+
			" FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
			" WHERE json_extract(messageData, '$.object') = ? AND channelID = ?)", messagedata.RollCallObject, channel).Scan(&state)
	if err != nil {
		return "", err
	}
	return state, nil
}

func (s *SQLite) CheckPrevID(channel string, nextID string) (bool, error) {
	var lastMsg []byte
	err := s.database.QueryRow("SELECT messageData"+
		" FROM inbox"+
		" WHERE storedTime= (SELECT MAX(storedTime)"+
		" FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		" WHERE json_extract(messageData, '$.object') = ? AND channelID = ?)", messagedata.RollCallObject, channel).Scan(&lastMsg)

	if err != nil {
		return false, err
	}

	var prevID1 struct {
		PrevID1 string `json:"update_id"`
	}
	var prevID2 struct {
		PrevID2 string `json:"id"`
	}
	err = json.Unmarshal(lastMsg, &prevID1)
	if err != nil || prevID1.PrevID1 == "" {
		err = json.Unmarshal(lastMsg, &prevID2)
		if err != nil || prevID2.PrevID2 == "" {
			return false, nil
		}
		return prevID2.PrevID2 == nextID, nil
	}
	return prevID1.PrevID1 == nextID, nil
}

func (s *SQLite) GetLaoWitnesses(laoID string) ([]string, error) {

	var witnesses []string
	err := s.database.QueryRow("SELECT json_extract(messageData, '$.witnesses')"+
		" FROM (select * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		" WHERE channelID = ? AND json_extract(messageData, '$.object') = ? AND json_extract(messageData, '$.action') = ?",
		laoID, messagedata.LAOObject, messagedata.LAOActionCreate).Scan(&witnesses)
	if err != nil {
		return nil, err
	}

	return witnesses, nil
}

func (s *SQLite) StoreChannelsAndMessage(channels []string, laoID string, msg message.Message) error {
	tx, err := s.database.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return err
	}

	msgBytes, err := json.Marshal(msg)
	if err != nil {
		return err
	}

	_, err = tx.Exec("INSERT INTO INBOX (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)",
		msg.MessageID, string(msgBytes), string(messageData), time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelID, messageID, isBaseChannel) VALUES (?, ?, ?)",
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

	msgBytes, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	messageData, err := base64.URLEncoding.DecodeString(msg.Data)
	if err != nil {
		return err
	}
	electionKeyMsgBytes, err := json.Marshal(electionKeyMsg)
	if err != nil {
		return err
	}
	electionPubBuf, err := electionPubKey.MarshalBinary()
	if err != nil {
		return err
	}
	electionSecretBuf, err := electionSecretKey.MarshalBinary()
	if err != nil {
		return err
	}

	storedTime := time.Now().UnixNano()

	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)",
		msg.MessageID, msgBytes, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelID, messageID, isBaseChannel) VALUES (?, ?, ?)",
		laoID, msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channel (channelID, typeID, laoID) VALUES (?, ?, ?)",
		electionID, "election", laoID)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelID, messageID, isBaseChannel) VALUES (?, ?, ?)",
		electionID, msg.MessageID, false)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO key (channelID, publicKey, privateKey) VALUES (?, ?, ?)",
		electionID, electionPubBuf, electionSecretBuf)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO inbox (messageID, message, storedTime) VALUES (?, ?, ?)",
		electionKeyMsg.MessageID, electionKeyMsgBytes, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelID, messageID) VALUES (?, ?)",
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

func (s *SQLite) GetLAOOrganizerPubKey(electionID string) (kyber.Point, error) {

	tx, err := s.database.Begin()
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	var laoID string
	err = tx.QueryRow("SELECT laoID FROM channel WHERE channelID = ?", electionID).Scan(&laoID)
	if err != nil {
		return nil, err
	}

	var electionPubBuf []byte
	err = tx.QueryRow("SELECT publicKey FROM key WHERE channelID = ?", laoID).Scan(&electionPubBuf)
	electionPubKey := crypto.Suite.Point()
	err = electionPubKey.UnmarshalBinary(electionPubBuf)
	if err != nil {
		return nil, err
	}

	err = tx.Commit()
	if err != nil {
		return nil, err
	}

	return electionPubKey, nil
}

func (s *SQLite) IsElectionStartedOrTerminated(electionID string) (bool, error) {
	rows, err := s.database.Query("SELECT json_extract(messageData, '$.started')"+
		" FROM inbox"+
		" WHERE storedTime = (SELECT MAX(storedTime)"+
		" FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		" WHERE channelID = ? AND json_extract(messageData, '$.object') = ?"+
		" AND (json_extract(messageData, '$.action') = ? OR json_extract(messageData, '$.action') = ?))",
		electionID, messagedata.ElectionObject, messagedata.ElectionActionOpen, messagedata.ElectionActionEnd)

	if err != nil {
		return false, err
	}
	count := 0
	for rows.Next() {
		count++
	}
	if rows.Err() != nil {
		return false, err
	}
	if count >= 1 {
		return true, nil
	}
	return false, nil
}

func (s *SQLite) GetElectionCreationTime(electionID string) (int64, error) {
	var creationTime int64
	err := s.database.QueryRow("SELECT json_extract(messageData, '$.created_at')"+
		"FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		"WHERE channelID = ? AND json_extract(messageData, '$.object') = ? AND json_extract(messageData, '$.action') = ?",
		electionID, messagedata.ElectionObject, messagedata.ElectionActionSetup).Scan(&creationTime)

	if err != nil {
		return 0, err
	}
	return creationTime, nil
}
