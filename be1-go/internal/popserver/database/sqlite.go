package database

import (
	"database/sql"
	"encoding/base64"
	"encoding/json"
	"errors"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
	_ "modernc.org/sqlite"
	"popstellar/crypto"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"strings"
	"time"
)

//const (
//	defaultPath = "sqlite.DB"
//)

// SQLite is a wrapper around the SQLite database.
type SQLite struct {
	Repository
	database *sql.DB
}

var channelTypeNameToID = map[string]string{
	"root":         "1",
	"lao":          "2",
	"election":     "3",
	"chirp":        "4",
	"reaction":     "5",
	"consensus":    "6",
	"popcha":       "7",
	"coin":         "8",
	"auth":         "9",
	"generalChirp": "10",
}
var channelTypeNames = []string{
	"root",
	"lao",
	"election",
	"chirp",
	"reaction",
	"consensus",
	"popcha",
	"coin",
	"auth",
	"generalChirp",
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
		"channelPath TEXT, " +
		"typeID TEXT, " +
		"laoID TEXT NULL, " +
		"FOREIGN KEY (laoID) REFERENCES channel(channelPath), " +
		"FOREIGN KEY (typeID) REFERENCES channelType(ID), " +
		"PRIMARY KEY (channelPath) " +
		")")
	return err
}

func createChannelMessage(tx *sql.Tx) error {
	_, err := tx.Exec("CREATE TABLE IF NOT EXISTS channelMessage (" +
		"channelPath TEXT, " +
		"messageID TEXT, " +
		"isBaseChannel BOOLEAN, " +
		"FOREIGN KEY (messageID) REFERENCES inbox(messageID), " +
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
		"name TEXT" +
		")")

	for _, channelType := range channelTypeNames {
		_, err = tx.Exec("INSERT INTO channelType (name) VALUES (?)", channelType)
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
		"privateKey BLOB NULL, " +
		"FOREIGN KEY (channelPath) REFERENCES channel(channelPath), " +
		"PRIMARY KEY (channelPath) " +
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
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage "+
		"(channelPath, messageID, isBaseChannel) VALUES "+
		"(?, ?, ?)", channel, msg.MessageID, true)
	if err != nil {
		return err
	}
	return tx.Commit()
}

func (s *SQLite) StoreMessageAndData(channelPath string, msg message.Message) error {
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
		"(?, ?, ?, ?)", msg.MessageID, msgByte, messageData, time.Now().UnixNano())
	if err != nil {
		return err

	}
	_, err = tx.Exec("INSERT INTO channelMessage "+
		"(channelPath, messageID, isBaseChannel) VALUES "+
		"(?, ?, ?)", channelPath, msg.MessageID, true)
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
func (s *SQLite) StoreChannel(channelPath, typeName, laoID string) error {
	_, err := s.database.Exec("INSERT INTO channel (channelPath, typeID, laoID) VALUES (?, ?, ?)",
		channelPath, channelTypeNameToID[typeName], laoID)
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
		"WHERE channelPath = ?", channel).Scan(&name)
	return name, err
}

// GetAllMessagesFromChannel returns all the messages received + sent on a channel sorted by stored time.
func (s *SQLite) GetAllMessagesFromChannel(channelPath string) ([]message.Message, error) {

	rows, err := s.database.Query("SELECT inbox.message "+
		"FROM inbox "+
		"JOIN channelMessage ON inbox.messageID = channelMessage.messageID "+
		"WHERE channelMessage.channelPath = ? "+
		"ORDER BY inbox.storedTime DESC", channelPath)
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

	rows, err := s.database.Query("SELECT message, channelPath "+
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

	rows, err := s.database.Query("SELECT inbox.messageID, channelPath "+
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
	err := s.database.QueryRow("SELECT channelPath from channel WHERE channelPath = ?", channel).Scan(&c)
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
	if err != nil {
		return err
	}

	storedTime := time.Now().UnixNano()
	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)", msg.MessageID, msgByte, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID, isBaseChannel) VALUES (?, ?, ?)", "/root", msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channel (channelPath, typeID, laoID) VALUES (?, ?, ?)", laoID, "lao", laoID)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID, isBaseChannel) VALUES (?, ?, ?)", laoID, msg.MessageID, false)
	if err != nil {
		return err
	}
	for channel, channelType := range channels {
		_, err = tx.Exec("INSERT INTO channel (channelPath, typeID, laoID) VALUES (?, ?, ?)",
			channel, channelTypeNameToID[channelType], laoID)
		if err != nil {
			return err
		}
	}
	_, err = tx.Exec("INSERT INTO key (channelPath, publicKey) VALUES (?, ?)", laoID, organizerPubBuf)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)", laoGreetMsg.MessageID, laoGreetMsgByte, laoGreetData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID, isBaseChannel) VALUES (?, ?, ?)", laoID, laoGreetMsg.MessageID, true)
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
	err := s.database.QueryRow("SELECT publicKey FROM key WHERE channelPath = ?", laoID).Scan(&organizerPubBuf)
	if err != nil {
		return nil, err
	}
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
			" WHERE json_extract(messageData, '$.object') = ? AND channelPath= ?)",
		messagedata.RollCallObject, channel).Scan(&state)
	if err != nil {
		return "", err
	}
	return state, nil
}

