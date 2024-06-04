package channel

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/mock"
	"popstellar/internal/singleton/database"
	state2 "popstellar/internal/singleton/state"
	"popstellar/internal/types"
	"testing"
)

const coinPath string = "../../validation/protocol/examples/messageData/coin"

type inputTestHandleChannelCoin struct {
	name      string
	channelID string
	message   message.Message
	hasError  bool
	sockets   []*mock.FakeSocket
}

func Test_handleChannelCoin(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()
	hubParams := types.NewHubParams()

	state2.SetState(subs, peers, queries, hubParams)

	mockRepository := mock.NewRepository(t)
	database.SetDatabase(mockRepository)

	inputs := make([]inputTestHandleChannelCoin, 0)

	// Tests that the channelPath works correctly when it receives a transaction

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction.json",
		"send transaction",
		mockRepository))

	// Tests that the channelPath works correctly when it receives a large transaction

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction_max_amount.json",
		"send transaction max amount",
		mockRepository))

	// Tests that the channelPath rejects transactions that exceed the maximum amount

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_overflow_amount.json",
		"send transaction overflow amount"))

	// Tests that the channelPath accepts transactions with zero amounts

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction_zero_amount.json",
		"send transaction zero amount",
		mockRepository))

	// Tests that the channelPath rejects transactions with negative amounts

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_negative_amount.json",
		"send transaction negative amount"))

	// Tests that the channelPath rejects Transaction with wrong id

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_wrong_transaction_id.json",
		"send transaction wrong id"))

	// Tests that the channelPath rejects Transaction with bad signature

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_bad_signature.json",
		"send transaction bad signature"))

	// Tests that the channelPath works correctly when it receives a transaction

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction_coinbase.json",
		"send transaction coinbase",
		mockRepository))

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

func newSuccessTestHandleChannelCoin(t *testing.T, filename string, name string, mockRepository *mock.Repository) inputTestHandleChannelCoin {
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

	mockRepository.On("StoreMessageAndData", channelID, m).Return(nil)

	sockets := []*mock.FakeSocket{
		{Id: laoID + "0"},
		{Id: laoID + "1"},
		{Id: laoID + "2"},
		{Id: laoID + "3"},
	}

	errAnswer := state2.AddChannel(channelID)
	require.Nil(t, errAnswer)

	for _, s := range sockets {
		errAnswer := state2.Subscribe(s, channelID)
		require.Nil(t, errAnswer)
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

	errAnswer := state2.AddChannel(channelID)
	require.Nil(t, errAnswer)

	return inputTestHandleChannelCoin{
		name:      name,
		channelID: channelID,
		message:   m,
		hasError:  true,
	}
}
