package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/message/messagedata/mroot"
	"testing"
)

func NewLaoCreateMsg(t *testing.T, sender string, ID string, laoName string, creation int64, organizer string,
	senderSK kyber.Scalar) mmessage.Message {
	laoCreate := mroot.LaoCreate{
		Object:    mmessage.LAOObject,
		Action:    mmessage.LAOActionCreate,
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
