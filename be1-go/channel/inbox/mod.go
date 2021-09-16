package inbox

import (
	"database/sql"
	"log"
	"os"
	"sync"
	"time"

	"golang.org/x/xerrors"

	_ "github.com/mattn/go-sqlite3"

	"student20_pop/channel"
	"student20_pop/message/answer"
	"student20_pop/message/query/method/message"
)

// Inbox represents an in-memory data store to record incoming messages.
type Inbox struct {
	mutex     sync.RWMutex
	msgs      map[string]*channel.MessageInfo
	channelID string
}

// NewInbox returns a new initialized inbox
func NewInbox(channelID string) *Inbox {
	return &Inbox{
		mutex:     sync.RWMutex{},
		msgs:      make(map[string]*channel.MessageInfo),
		channelID: channelID,
	}
}

// AddWitnessSignature adds a signature of witness to a message of ID
// `messageID`. if the signature was correctly added return true otherwise
// returns false
func (i *Inbox) AddWitnessSignature(messageID string, public string, signature string) error {
	msg, ok := i.GetMessage(messageID)
	if !ok {
		// TODO: We received a witness signature before the message itself. We
		// ignore it for now but it might be worth keeping it until we actually
		// receive the message
		log.Printf("failed to find message_id %s for witness message", messageID)
		return answer.NewErrorf(-4, "failed to find message_id %q for witness message", messageID)
	}

	i.mutex.Lock()
	defer i.mutex.Unlock()

	msg.WitnessSignatures = append(msg.WitnessSignatures, message.WitnessSignature{
		Witness:   public,
		Signature: signature,
	})

	if os.Getenv("HUB_DB") != "" {
		log.Println("adding witness into db")

		db, err := sql.Open("sqlite3", os.Getenv("HUB_DB"))
		if err != nil {
			log.Printf("error: failed to open connection: %v", err)
		} else {
			defer db.Close()

			err := addWitnessInDB(db, messageID, public, signature)
			if err != nil {
				log.Printf("error: failed to store witness into db: %v", err)
			}
		}
	}

	return nil
}

// StoreMessage stores a message inside the inbox
func (i *Inbox) StoreMessage(msg message.Message) {
	i.mutex.Lock()
	defer i.mutex.Unlock()

	storedTime := time.Now().UnixNano()

	messageInfo := &channel.MessageInfo{
		Message:    msg,
		StoredTime: storedTime,
	}

	i.msgs[msg.MessageID] = messageInfo

	if os.Getenv("HUB_DB") != "" {
		log.Println("storing message into db")

		err := i.storeMessageInDB(messageInfo)
		if err != nil {
			log.Printf("error: failed to store message into db: %v", err)
		}
	}
}

// GetMessages ...
func (i *Inbox) GetMessages() []channel.MessageInfo {
	i.mutex.RLock()
	defer i.mutex.RUnlock()

	messages := make([]channel.MessageInfo, 0, len(i.msgs))
	// iterate over map and collect all the values (messageInfo instances)
	for _, msgInfo := range i.msgs {
		messages = append(messages, *msgInfo)
	}

	return messages
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
	return &msgInfo.Message, true
}

func (i *Inbox) storeMessageInDB(messageInfo *channel.MessageInfo) error {
	db, err := sql.Open("sqlite3", os.Getenv("HUB_DB"))
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
		return xerrors.Errorf("failed to prepare query: %v", err)
	}

	defer stmt.Close()

	msg := messageInfo.Message

	_, err = stmt.Exec(msg.MessageID, msg.Sender, msg.Signature, msg.Data, messageInfo.StoredTime, i.channelID)
	if err != nil {
		return xerrors.Errorf("failed to exec query: %v", err)
	}

	for _, pubKeySigPair := range messageInfo.Message.WitnessSignatures {
		err = addWitnessInDB(db, string(messageInfo.Message.MessageID),
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
		return xerrors.Errorf("failed to prepare query: %v", err)
	}

	defer stmt.Close()

	_, err = stmt.Exec(pubKey, signature, messageID)
	if err != nil {
		return xerrors.Errorf("failed to exec query: %v", err)
	}

	return nil
}