func (s *SQLite) CheckPrevID(channel, nextID, expectedState string) (bool, error) {
	var lastMsg []byte
	err := s.database.QueryRow("SELECT messageData"+
		" FROM inbox"+
		" WHERE storedTime= (SELECT MAX(storedTime)"+
		" FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		" WHERE json_extract(messageData, '$.object') = ? AND channelPath = ?)", messagedata.RollCallObject, channel).Scan(&lastMsg)

	if err != nil {
		return false, err
	}

	switch expectedState {
	case messagedata.LAOActionCreate:
		var laoCreate messagedata.LaoCreate
		err = json.Unmarshal(lastMsg, &laoCreate)
		if err != nil {
			var unmarshalTypeError *json.UnmarshalTypeError
			if errors.As(err, &unmarshalTypeError) {
				return false, nil
			}
			return false, err
		}
		return laoCreate.ID == nextID, nil

	case messagedata.RollCallActionOpen:
		var rollCallOpen messagedata.RollCallOpen
		err = json.Unmarshal(lastMsg, &rollCallOpen)
		if err != nil {
			var unmarshalTypeError *json.UnmarshalTypeError
			if errors.As(err, &unmarshalTypeError) {
				return false, nil
			}
			return false, err
		}
		return rollCallOpen.UpdateID == nextID, nil

	case messagedata.RollCallActionClose:
		var rollCallClose messagedata.RollCallClose
		err = json.Unmarshal(lastMsg, &rollCallClose)
		if err != nil {
			var unmarshalTypeError *json.UnmarshalTypeError
			if errors.As(err, &unmarshalTypeError) {
				return false, nil
			}
			return false, err
		}
		return rollCallClose.UpdateID == nextID, nil

	default:
		return false, xerrors.New("unexpected state")
	}
}

func (s *SQLite) GetLaoWitnesses(laoID string) (map[string]struct{}, error) {

	var witnesses []string
	err := s.database.QueryRow("SELECT json_extract(messageData, '$.witnesses')"+
		" FROM (select * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		" WHERE channelPath = ? AND json_extract(messageData, '$.object') = ? AND json_extract(messageData, '$.action') = ?",
		laoID, messagedata.LAOObject, messagedata.LAOActionCreate).Scan(&witnesses)
	if err != nil {
		return nil, err
	}

	var witnessesMap = make(map[string]struct{})
	for _, witness := range witnesses {
		witnessesMap[witness] = struct{}{}
	}

	return witnessesMap, nil
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
		msg.MessageID, msgBytes, messageData, time.Now().UnixNano())
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID, isBaseChannel) VALUES (?, ?, ?)",
		laoID, msg.MessageID, true)
	if err != nil {
		return err
	}
	for _, channel := range channels {
		_, err = tx.Exec("INSERT INTO channel (channelPath, typeID, laoID) VALUES (?, ?, ?)",
			channel, "channel", laoID)
		if err != nil {
			return err
		}
	}
	err = tx.Commit()
	if err != nil {
		return err
	}
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
	electionKey, err := base64.URLEncoding.DecodeString(electionKeyMsg.Data)
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
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID, isBaseChannel) VALUES (?, ?, ?)",
		laoID, msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channel (channelPath, typeID, laoID) VALUES (?, ?, ?)",
		electionID, "election", laoID)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID, isBaseChannel) VALUES (?, ?, ?)",
		electionID, msg.MessageID, false)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO key (channelPath, publicKey, privateKey) VALUES (?, ?, ?)",
		electionID, electionPubBuf, electionSecretBuf)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)",
		electionKeyMsg.MessageID, electionKeyMsgBytes, electionKey, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID) VALUES (?, ?)",
		electionID, electionKeyMsg.MessageID)
	if err != nil {
		return err
	}
	err = tx.Commit()
	if err != nil {
		return err
	}
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
	err = tx.QueryRow("SELECT laoID FROM channel WHERE channelPath = ?", electionID).Scan(&laoID)
	if err != nil {
		return nil, err
	}

	var electionPubBuf []byte
	err = tx.QueryRow("SELECT publicKey FROM key WHERE channelPath = ?", laoID).Scan(&electionPubBuf)
	if err != nil {
		return nil, err
	}
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

