package db

import (
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/message/query/method/message"
	"testing"
)

func newFakeSQLite(t *testing.T) (SQLite, string, error) {
	dir, err := os.MkdirTemp("", "test-")
	require.NoError(t, err)

	fn := filepath.Join(dir, "test.DB")
	lite, err := NewSQLite(fn, true)
	require.NoError(t, err)

	return lite, dir, nil
}

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

//======================================================================================================================
// Repository interface implementation tests
//======================================================================================================================

func TestSQLite_GetMessageByID(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)

	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
		require.NoError(t, err)
	}

	expected := []message.Message{testMessages[0].msg, testMessages[1].msg, testMessages[3].msg}
	IDs := []string{"ID1", "ID2", "ID3"}
	for i, elem := range IDs {
		msg, err := lite.GetMessageByID(elem)
		require.NoError(t, err)
		require.Equal(t, expected[i], msg)
	}
}

func TestSQLite_GetMessagesByID(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
		require.NoError(t, err)
	}

	IDs := []string{"ID1", "ID2", "ID3"}
	expected := map[string]message.Message{"ID1": testMessages[0].msg,
		"ID2": testMessages[1].msg,
		"ID3": testMessages[2].msg}

	messages, err := lite.GetMessagesByID(IDs)
	require.NoError(t, err)
	require.Equal(t, expected, messages)
}

func TestSQLite_GetSortedMessages(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
		require.NoError(t, err)
	}

	expected := []message.Message{testMessages[3].msg, testMessages[0].msg}
	messages, err := lite.GetSortedMessages("channel1")
	require.NoError(t, err)
	require.Equal(t, expected, messages)
}

func TestSQLite_AddWitnessSignature(t *testing.T) {
	lite, dir, err := newFakeSQLite(t)
	require.NoError(t, err)
	defer lite.Close()
	defer os.RemoveAll(dir)

	testMessages := initMessages()
	for _, m := range testMessages {
		err = lite.StoreMessage(m.channel, m.msg)
		require.NoError(t, err)
	}
	// Add signatures to message1
	expected := []message.WitnessSignature{{Witness: "witness1", Signature: "sig1"}, {Witness: "witness2", Signature: "sig2"}}
	err = lite.AddWitnessSignature("ID1", "witness1", "sig1")
	require.NoError(t, err)
	err = lite.AddWitnessSignature("ID1", "witness2", "sig2")
	require.NoError(t, err)

	//Verify that the signature have been added to the message
	msg1, err := lite.GetMessageByID("ID1")
	require.NoError(t, err)
	require.Equal(t, expected, msg1.WitnessSignatures)

	message4 := message.Message{Data: "data4",
		Sender:            "sender4",
		Signature:         "sig4",
		MessageID:         "ID4",
		WitnessSignatures: []message.WitnessSignature{},
	}

	// Add signatures to message4 who is not currently stored
	err = lite.AddWitnessSignature("ID4", "witness2", "sig4")
	require.NoError(t, err)

	//Verify that the signature has been added to the message
	err = lite.StoreMessage("channel1", message4)
	require.NoError(t, err)
	expected = []message.WitnessSignature{{Witness: "witness2", Signature: "sig4"}}
	msg4, err := lite.GetMessageByID("ID4")
	require.NoError(t, err)
	require.Equal(t, expected, msg4.WitnessSignatures)
}

//======================================================================================================================
// HandleQueryRepository interface implementation tests
//======================================================================================================================
