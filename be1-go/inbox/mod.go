package inbox

import (
	"database/sql"
	be1_go "popstellar"
	"sort"
	"sync"
	"time"

	"golang.org/x/xerrors"

	_ "github.com/mattn/go-sqlite3"

	"popstellar/db/sqlite"
	"popstellar/message/answer"
	"popstellar/message/query/method/message"
)

const (
	dbPrepareErr  = "failed to prepare query: %v"
	dbParseRowErr = "failed to parse row: %v"
	dbRowIterErr  = "error in row iteration: %v"
	dbQueryRowErr = "failed to query rows: %v"
)

// messageInfo wraps a message with a stored time for sorting.
type messageInfo struct {
	message    message.Message
	storedTime int64
}

// Inbox represents an in-memory data store to record incoming messages.
type Inbox struct {
	mutex     sync.RWMutex
	msgs      map[string]*messageInfo
	channelID string
}

// NewInbox returns a new initialized inbox
func NewInbox(channelID string) *Inbox {
	return &Inbox{
		mutex:     sync.RWMutex{},
		msgs:      make(map[string]*messageInfo),
		channelID: channelID,
	}
}

// AddWitnessSignature adds a signature of witness to a message of ID
// `messageID`. if the signature was correctly added return true otherwise
// returns false
func (i *Inbox) AddWitnessSignature(messageID string, public string, signature string) error {
	log := be1_go.Logger

	msg, ok := i.GetMessage(messageID)
	if !ok {
		// TODO: We received a witness signature before the message itself. We
		// ignore it for now but it might be worth keeping it until we actually
		// receive the message
		return answer.NewErrorf(-4, "failed to find message_id %q for witness message", messageID)
	}

	i.mutex.Lock()
	defer i.mutex.Unlock()

	msg.WitnessSignatures = append(msg.WitnessSignatures, message.WitnessSignature{
		Witness:   public,
		Signature: signature,
	})

	if sqlite.GetDBPath() != "" {
		log.Info().Msg("adding witness into db")

		db, err := sql.Open("sqlite3", sqlite.GetDBPath())
		if err != nil {
			log.Err(err).Msg("failed to open connection")
		} else {
			defer db.Close()

			err := addWitnessInDB(db, messageID, public, signature)
			if err != nil {
				log.Err(err).Msg("failed to store witness into db")
			}
		}
	}

	return nil
}

// StoreMessage stores a message inside the inbox
func (i *Inbox) StoreMessage(msg message.Message) {
	log := be1_go.Logger

	i.mutex.Lock()
	defer i.mutex.Unlock()

	storedTime := time.Now().UnixNano()

	messageInfo := &messageInfo{
		message:    msg,
		storedTime: storedTime,
	}

	i.msgs[msg.MessageID] = messageInfo

	if sqlite.GetDBPath() != "" {
		log.Info().Msg("storing message into db")

		err := i.storeMessageInDB(messageInfo)
		if err != nil {
			log.Err(err).Msg("failed to store message into db")
		}
	}
}

// GetSortedMessages returns all messages stored sorted by stored time.
func (i *Inbox) GetSortedMessages() []message.Message {
	i.mutex.RLock()
	defer i.mutex.RUnlock()

	messages := make([]messageInfo, 0, len(i.msgs))
	// iterate over map and collect all the values (messageInfo instances)
	for _, msgInfo := range i.msgs {
		messages = append(messages, *msgInfo)
	}

	// sort.Slice on messages based on the timestamp
	sort.Slice(messages, func(i, j int) bool {
		return messages[i].storedTime < messages[j].storedTime
	})

	result := make([]message.Message, len(messages))

	// iterate and extract the messages[i].message field and
	// append it to the result slice
	for i, msgInfo := range messages {
		result[i] = msgInfo.message
	}

	return result
}

// GetMessage returns the message of messageID if it exists. We need a pointer
// on message to add witness signatures.
func (i *Inbox) GetMessage(messageID string) (*message.Message, bool) {
	i.mutex.Lock()
	defer i.mutex.Unlock()

	msgInfo, ok := i.msgs[messageID]
	if !ok {
		return nil, false
	}
	return &msgInfo.message, true
}

// ---
// DB operations
// --

// storeMessageInDB stores a message into the db. It should be used in case the
// `HUB_DB` env variable is set.
func (i *Inbox) storeMessageInDB(messageInfo *messageInfo) error {
	db, err := sql.Open("sqlite3", sqlite.GetDBPath())
	if err != nil {
		return xerrors.Errorf("failed to open connection: %v", err)
	}

	defer db.Close()

	query := `
		INSERT INTO 
			message_info(
				message_id, 
				sender, 
				message_signature, 
				raw_data, 
				message_timestamp, 
				lao_channel_id) 
		VALUES(?, ?, ?, ?, ?, ?)`

	stmt, err := db.Prepare(query)
	if err != nil {
		return xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	msg := messageInfo.message

	_, err = stmt.Exec(msg.MessageID, msg.Sender, msg.Signature, msg.Data, messageInfo.storedTime, i.channelID)
	if err != nil {
		return xerrors.Errorf("failed to exec query: %v", err)
	}

	for _, pubKeySigPair := range messageInfo.message.WitnessSignatures {
		err = addWitnessInDB(db, string(messageInfo.message.MessageID),
			pubKeySigPair.Witness, pubKeySigPair.Signature)
		if err != nil {
			return xerrors.Errorf("failed to store witness: %v", err)
		}
	}

	return nil
}

func addWitnessInDB(db *sql.DB, messageID string, pubKey string, signature string) error {
	query := `
		INSERT INTO
			message_witness(
				pub_key, 
				witness_signature, 
				message_id) 
		VALUES(?, ?, ?)`

	stmt, err := db.Prepare(query)
	if err != nil {
		return xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	_, err = stmt.Exec(pubKey, signature, messageID)
	if err != nil {
		return xerrors.Errorf("failed to exec query: %v", err)
	}

	return nil
}

// CreateInboxFromDB creates an inbox from a database
func CreateInboxFromDB(db *sql.DB, channelID string) (*Inbox, error) {
	log := be1_go.Logger

	inbox := NewInbox(channelID)

	query := `
		SELECT
			message_id, 
			sender, 
			message_signature, 
			raw_data, 
			message_timestamp
		FROM
			message_info
		WHERE
			lao_channel_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	for rows.Next() {
		var messageID string
		var sender string
		var messageSignature string
		var rawData string
		var timestamp int64

		err = rows.Scan(&messageID, &sender, &messageSignature, &rawData, &timestamp)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		witnesses, err := getWitnessesMessageFromDB(db, messageID)
		if err != nil {
			return nil, xerrors.Errorf("failed to get witnesses: %v", err)
		}

		messageInfo := messageInfo{
			message: message.Message{
				MessageID:         messageID,
				Sender:            sender,
				Signature:         messageSignature,
				WitnessSignatures: witnesses,
				Data:              rawData,
			},
			storedTime: timestamp,
		}

		log.Info().Msgf("msg load: %+v", messageInfo.message)

		inbox.msgs[messageID] = &messageInfo
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return inbox, nil
}

func getWitnessesMessageFromDB(db *sql.DB, messageID string) ([]message.WitnessSignature, error) {
	query := `
		SELECT
			pub_key,
			witness_signature
		FROM
			message_witness
		WHERE
			message_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(messageID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := make([]message.WitnessSignature, 0)

	for rows.Next() {
		var pubKey string
		var signature string

		err = rows.Scan(&pubKey, &signature)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, message.WitnessSignature{
			Witness:   pubKey,
			Signature: signature,
		})
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}