func (s *SQLite) GetElectionSecretKey(electionID string) (kyber.Scalar, error) {
	var electionSecretBuf []byte
	err := s.database.QueryRow("SELECT privateKey FROM key WHERE channelPath = ?", electionID).Scan(&electionSecretBuf)
	if err != nil {
		return nil, err
	}

	electionSecretKey := crypto.Suite.Scalar()
	err = electionSecretKey.UnmarshalBinary(electionSecretBuf)
	if err != nil {
		return nil, err
	}
	return electionSecretKey, nil
}

func (s *SQLite) getElectionStateCounter(electionID string) (string, error) {
	var state string
	err := s.database.QueryRow("SELECT json_extract(messageData, '$.action')"+
		" FROM inbox"+
		" WHERE storedTime = (SELECT MAX(storedTime)"+
		" FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		" WHERE channelPath = ?)", electionID).Scan(&state)

	if err != nil && !errors.Is(err, sql.ErrNoRows) {
		return "", err
	}
	return state, nil
}

func (s *SQLite) IsElectionStartedOrEnded(electionID string) (bool, error) {
	state, err := s.getElectionStateCounter(electionID)
	if err != nil {
		return false, err
	}

	return state == messagedata.ElectionActionOpen || state == messagedata.ElectionActionEnd, nil
}

func (s *SQLite) IsElectionStarted(electionID string) (bool, error) {
	state, err := s.getElectionStateCounter(electionID)
	if err != nil {
		return false, err
	}
	return state == messagedata.ElectionActionOpen, nil
}

func (s *SQLite) IsElectionEnded(electionID string) (bool, error) {
	state, err := s.getElectionStateCounter(electionID)
	if err != nil {
		return false, err
	}

	return state == messagedata.ElectionActionEnd, nil
}

func (s *SQLite) GetElectionCreationTime(electionID string) (int64, error) {
	var creationTime int64
	err := s.database.QueryRow("SELECT json_extract(messageData, '$.created_at')"+
		"FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		"WHERE channelPath = ? AND json_extract(messageData, '$.object') = ? AND json_extract(messageData, '$.action') = ?",
		electionID, messagedata.ElectionObject, messagedata.ElectionActionSetup).Scan(&creationTime)

	if err != nil {
		return 0, err
	}
	return creationTime, nil
}

func (s *SQLite) GetElectionType(electionID string) (string, error) {
	var electionType string
	err := s.database.QueryRow("SELECT json_extract(messageData, '$.version')"+
		"FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		"WHERE channelPath = ? AND json_extract(messageData, '$.object') = ? AND json_extract(messageData, '$.action') = ?",
		electionID, messagedata.ElectionObject, messagedata.ElectionActionSetup).Scan(&electionType)

	if err != nil {
		return "", err
	}
	return electionType, nil
}

