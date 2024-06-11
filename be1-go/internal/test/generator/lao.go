package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/channel"
	mlao2 "popstellar/internal/handler/channel/lao/mlao"
	"popstellar/internal/handler/message/mmessage"
	"testing"
)

func NewLaoStateMsg(t *testing.T, organizer, laoID, name, modificationID string, creation, lastModified int64,
	organizerSK kyber.Scalar) mmessage.Message {
	laoState := mlao2.LaoState{
		Object:                 channel.LAOObject,
		Action:                 channel.LAOActionState,
		ID:                     laoID,
		Name:                   name,
		Creation:               creation,
		LastModified:           lastModified,
		Organizer:              organizer,
		Witnesses:              []string{},
		ModificationID:         modificationID,
		ModificationSignatures: []mlao2.ModificationSignature{},
	}

	buf, err := json.Marshal(laoState)
	require.NoError(t, err)

	msg := newMessage(t, organizer, organizerSK, buf)

	return msg
}

func NewRollCallCreateMsg(t *testing.T, sender, laoName, createID string, creation, start, end int64,
	senderSK kyber.Scalar) mmessage.Message {
	rollCallCreate := mlao2.RollCallCreate{
		Object:        channel.RollCallObject,
		Action:        channel.RollCallActionCreate,
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

	rollCallOpen := mlao2.RollCallOpen{
		Object:   channel.RollCallObject,
		Action:   channel.RollCallActionOpen,
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

	rollCallReOpen := mlao2.RollCallOpen{
		Object:   channel.RollCallObject,
		Action:   channel.RollCallActionReOpen,
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

	rollCallClose := mlao2.RollCallClose{
		Object:    channel.RollCallObject,
		Action:    channel.RollCallActionClose,
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
	createdAt, start, end int64, questions []mlao2.ElectionSetupQuestion, senderSK kyber.Scalar) mmessage.Message {

	electionSetup := mlao2.ElectionSetup{
		Object:    channel.ElectionObject,
		Action:    channel.ElectionActionSetup,
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
