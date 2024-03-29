package hub

import (
	"github.com/rs/zerolog/log"
	"popstellar/hub/mocks"
	"popstellar/message/query/method/message"
	"testing"
)

func Test_handleChannelRoot(t *testing.T) {
	repo := mocks.NewRepository(t)
	repo.On("GetMessageByID", "messageID1").Return(message.Message{Data: "data1",
		Sender:            "sender1",
		Signature:         "sig1",
		MessageID:         "ID1",
		WitnessSignatures: []message.WitnessSignature{},
	}, nil)
	msg, err := repo.GetMessageByID("messageID1")
	if err != nil {
		return
	}
	log.Info().Msg(msg.MessageID)
}