func (s *SQLite) GetElectionAttendees(electionID string) (map[string]struct{}, error) {
	var rollCallCloseBytes []byte
	err := s.database.QueryRow(`
	SELECT joined.messageData
	FROM (
		SELECT * FROM inbox
		JOIN channelMessage ON inbox.messageID = channelMessage.messageID
	) joined
	JOIN channel c ON joined.channelPath = c.laoID
	WHERE c.channelPath = ? 
	AND json_extract(joined.messageData, '$.object') = ? 
	AND json_extract(joined.messageData, '$.action') = ?
	AND joined.storedTime = (
		SELECT MAX(storedTime)
		FROM (
			SELECT * FROM inbox
			JOIN channelMessage ON inbox.messageID = channelMessage.messageID
		)
		WHERE channelPath = c.laoID 
		AND json_extract(messageData, '$.object') = ? 
		AND json_extract(messageData, '$.action') = ?
	)`,
		electionID,
		messagedata.RollCallObject,
		messagedata.RollCallActionClose,
		messagedata.RollCallObject,
		messagedata.RollCallActionClose,
	).Scan(&rollCallCloseBytes)
	if err != nil {
		return nil, err
	}

	var rollCallClose messagedata.RollCallClose
	err = json.Unmarshal(rollCallCloseBytes, &rollCallClose)
	if err != nil {
		return nil, err
	}

	attendeesMap := make(map[string]struct{})
	for _, attendee := range rollCallClose.Attendees {
		attendeesMap[attendee] = struct{}{}
	}
	return attendeesMap, nil
}

func (s *SQLite) getElectionSetup(electionID string, tx *sql.Tx) (messagedata.ElectionSetup, error) {
	var electionSetupBytes []byte
	err := tx.QueryRow("SELECT messageData"+
		" FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		" WHERE channelPath = ? AND json_extract(messageData, '$.object') = ? AND json_extract(messageData, '$.action') = ?",
		electionID, messagedata.ElectionObject, messagedata.ElectionActionSetup).Scan(&electionSetupBytes)
	if err != nil {
		return messagedata.ElectionSetup{}, err
	}

	var electionSetup messagedata.ElectionSetup
	err = json.Unmarshal(electionSetupBytes, &electionSetup)
	if err != nil {
		return messagedata.ElectionSetup{}, err
	}
	return electionSetup, nil

}

func (s *SQLite) GetElectionQuestions(electionID string) (map[string]types.Question, error) {
	tx, err := s.database.Begin()
	if err != nil {
		return nil, err

	}
	defer tx.Rollback()

	electionSetup, err := s.getElectionSetup(electionID, tx)
	if err != nil {
		return nil, err

	}
	questions, err := getQuestionsFromMessage(electionSetup)
	if err != nil {
		return nil, err
	}

	err = tx.Commit()
	if err != nil {
		return nil, err

	}
	return questions, nil
}

func (s *SQLite) GetElectionQuestionsWithValidVotes(electionID string) (map[string]types.Question, error) {

	tx, err := s.database.Begin()
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	electionSetup, err := s.getElectionSetup(electionID, tx)
	if err != nil {
		return nil, err
	}
	questions, err := getQuestionsFromMessage(electionSetup)
	if err != nil {
		return nil, err
	}

	rows, err := tx.Query("SELECT messageData, messageID, json_extract(message, '$.sender')"+
		" FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		" WHERE channelPath = ? AND json_extract(messageData, '$.object') = ? AND json_extract(messageData, '$.action') = ?",
		electionID, messagedata.ElectionObject, messagedata.VoteActionCastVote)

	if err != nil {
		return nil, err
	}

	for rows.Next() {
		var voteBytes []byte
		var msgID string
		var sender string
		if err = rows.Scan(&voteBytes, &msgID, &sender); err != nil {
			return nil, err
		}
		var vote messagedata.VoteCastVote
		err = json.Unmarshal(voteBytes, &vote)
		if err != nil {
			return nil, err
		}
		err = updateVote(msgID, sender, vote, questions)
		if err != nil {
			return nil, err
		}
	}
	if err = rows.Err(); err != nil {
		return nil, err
	}
	err = tx.Commit()
	if err != nil {
		return nil, err
	}
	return questions, nil
}

func getQuestionsFromMessage(electionSetup messagedata.ElectionSetup) (map[string]types.Question, error) {
	questions := make(map[string]types.Question)
	for _, question := range electionSetup.Questions {
		ballotOptions := make([]string, len(question.BallotOptions))
		copy(ballotOptions, question.BallotOptions)
		_, ok := questions[question.ID]
		if ok {
			return nil, xerrors.Errorf("duplicate question ID")
		}
		questions[question.ID] = types.Question{
			ID:            []byte(question.ID),
			BallotOptions: ballotOptions,
			ValidVotes:    make(map[string]types.ValidVote),
			Method:        question.VotingMethod,
		}
	}
	return questions, nil
}

