package hreaction

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/generator"
	"popstellar/internal/handler/messagedata/reaction/hreaction/mocks"
	"popstellar/internal/handler/messagedata/root/hroot"
	"popstellar/internal/message/query/method/message"
	"popstellar/internal/state"
	"popstellar/internal/validation"
	"strings"
	"testing"
	"time"
)

func Test_handleChannelReaction(t *testing.T) {
	type input struct {
		name        string
		channelPath string
		msg         message.Message
		isError     bool
		contains    string
	}

	subs := state.NewSubscribers()

	db := mocks.NewRepository(t)

	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	reactionHandler := New(subs, db, schema)

	sender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="
	//wrongSender := "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="
	chirpID := "AAAAdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="
	invalidChirpID := "NotGooD"

	var args []input

	// Test 1: successfully add a reaction 👍

	laoID := "lao1"
	channelID := hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions

	args = append(args, input{
		name:        "Test 1",
		channelPath: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👍", chirpID, time.Now().Unix(), db,
			false, false, subs),
		isError:  false,
		contains: "",
	})

	// Test 2: successfully add a reaction 👎

	laoID = "lao2"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions

	args = append(args, input{
		name:        "Test 2",
		channelPath: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👎", chirpID, time.Now().Unix(), db,
			false, false, subs),
		isError:  false,
		contains: "",
	})

	// Test 3: successfully add a reaction ❤️

	laoID = "lao3"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions

	args = append(args, input{
		name:        "Test 3",
		channelPath: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "❤️", chirpID, time.Now().Unix(), db,
			false, false, subs),
		isError:  false,
		contains: "",
	})

	// Test 4: failed to add a reaction because wrong chirpID

	laoID = "lao4"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions

	args = append(args, input{
		name:        "Test 4",
		channelPath: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👍", invalidChirpID, time.Now().Unix(), db,
			true, false, subs),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 5: failed to add a reaction because negative timestamp

	laoID = "lao5"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions

	args = append(args, input{
		name:        "Test 5",
		channelPath: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👍", chirpID, -1, db,
			true, false, subs),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 6: failed to add a reaction because didn't participate in roll-call

	laoID = "lao6"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions

	args = append(args, input{
		name:        "Test 6",
		channelPath: channelID,
		msg: newReactionAddMsg(t, channelID, sender, "👍", chirpID, time.Now().Unix(), db,
			false, true, subs),
		isError:  true,
		contains: "user not inside roll-call",
	})

	// Test 7: successfully delete a reaction

	laoID = "lao7"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions
	reactionID := "AAAAdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	args = append(args, input{
		name:        "Test 7",
		channelPath: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, time.Now().Unix(), db,
			false, false, false, false, subs),
		isError:  false,
		contains: "",
	})

	// Test 8: failed to delete a reaction because negative timestamp

	laoID = "lao8"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions
	reactionID = "AAAAABu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	args = append(args, input{
		name:        "Test 8",
		channelPath: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, -1, db,
			true, false, false, false, subs),
		isError:  true,
		contains: "invalid message field",
	})

	// Test 9: failed to delete a reaction because reaction doesn't exist

	laoID = "lao9"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions
	reactionID = "AAAAdBB8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sK="

	args = append(args, input{
		name:        "Test 9",
		channelPath: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, time.Now().Unix(), db,
			false, true, false, false, subs),
		isError:  true,
		contains: "unknown reaction",
	})

	// Test 10: failed to delete a reaction because not owner

	laoID = "lao10"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions
	reactionID = "AAAAdBB8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4KK="

	args = append(args, input{
		name:        "Test 10",
		channelPath: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, time.Now().Unix(), db,
			false, false, true, false, subs),
		isError:  true,
		contains: "only the owner of the reaction can delete it",
	})

	// Test 11: failed to delete a reaction because didn't participate in roll-call

	laoID = "lao11"
	channelID = hroot.RootPrefix + laoID + hroot.Social + hroot.Reactions
	reactionID = "AAAAdBB8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dRYKK="

	args = append(args, input{
		name:        "Test 11",
		channelPath: channelID,
		msg: newReactionDeleteMsg(t, channelID, sender, reactionID, time.Now().Unix(), db,
			false, false, false, true, subs),
		isError:  true,
		contains: "user not inside roll-call",
	})

	// Tests all cases

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err := reactionHandler.Handle(arg.channelPath, arg.msg)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}

}

func newReactionAddMsg(t *testing.T, channelID string, sender string, reactionCodePoint, chirpID string, timestamp int64,
	db *mocks.Repository, hasInvalidField, isNotAttendee bool, subs *state.Subscribers) message.Message {

	msg := generator.NewReactionAddMsg(t, sender, nil, reactionCodePoint, chirpID, timestamp)

	err := subs.AddChannel(channelID)
	require.NoError(t, err)

	laoPath, _ := strings.CutSuffix(channelID, hroot.Social+hroot.Reactions)

	if !hasInvalidField && !isNotAttendee {
		db.On("IsAttendee", laoPath, sender).Return(true, nil)
		db.On("StoreMessageAndData", channelID, msg).Return(nil)
	}

	if isNotAttendee {
		db.On("IsAttendee", laoPath, sender).Return(false, nil)
	}

	return msg
}

func newReactionDeleteMsg(t *testing.T, channelID string, sender string, reactionID string, timestamp int64,
	db *mocks.Repository, hasInvalidField, hasNotReaction, isNotOwner, isNotAttendee bool,
	subs *state.Subscribers) message.Message {

	msg := generator.NewReactionDeleteMsg(t, sender, nil, reactionID, timestamp)

	err := subs.AddChannel(channelID)
	require.NoError(t, err)

	laoPath, _ := strings.CutSuffix(channelID, hroot.Social+hroot.Reactions)

	if !hasInvalidField && !hasNotReaction && !isNotOwner && !isNotAttendee {
		db.On("IsAttendee", laoPath, sender).Return(true, nil)

		db.On("GetReactionSender", reactionID).Return(sender, nil)

		db.On("StoreMessageAndData", channelID, msg).Return(nil)
	}

	if hasNotReaction {
		db.On("IsAttendee", laoPath, sender).Return(true, nil)

		db.On("GetReactionSender", reactionID).Return("", nil)
	}

	if isNotOwner {
		db.On("IsAttendee", laoPath, sender).Return(true, nil)

		db.On("GetReactionSender", reactionID).Return("notSender", nil)
	}

	if isNotAttendee {
		db.On("IsAttendee", laoPath, sender).Return(false, nil)
	}

	return msg
}
