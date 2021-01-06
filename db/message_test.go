package db

import (
	"bytes"
	"encoding/json"
	"os"
	"student20_pop/message"
	"testing"
)

// the package DB is very low-level. So it does no check on data validity (ID, hashes, etc...)

// TestWriteMessage tests if CreateMessage works, if CreateMessage returns an error if the channel already
// exists, if UpdateMessage works, and if UpdateMessage returns an error if the channel does not exists
func TestWriteMessage(t *testing.T) {

	// error not logged : error occurs if file does not exists
	_ = os.Remove("test.db")
	msg := getMsg()

	// produces an error as the channel does not exists
	err1 := UpdateMessage(msg, "test channel", "test.db")
	if err1 == nil {
		t.Errorf("Was able to update unexisting message")
	}

	//produces no error as the channels does not exist yet
	err1 = CreateMessage(msg, "test channel", "test.db")
	if err1 != nil {
		t.Errorf("Message creation in database unsuccessful")
	}

	// produces no errors as the channel already exists
	err1 = UpdateMessage(msg, "test channel", "test.db")
	if err1 != nil {
		t.Errorf("Message update in database unsuccessful")
	}

	// produces an error as the channel already exists
	err1 = CreateMessage(msg, "test channel", "test.db")
	if err1 == nil {
		t.Errorf("Message creation in database unsuccessful")
	}

	_ = os.Remove("test.db")
}

// TestGetMessage tests if retrieving a message from the Database gives the wanted result
func TestGetMessage(t *testing.T) {
	// error not logged : error occurs if file does not exists
	_ = os.Remove("test.db")
	msg := getMsg()

	err1 := CreateMessage(msg, "test channel", "test.db")
	if err1 != nil {
		t.Errorf("unable to store message %v", err1)
	}

	dMsg := GetMessage([]byte("test channel"), []byte("1"), "test.db")

	if dMsg == nil {
		t.Errorf("could not retrieve entry in the database")
	}

	mMsg, err1 := json.Marshal(msg)

	if !bytes.Equal(mMsg, dMsg) {
		t.Errorf("Message before and after storing in the database not the same")
	}
	_ = os.Remove("test.db")

}

// getMsg returns dummy Message in order to factorize it's generation
func getMsg() message.Message {

	msg := message.Message{
		Data:              []byte("this is some data"),
		Sender:            []byte("this is the sender"),
		Signature:         []byte("this is his signature"),
		MessageId:         []byte("1"),
		WitnessSignatures: nil,
	}

	return msg

}
