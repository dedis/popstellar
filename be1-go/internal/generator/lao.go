package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/message/messagedata/mlao"
	"testing"
)

func NewLaoStateMsg(t *testing.T, organizer, laoID, name, modificationID string, creation, lastModified int64,
	organizerSK kyber.Scalar) mmessage.Message {
	laoState := mlao.LaoState{
		Object:                 mmessage.LAOObject,
		Action:                 mmessage.LAOActionState,
		ID:                     laoID,
		Name:                   name,
		Creation:               creation,
		LastModified:           lastModified,
		Organizer:              organizer,
		Witnesses:              []string{},
		ModificationID:         modificationID,
		ModificationSignatures: []mlao.ModificationSignature{},
	}

	buf, err := json.Marshal(laoState)
	require.NoError(t, err)

	msg := newMessage(t, organizer, organizerSK, buf)

	return msg
}

func NewRollCallCreateMsg(t *testing.T, sender, laoName, createID string, creation, start, end int64,
	senderSK kyber.Scalar) mmessage.Message {
	rollCallCreate := mlao.RollCallCreate{
		Object:        mmessage.RollCallObject,
		Action:        mmessage.RollCallActionCreate,
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
	senderSK kyber.Scalar) mmessage.Message {

	rollCallOpen := mlao.RollCallOpen{
		Object:   mmessage.RollCallObject,
		Action:   mmessage.RollCallActionOpen,
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
	senderSK kyber.Scalar) mmessage.Message {

	rollCallReOpen := mlao.RollCallOpen{
		Object:   mmessage.RollCallObject,
		Action:   mmessage.RollCallActionReOpen,
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
	senderSK kyber.Scalar) mmessage.Message {

	rollCallClose := mlao.RollCallClose{
		Object:    mmessage.RollCallObject,
		Action:    mmessage.RollCallActionClose,
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
	createdAt, start, end int64, questions []mlao.ElectionSetupQuestion, senderSK kyber.Scalar) mmessage.Message {

	electionSetup := mlao.ElectionSetup{
		Object:    mmessage.ElectionObject,
		Action:    mmessage.ElectionActionSetup,
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
