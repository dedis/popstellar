package channel

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	database2 "popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"popstellar/message/query/method"
	"popstellar/message/query/method/message"
	"popstellar/network/socket"
	"testing"
)

const coinPath string = "../../../validation/protocol/examples/messageData/coin"

type inputTestHandleChannelCoin struct {
	name      string
	channelID string
	message   message.Message
	hasError  bool
	sockets   []*socket.FakeSocket
}

func Test_handleChannelCoin(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	mockRepo, err := database2.SetDatabase(t)
	require.NoError(t, err)

	inputs := make([]inputTestHandleChannelCoin, 0)

	// Tests that the channel works correctly when it receives a transaction

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction.json",
		"send transaction",
		mockRepo))

	// Tests that the channel works correctly when it receives a large transaction

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction_max_amount.json",
		"send transaction max amount",
		mockRepo))

	// Tests that the channel rejects transactions that exceed the maximum amount

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_overflow_amount.json",
		"send transaction overflow amount"))

	// Tests that the channel accepts transactions with zero amounts

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction_zero_amount.json",
		"send transaction zero amount",
		mockRepo))

	// Tests that the channel rejects transactions with negative amounts

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_negative_amount.json",
		"send transaction negative amount"))

	// Tests that the channel rejects Transaction with wrong id

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_wrong_transaction_id.json",
		"send transaction wrong id"))

	// Tests that the channel rejects Transaction with bad signature

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_bad_signature.json",
		"send transaction bad signature"))

	// Tests that the channel works correctly when it receives a transaction

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction_coinbase.json",
		"send transaction coinbase",
		mockRepo))

	// Tests all cases

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			errAnswer := handleChannelCoin(i.channelID, i.message)
			if i.hasError {
				require.NotNil(t, errAnswer)
			} else {
				require.Nil(t, errAnswer)

				for _, s := range i.sockets {
					require.NotNil(t, s.Msg)

					var msg method.Broadcast
					err := json.Unmarshal(s.Msg, &msg)
					require.NoError(t, err)

					require.Equal(t, i.message, msg.Params.Message)
				}
			}
		})
	}

}

func newSuccessTestHandleChannelCoin(t *testing.T, filename string, name string, mockRepo *database2.MockRepository) inputTestHandleChannelCoin {
	laoID := messagedata.Hash(name)
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var channelID = "/root/" + laoID + "/coin"

	file := filepath.Join(coinPath, filename)
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	mockRepo.On("StoreMessageAndData", channelID, m).Return(nil)

	sockets := []*socket.FakeSocket{
		{Id: laoID + "0"},
		{Id: laoID + "1"},
		{Id: laoID + "2"},
		{Id: laoID + "3"},
	}

	subs, ok := state.GetSubsInstance()
	require.True(t, ok)

	subs.AddChannel(channelID)

	for _, s := range sockets {
		err := subs.Subscribe(channelID, s)
		require.Nil(t, err)
	}

	return inputTestHandleChannelCoin{
		name:      name,
		channelID: channelID,
		message:   m,
		hasError:  false,
		sockets:   sockets,
	}
}

func newFailTestHandleChannelCoin(t *testing.T, filename string, name string) inputTestHandleChannelCoin {
	laoID := messagedata.Hash(name)
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var channelID = "/root/" + laoID + "/coin"

	file := filepath.Join(coinPath, filename)
	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := message.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         messagedata.Hash(buf64, "h"),
		WitnessSignatures: []message.WitnessSignature{},
	}

	subs, ok := state.GetSubsInstance()
	require.True(t, ok)

	subs.AddChannel(channelID)

	return inputTestHandleChannelCoin{
		name:      name,
		channelID: channelID,
		message:   m,
		hasError:  true,
	}
}
