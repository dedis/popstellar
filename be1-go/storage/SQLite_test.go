package storage

import (
	"golang.org/x/xerrors"
	"os"
	"path/filepath"
	"popstellar/message/query/method/message"
	"reflect"
	"testing"
)

type testMessage struct {
	msg     message.Message
	channel string
}

func initMessages() []testMessage {

	message1 := message.Message{Data: "data1",
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}

	message2 := message.Message{Data: "data2",
		Sender:            "sender2",
		Signature:         "sig2",
		MessageID:         "ID2",
		WitnessSignatures: []message.WitnessSignature{},
	}

	message3 := message.Message{Data: "data3",
		Sender:            "sender3",
		Signature:         "sig3",
		MessageID:         "ID3",
		WitnessSignatures: []message.WitnessSignature{},
	}

	return []testMessage{{msg: message1, channel: "channel1"},
		{msg: message2, channel: "channel2"},
		{msg: message3, channel: "channel1/subChannel1"},
		{msg: message3, channel: "channel1"},
	}
}

func newFakeSQLite() (SQLite, string, error) {
	dir, err := os.MkdirTemp("", "test-")
	if err != nil {
		return SQLite{}, "", xerrors.Errorf("an error '%s' was not expected when creating a temporary directory", err)
	}

	fn := filepath.Join(dir, "test.db")

	lite, err := New(fn)
	if err != nil {
		return SQLite{}, "", xerrors.Errorf("an error '%s' was not expected when creating a new SQLite instance", err)
	}
	return lite, dir, nil
}

func TestSQLite_GetMessagesByID(t *testing.T) {

	lite, dir, err := newFakeSQLite()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when creating a new fake SQLite instance", err)
	}

	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()

	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
		if err != nil {
			t.Fatalf("an error '%s' was not expected when storing a message", err)
		}
	}

	IDs := []string{"ID1", "ID2", "ID3"}
	expected := map[string]message.Message{"ID1": testMessages[0].msg,
		"ID2": testMessages[1].msg,
		"ID3": testMessages[2].msg}

	messages, err := lite.GetMessagesByID(IDs)
	if err != nil {
		t.Fatalf("an error '%s' was not expected when getting messages by ID", err)
	}

	if len(messages) != len(expected) {
		t.Fatalf("expected %d messages, got %d", len(expected), len(messages))
	}

	if !reflect.DeepEqual(messages, expected) {
		t.Fatalf("expected messages to be equal to the stored messages")
	}
}

func TestSQLite_GetSortedMessages(t *testing.T) {

	lite, dir, err := newFakeSQLite()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when creating a new fake SQLite instance", err)

	}

	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()

	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
		if err != nil {
			t.Fatalf("an error '%s' was not expected when storing a message", err)
		}
	}

	expected := []message.Message{testMessages[3].msg, testMessages[0].msg}
	messages, err := lite.GetSortedMessages("channel1")
	if err != nil {
		t.Fatalf("an error '%s' was not expected when getting sorted messages", err)
	}

	if len(messages) != len(expected) {
		t.Fatalf("expected %d messages, got %d", len(expected), len(messages))
	}

	if !reflect.DeepEqual(messages, expected) {
		t.Fatalf("expected messages to be equal to the stored messages")
	}
}

func TestSQLite_GetIDsTable(t *testing.T) {

	lite, dir, err := newFakeSQLite()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when creating a new fake SQLite instance", err)

	}

	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()

	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
		if err != nil {
			t.Fatalf("an error '%s' was not expected when storing a message", err)
		}
	}

	expected := map[string][]string{"channel1": {"ID1", "ID3"}, "channel2": {"ID2"}}
	table, err := lite.GetIDsTable()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when getting the IDs table", err)
	}

	if len(table) != len(expected) {
		t.Fatalf("expected %d channels, got %d", len(expected), len(table))
	}

	if !reflect.DeepEqual(table, expected) {
		t.Fatalf("expected table to be equal to the stored table")
	}
}

func TestSQLite_GetMessageByID(t *testing.T) {

	lite, dir, err := newFakeSQLite()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when creating a new fake SQLite instance", err)
	}

	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()

	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
		if err != nil {
			t.Fatalf("an error '%s' was not expected when storing a message", err)
		}
	}

	expected := []message.Message{testMessages[0].msg, testMessages[1].msg, testMessages[3].msg}
	IDs := []string{"ID1", "ID2", "ID3"}

	for i, elem := range IDs {
		msg, err := lite.GetMessageByID(elem)
		if err != nil {
			t.Fatalf("an error '%s' was not expected when getting a message by ID", err)
		}
		if !reflect.DeepEqual(msg, expected[i]) {
			t.Fatalf("expected message to be equal to the stored message")
		}
	}
}

func TestSQLite_AddWitnessSignature(t *testing.T) {
	lite, dir, err := newFakeSQLite()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when creating a new fake SQLite instance", err)
	}

	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()

	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
		if err != nil {
			t.Fatalf("an error '%s' was not expected when storing a message", err)
		}
	}

	expected := []message.WitnessSignature{{Witness: "witness1", Signature: "sig1"}, {Witness: "witness2", Signature: "sig2"}}

	err = lite.AddWitnessSignature("ID1", "witness1", "sig1")
	if err != nil {
		t.Fatalf("an error '%s' was not expected when adding witness1 signature1", err)
	}
	err = lite.AddWitnessSignature("ID1", "witness2", "sig2")
	if err != nil {
		t.Fatalf("an error '%s' was not expected when adding witness2 signature2", err)
	}

	msg1, err := lite.GetMessageByID("ID1")
	if err != nil {
		t.Fatalf("an error '%s' was not expected when getting a message1 by ID", err)
	}
	if !reflect.DeepEqual(msg1.WitnessSignatures, expected) {

		t.Fatalf("expected witness signatures to be equal to the stored witness signatures for message1")
	}

	message4 := message.Message{Data: "data4",
		Sender:            "sender4",
		Signature:         "sig4",
		MessageID:         "ID4",
		WitnessSignatures: []message.WitnessSignature{},
	}

	err = lite.AddWitnessSignature("ID4", "witness2", "sig4")
	if err != nil {
		t.Fatalf("an error '%s' was not expected when adding a witness2 signature4", err)
	}

	err = lite.StoreMessage("channel1", message4)
	if err != nil {
		t.Fatalf("an error '%s' was not expected when storing message4", err)
	}

	expected = []message.WitnessSignature{{Witness: "witness2", Signature: "sig4"}}
	msg4, err := lite.GetMessageByID("ID4")
	if err != nil {
		t.Fatalf("an error '%s' was not expected when getting a message4 by ID", err)
	}
	if len(msg4.WitnessSignatures) != len(expected) {
		t.Fatalf("expected %d witness signatures, got %d", len(expected), len(msg4.WitnessSignatures))
	}

	if !reflect.DeepEqual(msg4.WitnessSignatures, expected) {
		t.Fatalf("expected witness signatures to be equal to the stored witness signatures for message4")
	}
}