func updateVote(msgID, sender string, castVote messagedata.VoteCastVote, questions map[string]types.Question) error {
	for idx, vote := range castVote.Votes {
		question, ok := questions[vote.Question]
		if !ok {
			return xerrors.Errorf("question not found for vote number %d sent by %s", idx, sender)
		}
		earlierVote, ok := question.ValidVotes[sender]
		if !ok || earlierVote.VoteTime < castVote.CreatedAt {
			question.ValidVotes[sender] = types.ValidVote{
				MsgID:    msgID,
				ID:       vote.ID,
				VoteTime: castVote.CreatedAt,
				Index:    vote.Vote,
			}
		}
	}
	return nil
}

func (s *SQLite) StoreMessageAndElectionResult(channelPath string, msg, electionResultMsg message.Message) error {
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
	electionResult, err := base64.URLEncoding.DecodeString(electionResultMsg.Data)
	if err != nil {
		return err
	}
	electionResultMsgBytes, err := json.Marshal(electionResultMsg)
	if err != nil {
		return err
	}
	storedTime := time.Now().UnixNano()

	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)",
		msg.MessageID, msgBytes, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID, isBaseChannel) VALUES (?, ?, ?)",
		channelPath, msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)",
		electionResultMsg.MessageID, electionResultMsgBytes, electionResult, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID) VALUES (?, ?)",
		channelPath, electionResultMsg.MessageID)
	if err != nil {
		return err
	}
	err = tx.Commit()
	return err
}

//======================================================================================================================
// ChirpRepository interface implementation
//======================================================================================================================

func (s *SQLite) StoreChirpMessages(channel, generalChannel string, msg, generalMsg message.Message) error {
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
	generalMsgBytes, err := json.Marshal(generalMsg)
	if err != nil {
		return err
	}
	generalMessageData, err := base64.URLEncoding.DecodeString(generalMsg.Data)
	if err != nil {
		return err
	}
	storedTime := time.Now().UnixNano()

	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)",
		msg.MessageID, msgBytes, messageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID, isBaseChannel) VALUES (?, ?, ?)",
		channel, msg.MessageID, true)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO inbox (messageID, message, messageData, storedTime) VALUES (?, ?, ?, ?)",
		generalMsg.MessageID, generalMsgBytes, generalMessageData, storedTime)
	if err != nil {
		return err
	}
	_, err = tx.Exec("INSERT INTO channelMessage (channelPath, messageID) VALUES (?, ?)",
		generalChannel, generalMsg.MessageID)
	if err != nil {
		return err
	}
	err = tx.Commit()
	return err
}

//======================================================================================================================
// ReactionRepository interface implementation
//======================================================================================================================

func (s *SQLite) IsAttendee(laoPath, poptoken string) (bool, error) {

	var rollCallCloseBytes []byte

	err := s.database.QueryRow("SELECT messageData"+
		" FROM inbox"+
		" WHERE storedTime = (SELECT MAX(storedTime)"+
		" FROM (SELECT * FROM inbox JOIN channelMessage ON inbox.messageID = channelMessage.messageID)"+
		" WHERE channelPath = ? AND json_extract(messageData, '$.object') = ? AND json_extract(messageData, '$.action') = ?)",
		laoPath, messagedata.RollCallObject, messagedata.RollCallActionClose).Scan(&rollCallCloseBytes)

	if err != nil {
		return false, err
	}

	var rollCallClose messagedata.RollCallClose
	err = json.Unmarshal(rollCallCloseBytes, &rollCallClose)
	if err != nil {
		return false, err
	}

	for _, attendee := range rollCallClose.Attendees {
		if attendee == poptoken {
			return true, nil
		}
	}

	return false, nil
}

func (s *SQLite) GetReactionSender(messageID string) (string, error) {
	var sender string
	var object string
	var action string
	err := s.database.QueryRow("SELECT json_extract(message, '$.sender'), json_extract(messageData, '$.object'), json_extract(messageData, '$.action')"+
		" FROM inbox"+
		" WHERE messageID = ?", messageID).Scan(&sender, &object, &action)
	if err != nil && errors.Is(err, sql.ErrNoRows) {
		return "", nil
	} else if err != nil {
		return "", err

	}

	if object != messagedata.ReactionObject || action != messagedata.ReactionActionAdd {
		return "", xerrors.New("unexpected object or action")
	}
	return sender, nil
}
