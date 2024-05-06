package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
)

func NewLaoCreateMsg(t *testing.T, sender string, ID string, laoName string, creation int64, organizer string,
	senderPK kyber.Scalar) message.Message {
	laoCreate := messagedata.LaoCreate{
		Object:    messagedata.LAOObject,
		Action:    messagedata.LAOActionCreate,
		ID:        ID,
		Name:      laoName,
		Creation:  creation,
		Organizer: organizer,
		Witnesses: []string{},
	}

	buf, err := json.Marshal(laoCreate)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderPK, buf)

	return msg
}
