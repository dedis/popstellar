package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/channel"
	"popstellar/internal/handler/channel/root/mroot"
	"popstellar/internal/handler/message/mmessage"
	"testing"
)

func NewLaoCreateMsg(t *testing.T, sender string, ID string, laoName string, creation int64, organizer string,
	senderSK kyber.Scalar) mmessage.Message {
	laoCreate := mroot.LaoCreate{
		Object:    channel.LAOObject,
		Action:    channel.LAOActionCreate,
		ID:        ID,
		Name:      laoName,
		Creation:  creation,
		Organizer: organizer,
		Witnesses: []string{},
	}

	buf, err := json.Marshal(laoCreate)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, buf)

	return msg
}
