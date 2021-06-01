package hub

import (
	"encoding/base64"
	"log"
	"student20_pop/message"
	"sync"
	"time"
)

type inbox struct {
	mutex sync.RWMutex
	msgs  map[string]*messageInfo
}

// addWitnessSig adds a signature of witness to a message of ID `messageID`.
// if no message has ID `messageID`, does nothing
func (i inbox) addWitnessSig(messageID []byte, public message.PublicKey, signature message.Signature) {
	msg, ok := i.getMessage(messageID)
	if !ok {
		// TODO: We received a witness signature before the message itself.
		// We ignore it for now but it might be worth keeping it until we
		// actually receive the message
		log.Printf("failed to find message_id %s for witness message", messageID)
	}
	i.mutex.Lock()
	msg.WitnessSignatures = append(msg.WitnessSignatures, message.PublicKeySignaturePair{
		Witness:   public,
		Signature: signature,
	})
	i.mutex.Unlock()
}

// storeMessage stores a message inside the inbox
func (i inbox) storeMessage(msg message.Message) {
	msgIDEncoded := base64.URLEncoding.EncodeToString(msg.MessageID)
	storedTime := message.Timestamp(time.Now().UnixNano())

	messageInfo := &messageInfo{
		message:    msg,
		storedTime: storedTime,
	}

	i.mutex.Lock()
	i.msgs[msgIDEncoded] = messageInfo
	i.mutex.Unlock()
}

// getMessage returns the message of messageID if it exists.
func (i inbox) getMessage(messageID []byte) (message.Message, bool) {
	msgIDEncoded := base64.URLEncoding.EncodeToString(messageID)

	i.mutex.RLock()
	msgInfo, ok := i.msgs[msgIDEncoded]
	i.mutex.Unlock()
	return msgInfo.message, ok
}
