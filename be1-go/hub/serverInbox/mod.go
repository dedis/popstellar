package serverInbox

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"sort"
	"sync"
	"time"
)

// messageInfo wraps a message with a stored time for sorting.
type messageInfo struct {
	message    method.Publish
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
	//	log := be1_go.Logger

	msg, ok := i.GetMessage(messageID)
	if !ok {
		// TODO: We received a witness signature before the message itself. We
		// ignore it for now but it might be worth keeping it until we actually
		// receive the message
		return answer.NewErrorf(-4, "failed to find message_id %q for witness message", messageID)
	}

	i.mutex.Lock()
	defer i.mutex.Unlock()

	msg.Params.Message.WitnessSignatures = append(msg.Params.Message.WitnessSignatures, message.WitnessSignature{
		Witness:   public,
		Signature: signature,
	})

	/**
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
	*/

	return nil
}

// StoreMessage stores a message inside the inbox
func (i *Inbox) StoreMessage(publish method.Publish) {
	//	log := be1_go.Logger

	i.mutex.Lock()
	defer i.mutex.Unlock()

	storedTime := time.Now().UnixNano()

	messageInfo := &messageInfo{
		message:    publish,
		storedTime: storedTime,
	}

	i.msgs[publish.Params.Message.MessageID] = messageInfo

	/**
	if sqlite.GetDBPath() != "" {
		log.Info().Msg("storing message into db")

		err := i.storeMessageInDB(messageInfo)
		if err != nil {
			log.Err(err).Msg("failed to store message into db")
		}
	}
	*/
}

// GetSortedMessages returns all messages stored sorted by stored time.
func (i *Inbox) GetSortedMessages() []string {
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

	result := make([]string, len(messages))

	// iterate and extract the messages[i].message field and
	// append it to the result slice
	for i, msgInfo := range messages {
		buf, err := json.Marshal(msgInfo.message)
		if err == nil {
			result[i] = string(buf)
		}
	}

	return result
}

// GetMessage returns the message of messageID if it exists. We need a pointer
// on message to add witness signatures.
func (i *Inbox) GetMessage(messageID string) (*method.Publish, bool) {
	i.mutex.Lock()
	defer i.mutex.Unlock()

	msgInfo, ok := i.msgs[messageID]
	if !ok {
		return nil, false
	}
	return &msgInfo.message, true
}

// TODO adapt database oprations to this inbox
