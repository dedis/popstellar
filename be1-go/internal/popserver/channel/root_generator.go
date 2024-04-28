package channel

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	database2 "popstellar/internal/popserver/database"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
	"time"
)

const (
	// WrongSender A public key different from the owner public key
	WrongSender  = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
	GoodLaoName  = "laoName"
	WrongLaoName = "wrongLaoName"
)

func NewLaoCreateMsg(t *testing.T, organizer, sender, laoName string, mockRepo *database2.MockRepository, isError bool) message.Message {
	creation := time.Now().Unix()
	laoID := messagedata.Hash(
		organizer,
		fmt.Sprintf("%d", creation),
		GoodLaoName,
	)
	laoCreate := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        laoID,
		Name:      laoName,
		Creation:  creation,
		Organizer: organizer,
		Witnesses: []string{},
	}

	buf, err := json.Marshal(laoCreate)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)
	msg := newRootMessage(t, buf64, sender)

	mockRepo.On("HasChannel", RootPrefix+laoID).Return(false, nil)
	if !isError {
		laoPath := RootPrefix + laoID
		organizerBuf, err := base64.URLEncoding.DecodeString(organizer)
		require.NoError(t, err)
		channels := map[string]string{
			laoPath + Social + Chirps:    ChannelChirp,
			laoPath + Social + Reactions: ChannelReaction,
			laoPath + Consensus:          ChannelConsensus,
			laoPath + Coin:               ChannelCoin,
			laoPath + Auth:               ChannelAuth,
		}
		mockRepo.On("StoreChannelsAndMessageWithLaoGreet",
			channels,
			laoPath,
			organizerBuf,
			msg, mock.AnythingOfType("message.Message")).Return(nil)
	}
	return msg
}

func NewNothingMsg(t *testing.T, sender string) message.Message {
	data := struct {
		Object string `json:"object"`
		Action string `json:"action"`
		Not    string `json:"not"`
	}{
		Object: "lao",
		Action: "nothing",
		Not:    "no",
	}
	buf, err := json.Marshal(data)
	require.NoError(t, err)
	buf64 := base64.URLEncoding.EncodeToString(buf)

	return newRootMessage(t, buf64, sender)
}

func newRootMessage(t *testing.T, data, sender string) message.Message {
	return message.Message{
		Data:              data,
		Sender:            sender,
		Signature:         "signature",
		MessageID:         "ID",
		WitnessSignatures: []message.WitnessSignature{},
	}
}
