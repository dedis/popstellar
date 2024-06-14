package hcoin

import (
	"embed"
	"encoding/base64"
	"encoding/json"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"io"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/coin/hcoin/mocks"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/method/broadcast/mbroadcast"
	mocks2 "popstellar/internal/network/socket/mocks"
	"popstellar/internal/state"
	"popstellar/internal/validation"
	"testing"
)

//go:embed testdata/*.json
var testData embed.FS

type inputTestHandleChannelCoin struct {
	name      string
	channelID string
	message   mmessage.Message
	hasError  bool
	sockets   []*mocks2.FakeSocket
}

func Test_handleChannelCoin(t *testing.T) {
	log := zerolog.New(io.Discard)

	subs := state.NewSubscribers(log)

	db := mocks.NewRepository(t)

	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	coin := New(subs, db, schema, log)

	inputs := make([]inputTestHandleChannelCoin, 0)

	// Tests that the channelPath works correctly when it receives a transaction

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction.json",
		"send transaction",
		db, subs))

	// Tests that the channelPath works correctly when it receives a large transaction

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction_max_amount.json",
		"send transaction max amount",
		db, subs))

	// Tests that the channelPath rejects transactions that exceed the maximum amount

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_overflow_amount.json",
		"send transaction overflow amount",
		subs))

	// Tests that the channelPath accepts transactions with zero amounts

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction_zero_amount.json",
		"send transaction zero amount",
		db, subs))

	// Tests that the channelPath rejects transactions with negative amounts

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_negative_amount.json",
		"send transaction negative amount",
		subs))

	// Tests that the channelPath rejects Transaction with wrong id

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_wrong_transaction_id.json",
		"send transaction wrong id",
		subs))

	// Tests that the channelPath rejects Transaction with bad signature

	inputs = append(inputs, newFailTestHandleChannelCoin(t,
		"post_transaction_bad_signature.json",
		"send transaction bad signature",
		subs))

	// Tests that the channelPath works correctly when it receives a transaction

	inputs = append(inputs, newSuccessTestHandleChannelCoin(t,
		"post_transaction_coinbase.json",
		"send transaction coinbase",
		db, subs))

	// Tests all cases

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			err := coin.Handle(i.channelID, i.message)
			if i.hasError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)

				for _, s := range i.sockets {
					require.NotNil(t, s.Msg)

					var msg mbroadcast.Broadcast
					err := json.Unmarshal(s.Msg, &msg)
					require.NoError(t, err)

					require.Equal(t, i.message, msg.Params.Message)
				}
			}
		})
	}

}

func newSuccessTestHandleChannelCoin(t *testing.T, filename string, name string, mockRepository *mocks.Repository,
	subs *state.Subscribers) inputTestHandleChannelCoin {
	laoID := channel.Hash(name)
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var channelID = "/root/" + laoID + "/coin"

	buf, err := testData.ReadFile("testdata/" + filename)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := mmessage.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         channel.Hash(buf64, "h"),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	mockRepository.On("StoreMessageAndData", channelID, m).Return(nil)

	sockets := []*mocks2.FakeSocket{
		{Id: laoID + "0"},
		{Id: laoID + "1"},
		{Id: laoID + "2"},
		{Id: laoID + "3"},
	}

	err = subs.AddChannel(channelID)
	require.NoError(t, err)

	for _, s := range sockets {
		err = subs.Subscribe(channelID, s)
		require.NoError(t, err)
	}

	return inputTestHandleChannelCoin{
		name:      name,
		channelID: channelID,
		message:   m,
		hasError:  false,
		sockets:   sockets,
	}
}

func newFailTestHandleChannelCoin(t *testing.T, filename string, name string,
	subs *state.Subscribers) inputTestHandleChannelCoin {
	laoID := channel.Hash(name)
	var sender = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	var channelID = "/root/" + laoID + "/coin"

	buf, err := testData.ReadFile("testdata/" + filename)
	require.NoError(t, err)

	buf64 := base64.URLEncoding.EncodeToString(buf)

	m := mmessage.Message{
		Data:              buf64,
		Sender:            sender,
		Signature:         "h",
		MessageID:         channel.Hash(buf64, "h"),
		WitnessSignatures: []mmessage.WitnessSignature{},
	}

	err = subs.AddChannel(channelID)
	require.NoError(t, err)

	return inputTestHandleChannelCoin{
		name:      name,
		channelID: channelID,
		message:   m,
		hasError:  true,
	}
}
