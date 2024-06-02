package generatortest

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/message/messagedata"
	"popstellar/internal/message/query/method/message"
	"testing"
)

func NewLaoStateMsg(t *testing.T, organizer, laoID, name, modificationID string, creation, lastModified int64,
	organizerSK kyber.Scalar) message.Message {
	laoState := messagedata.LaoState{
		Object:                 messagedata.LAOObject,
		Action:                 messagedata.LAOActionState,
		ID:                     laoID,
		Name:                   name,
		Creation:               creation,
		LastModified:           lastModified,
		Organizer:              organizer,
		Witnesses:              []string{},
		ModificationID:         modificationID,
		ModificationSignatures: []messagedata.ModificationSignature{},
	}

	buf, err := json.Marshal(laoState)
	require.NoError(t, err)

	msg := newMessage(t, organizer, organizerSK, buf)

	return msg
}

func NewRollCallCreateMsg(t *testing.T, sender, laoName, createID string, creation, start, end int64,
	senderSK kyber.Scalar) message.Message {
	rollCallCreate := messagedata.RollCallCreate{
		Object:        messagedata.RollCallObject,
		Action:        messagedata.RollCallActionCreate,
		ID:            createID,
		Name:          laoName,
		Creation:      creation,
		ProposedStart: start,
		ProposedEnd:   end,
		Location:      "Location",
		Description:   "Description",
	}

	buf, err := json.Marshal(rollCallCreate)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, buf)

	return msg
}

func NewRollCallOpenMsg(t *testing.T, sender, updateID, opens string, openedAt int64,
	senderSK kyber.Scalar) message.Message {

	rollCallOpen := messagedata.RollCallOpen{
		Object:   messagedata.RollCallObject,
		Action:   messagedata.RollCallActionOpen,
		UpdateID: updateID,
		Opens:    opens,
		OpenedAt: openedAt,
	}

	buf, err := json.Marshal(rollCallOpen)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, buf)

	return msg
}

func NewRollCallReOpenMsg(t *testing.T, sender, updateID, opens string, openedAt int64,
	senderSK kyber.Scalar) message.Message {

	rollCallReOpen := messagedata.RollCallOpen{
		Object:   messagedata.RollCallObject,
		Action:   messagedata.RollCallActionReOpen,
		UpdateID: updateID,
		Opens:    opens,
		OpenedAt: openedAt,
	}

	buf, err := json.Marshal(rollCallReOpen)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, buf)

	return msg
}

func NewRollCallCloseMsg(t *testing.T, sender, updateID, closes string, closedAt int64, attendees []string,
	senderSK kyber.Scalar) message.Message {

	rollCallClose := messagedata.RollCallClose{
		Object:    messagedata.RollCallObject,
		Action:    messagedata.RollCallActionClose,
		UpdateID:  updateID,
		Closes:    closes,
		ClosedAt:  closedAt,
		Attendees: attendees,
	}

	buf, err := json.Marshal(rollCallClose)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, buf)

	return msg
}

func NewElectionSetupMsg(t *testing.T, sender, ID, setupLao, electionName, version string,
	createdAt, start, end int64, questions []messagedata.ElectionSetupQuestion, senderSK kyber.Scalar) message.Message {

	electionSetup := messagedata.ElectionSetup{
		Object:    messagedata.ElectionObject,
		Action:    messagedata.ElectionActionSetup,
		ID:        ID,
		Lao:       setupLao,
		Name:      electionName,
		Version:   version,
		CreatedAt: createdAt,
		StartTime: start,
		EndTime:   end,
		Questions: questions,
	}

	buf, err := json.Marshal(electionSetup)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSK, buf)

	return msg
}
