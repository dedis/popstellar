package db

import (
	"encoding/base64"
	"io/ioutil"
	"student20_pop/message"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestSQLite_NewRepository(t *testing.T) {
	dir, err := ioutil.TempDir("", "pop")
	require.NoError(t, err)

	repo, err := NewSQLiteRepository(dir + "/test.db")
	require.NoError(t, err)

	err = repo.Close()
	require.NoError(t, err)
}

func TestSQLite_AddMessage(t *testing.T) {
	dir, err := ioutil.TempDir("", "pop")
	require.NoError(t, err)

	repo, err := NewSQLiteRepository(dir + "/add_message.db")
	require.NoError(t, err)

	defer repo.Close()

	channelID := "12345"
	timestamp := message.Timestamp(time.Now().UnixNano())

	data, err := message.NewCreateLAOData("test", timestamp, []byte{1, 2, 3, 4}, []message.PublicKey{})
	require.NoError(t, err)

	msg := message.Message{
		MessageID:         []byte{6, 7, 8, 9, 10},
		Data:              data,
		Sender:            []byte{1, 2, 3, 4},
		Signature:         []byte{1, 2, 3, 4},
		WitnessSignatures: []message.PublicKeySignaturePair{},
	}

	err = repo.AddMessage(channelID, msg, timestamp)
	require.NoError(t, err)
}

func TestSQLite_GetMessages(t *testing.T) {
	dir, err := ioutil.TempDir("", "pop")
	require.NoError(t, err)

	repo, err := NewSQLiteRepository(dir + "/get_messages.db")
	require.NoError(t, err)

	defer repo.Close()

	channelID := "12345"
	now := time.Now().UnixNano()

	for i := 0; i < 100; i++ {
		timestamp := message.Timestamp(now + int64(i))
		data, err := message.NewCreateLAOData("test", timestamp, []byte{1, 2, 3, 4}, []message.PublicKey{})
		require.NoError(t, err)

		msg := message.Message{
			MessageID:         []byte{6, 7, 8, 9, byte(i)},
			Data:              data,
			Sender:            []byte{1, 2, 3, 4},
			Signature:         []byte{1, 2, 3, 4},
			WitnessSignatures: []message.PublicKeySignaturePair{},
		}

		err = repo.AddMessage(channelID, msg, timestamp)
		require.NoError(t, err)
	}

	messages, err := repo.GetMessages(channelID)
	require.NoError(t, err)

	require.Equal(t, 100, len(messages))
}

func TestSQLite_GetMessagesInRange(t *testing.T) {
	dir, err := ioutil.TempDir("", "pop")
	require.NoError(t, err)

	repo, err := NewSQLiteRepository(dir + "/get_messages_range.db")
	require.NoError(t, err)

	defer repo.Close()

	channelID := "12345"
	now := time.Now().UnixNano()

	for i := 0; i < 100; i++ {
		timestamp := message.Timestamp(now + int64(i))
		data, err := message.NewCreateLAOData("test", timestamp, []byte{1, 2, 3, 4}, []message.PublicKey{})
		require.NoError(t, err)

		msg := message.Message{
			MessageID:         []byte{6, 7, 8, 9, byte(i)},
			Data:              data,
			Sender:            []byte{1, 2, 3, 4},
			Signature:         []byte{1, 2, 3, 4},
			WitnessSignatures: []message.PublicKeySignaturePair{},
		}

		err = repo.AddMessage(channelID, msg, timestamp)
		require.NoError(t, err)
	}

	start := message.Timestamp(now + int64(20))
	end := message.Timestamp(now + int64(50))
	messages, err := repo.GetMessagesInRange(channelID, start, end)
	require.NoError(t, err)

	require.Equal(t, 31, len(messages))
}

func TestSQLite_AddWitnessToMessage(t *testing.T) {
	dir, err := ioutil.TempDir("", "pop")
	require.NoError(t, err)

	repo, err := NewSQLiteRepository(dir + "/add_witness.db")
	require.NoError(t, err)

	defer repo.Close()

	channelID := "12345"
	timestamp := message.Timestamp(time.Now().UnixNano())

	data, err := message.NewCreateLAOData("test", timestamp, []byte{1, 2, 3, 4}, []message.PublicKey{})
	require.NoError(t, err)

	msg := message.Message{
		MessageID:         []byte{6, 7, 8, 9, 10},
		Data:              data,
		Sender:            []byte{1, 2, 3, 4},
		Signature:         []byte{1, 2, 3, 4},
		WitnessSignatures: []message.PublicKeySignaturePair{},
	}

	messageID := base64.StdEncoding.EncodeToString(msg.MessageID)

	err = repo.AddMessage(channelID, msg, timestamp)
	require.NoError(t, err)

	addWitness := func(i byte, wg *sync.WaitGroup) {
		pair := message.PublicKeySignaturePair{
			Signature: []byte{i},
			Witness:   []byte{i},
		}

		err := repo.AddWitnessToMessage(messageID, pair)
		require.NoError(t, err)
		wg.Done()
	}

	wg := sync.WaitGroup{}
	for i := 0; i < 100; i++ {
		wg.Add(1)
		go addWitness(byte(i), &wg)
	}

	wg.Wait()

	messages, err := repo.GetMessages(channelID)
	require.NoError(t, err)

	require.Equal(t, 1, len(messages))

	require.Equal(t, 100, len(messages[0].WitnessSignatures))

	counts := make(map[byte]int)

	for i := 0; i < 100; i++ {
		witness := messages[0].WitnessSignatures[i].Witness
		require.Equal(t, 1, len(witness))

		idx := witness[0]
		counts[idx]++
	}

	require.Equal(t, 100, len(counts))

	for _, v := range counts {
		require.Equal(t, 1, v)
	}
}
