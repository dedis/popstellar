package inbox

import (
	"database/sql"
	"log"
	"sort"
	"sync"
	"time"

	_ "github.com/mattn/go-sqlite3"

	"popstellar/db/sqlite"
	"popstellar/message/answer"
	"popstellar/message/query/method/message"
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

	if sqlite.GetDBPath() != "" {
		log.Println("adding witness into db")

		db, err := sql.Open("sqlite3", sqlite.GetDBPath())
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

	messageInfo := &messageInfo{
		message:    msg,
		storedTime: storedTime,
	}

	i.msgs[msg.MessageID] = messageInfo

	if sqlite.GetDBPath() != "" {
		log.Println("storing message into db")

		err := i.storeMessageInDB(messageInfo)
		if err != nil {
			log.Printf("error: failed to store message into db: %v", err)
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
