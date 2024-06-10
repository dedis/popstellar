package helection

import (
	"encoding/base64"
	"fmt"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"popstellar/internal/handler/generator"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata"
	"popstellar/internal/handler/messagedata/election/helection/mocks"
	"popstellar/internal/handler/messagedata/election/telection"
	"popstellar/internal/handler/messagedata/lao/mlao"
	"popstellar/internal/state"
	"popstellar/internal/validation"
	"testing"
)

const (
	ownerPubBuf64 = "3yPmdBu8DM7jT30IKqkPjuFFIHnubO0z4E0dV7dR4sY="
	wrongSender   = "M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU="
)

func Test_handleChannelElection(t *testing.T) {
	type input struct {
		name        string
		channelPath string
		msg         mmessage.Message
		isError     bool
		contains    string
	}

	var args []input

	subs := state.NewSubscribers()

	db := mocks.NewRepository(t)

	schema, err := validation.NewSchemaValidator()
	require.NoError(t, err)

	ownerPubBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(ownerPubBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	conf := state.CreateConfig(ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")

	election := New(conf, subs, db, schema)

	laoID := base64.URLEncoding.EncodeToString([]byte("laoID"))
	electionID := base64.URLEncoding.EncodeToString([]byte("electionID"))
	channelPath := "/root/" + laoID + "/" + electionID

	// Test 1 Error when ElectionOpen sender is not the same as the lao organizer
	args = append(args, input{
		name: "Test 1",
		msg: newElectionOpenMsg(t, ownerPublicKey, wrongSender, laoID, electionID, channelPath, "",
			-1, true, db),
		channelPath: channelPath,
		isError:     true,
		contains:    "sender is not the organizer of the channel",
	})

	wrongChannelPath := "/root/" + base64.URLEncoding.EncodeToString([]byte("wrongLaoID")) + "/" + electionID

	// Test 2 Error when ElectionOpen lao id is not the same as the channel
	args = append(args, input{
		name: "Test 2",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, wrongChannelPath, "",
			-1, true, db),
		channelPath: wrongChannelPath,
		isError:     true,
		contains:    "lao id is not the same as the channel",
	})

	wrongChannelPath = "/root/" + laoID + "/" + base64.URLEncoding.EncodeToString([]byte("wrongElectionID"))

	// Test 3 Error when ElectionOpen election id is not the same as the channel
	args = append(args, input{
		name: "Test 3",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, wrongChannelPath, "",
			-1, true, db),
		channelPath: wrongChannelPath,
		isError:     true,
		contains:    "election id is not the same as the channel",
	})

	// Test 4 Error when Election is already started or ended
	args = append(args, input{
		name: "Test 4",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionOpen,
			-1, true, db),
		channelPath: channelPath,
		isError:     true,
		contains:    "election is already started or ended",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID2"))
	channelPath = "/root/" + laoID + "/" + electionID
	// Test 5 Error when ElectionOpen opened at before createdAt
	args = append(args, input{
		name: "Test 5",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionSetup,
			2, true, db),
		channelPath: channelPath,
		isError:     true,
		contains:    "election open cannot have a creation time prior to election setup",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID3"))
	channelPath = "/root/" + laoID + "/" + electionID

	err = subs.AddChannel(channelPath)
	require.NoError(t, err)

	// Test 6: Success when ElectionOpen is valid
	args = append(args, input{
		name: "Test 6",
		msg: newElectionOpenMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionSetup,
			1, false, db),
		channelPath: channelPath,
		isError:     false,
		contains:    "",
	})

	laoID = base64.URLEncoding.EncodeToString([]byte("electionID4"))
	channelPath = "/root/" + laoID + "/" + electionID

	// Test 7 Error when ElectionEnd sender is not the same as the lao organizer
	args = append(args, input{
		name: "Test 7",
		msg: newElectionEndMsg(t, ownerPublicKey, wrongSender, laoID, electionID, channelPath, "", "",
			-1, true, db),
		channelPath: channelPath,
		isError:     true,
		contains:    "sender is not the organizer of the channel",
	})

	wrongChannelPath = "/root/" + base64.URLEncoding.EncodeToString([]byte("wrongLaoID2")) + "/" + electionID

	// Test 8 Error when ElectionEnd lao id is not the same as the channel
	args = append(args, input{
		name: "Test 8",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, wrongChannelPath, "", "",
			-1, true, db),
		channelPath: wrongChannelPath,
		isError:     true,
		contains:    "lao id is not the same as the channel",
	})

	wrongChannelPath = "/root/" + laoID + "/" + base64.URLEncoding.EncodeToString([]byte("wrongElectionID2"))

	// Test 9 Error when ElectionEnd election id is not the same as the channel
	args = append(args, input{
		name: "Test 9",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, wrongChannelPath, "", "",
			-1, true, db),
		channelPath: wrongChannelPath,
		isError:     true,
		contains:    "election id is not the same as the channel",
	})

	// Test 10 Error when ElectionEnd is not started
	args = append(args, input{
		name: "Test 10",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionEnd, "",
			-1, true, db),
		channelPath: channelPath,
		isError:     true,
		contains:    "election was not started",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID5"))
	channelPath = "/root/" + laoID + "/" + electionID

	// Test 11 Error when ElectionEnd creation time is before ElectionSetup creation time
	args = append(args, input{
		name: "Test 11",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionOpen, "",
			2, true, db),
		channelPath: channelPath,
		isError:     true,
		contains:    "election end cannot have a creation time prior to election setup",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID6"))
	channelPath = "/root/" + laoID + "/" + electionID

	wrongVotes := messagedata.Hash("wrongVotes")

	// Test 12 Error when ElectionEnd is not the expected hash
	args = append(args, input{
		name: "Test 12",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionOpen, wrongVotes,
			1, true, db),
		channelPath: channelPath,
		isError:     true,
		contains:    fmt.Sprintf("registered votes is %s, should be sorted and equal to", wrongVotes),
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID7"))
	channelPath = "/root/" + laoID + "/" + electionID

	registeredVotes := messagedata.Hash("voteID1", "voteID2", "voteID3")

	err = subs.AddChannel(channelPath)
	require.NoError(t, err)

	// Test 13: Success when ElectionEnd is valid
	args = append(args, input{
		name: "Test 13",
		msg: newElectionEndMsg(t, ownerPublicKey, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionOpen, registeredVotes,
			1, false, db),
		channelPath: channelPath,
		isError:     false,
		contains:    "",
	})

	votes := []generator.VoteInt{
		{
			ID:       base64.URLEncoding.EncodeToString([]byte("voteID1")),
			Question: base64.URLEncoding.EncodeToString([]byte("questionID1")),
			Vote:     1,
		},
	}

	// Test 14 Error when VoteCastVote sender is not the same as the lao organizer
	args = append(args, input{
		name: "Test 14",
		msg: newVoteCastVoteIntMsg(t, wrongSender, laoID, electionID, channelPath, "", "",
			-1, votes, nil, ownerPublicKey, db, true),
		channelPath: channelPath,
		isError:     true,
		contains:    "sender is not an attendee or the organizer of the election",
	})

	// Test 15 Error when VoteCastVote lao id is not the same as the channel
	wrongChannelPath = "/root/" + base64.URLEncoding.EncodeToString([]byte("wrongLaoID3")) + "/" + electionID

	args = append(args, input{
		name: "Test 15",
		msg: newVoteCastVoteIntMsg(t, ownerPubBuf64, laoID, electionID, wrongChannelPath, "", "",
			-1, votes, nil, ownerPublicKey, db, true),
		channelPath: wrongChannelPath,
		isError:     true,
		contains:    "lao id is not the same as the channel",
	})

	// Test 16 Error when VoteCastVote election id is not the same as the channel
	wrongChannelPath = "/root/" + laoID + "/" + base64.URLEncoding.EncodeToString([]byte("wrongElectionID3"))

	args = append(args, input{
		name: "Test 16",
		msg: newVoteCastVoteIntMsg(t, ownerPubBuf64, laoID, electionID, wrongChannelPath, "", "",
			-1, votes, nil, ownerPublicKey, db, true),
		channelPath: wrongChannelPath,
		isError:     true,
		contains:    "election id is not the same as the channel",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID9"))
	channelPath = "/root/" + laoID + "/" + electionID

	//Test 17 Error when VoteCastVote createdAt is before electionSetup createdAt
	args = append(args, input{
		name: "Test 17",
		msg: newVoteCastVoteIntMsg(t, ownerPubBuf64, laoID, electionID, channelPath, "", "",
			2, votes, nil, ownerPublicKey, db, true),
		channelPath: channelPath,
		isError:     true,
		contains:    "cast vote cannot have a creation time prior to election setup",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID10"))
	channelPath = "/root/" + laoID + "/" + electionID

	//Test 18 Error when VoteCastVote question is not present in election setup
	questions := map[string]telection.Question{
		base64.URLEncoding.EncodeToString([]byte("questionID2")): {ID: []byte(base64.URLEncoding.EncodeToString([]byte("questionID2")))},
		base64.URLEncoding.EncodeToString([]byte("questionID3")): {ID: []byte(base64.URLEncoding.EncodeToString([]byte("questionID3")))},
	}

	args = append(args, input{
		name: "Test 18",
		msg: newVoteCastVoteIntMsg(t, ownerPubBuf64, laoID, electionID, channelPath, "", "",
			0, votes, questions, ownerPublicKey, db, true),
		channelPath: channelPath,
		isError:     true,
		contains:    "Question does not exist",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID11"))
	channelPath = "/root/" + laoID + "/" + electionID

	//Test 19 Error when VoteCastVote contains a string vote in an OpenBallot election
	stringVotes := []generator.VoteString{
		{
			ID:       base64.URLEncoding.EncodeToString([]byte("voteID1")),
			Question: base64.URLEncoding.EncodeToString([]byte("questionID2")),
			Vote:     base64.URLEncoding.EncodeToString([]byte("1")),
		},
	}

	args = append(args, input{
		name: "Test 19",
		msg: newVoteCastVoteStringMsg(t, ownerPubBuf64, laoID, electionID, channelPath, mlao.OpenBallot,
			0, stringVotes, questions, ownerPublicKey, db),
		channelPath: channelPath,
		isError:     true,
		contains:    "vote in open ballot should be an integer",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID12"))
	channelPath = "/root/" + laoID + "/" + electionID

	//Test 20 Error when VoteCastVote contains a int vote in an SecretBallot election
	intVotes := []generator.VoteInt{
		{
			ID:       base64.URLEncoding.EncodeToString([]byte("voteID1")),
			Question: base64.URLEncoding.EncodeToString([]byte("questionID2")),
			Vote:     1,
		},
	}

	args = append(args, input{
		name: "Test 20",
		msg: newVoteCastVoteIntMsg(t, ownerPubBuf64, laoID, electionID, channelPath, "", mlao.SecretBallot,
			0, intVotes, questions, ownerPublicKey, db, true),
		channelPath: channelPath,
		isError:     true,
		contains:    "vote in secret ballot should be a string",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID13"))
	channelPath = "/root/" + laoID + "/" + electionID

	//Test 21 Error when a vote ID in VoteCastVote is not the expected hash

	args = append(args, input{
		name: "Test 21",
		msg: newVoteCastVoteIntMsg(t, ownerPubBuf64, laoID, electionID, channelPath, "", mlao.OpenBallot,
			0, intVotes, questions, ownerPublicKey, db, true),
		channelPath: channelPath,
		isError:     true,
		contains:    "vote ID is not the expected hash",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID14"))
	channelPath = "/root/" + laoID + "/" + electionID

	//Test 22 Success when election is already ended
	questionID := base64.URLEncoding.EncodeToString([]byte("questionID2"))
	voteID := messagedata.Hash(voteFlag, electionID, questionID, "1")

	votes = []generator.VoteInt{
		{
			ID:       voteID,
			Question: questionID,
			Vote:     1,
		},
	}

	err = subs.AddChannel(channelPath)
	require.NoError(t, err)

	args = append(args, input{
		name: "Test 22",
		msg: newVoteCastVoteIntMsg(t, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionEnd, mlao.OpenBallot,
			0, votes, questions, ownerPublicKey, db, false),
		channelPath: channelPath,
		isError:     false,
		contains:    "",
	})

	//to avoid conflicts with the previous test
	electionID = base64.URLEncoding.EncodeToString([]byte("electionID14"))
	channelPath = "/root/" + laoID + "/" + electionID

	//Test 23 Success when election is started
	args = append(args, input{
		name: "Test 23",
		msg: newVoteCastVoteIntMsg(t, ownerPubBuf64, laoID, electionID, channelPath, messagedata.ElectionActionOpen, "",
			-1, votes, nil, ownerPublicKey, db, true),
		channelPath: channelPath,
		isError:     false,
		contains:    "",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			err = election.Handle(arg.channelPath, arg.msg)
			if arg.isError {
				require.Error(t, err, arg.contains)
			} else {
				require.NoError(t, err)
			}
		})
	}
}

func newElectionOpenMsg(t *testing.T, owner kyber.Point, sender, laoID, electionID, channelPath, state string,
	createdAt int64, isError bool, db *mocks.Repository) mmessage.Message {

	msg := generator.NewElectionOpenMsg(t, sender, laoID, electionID, 1, nil)

	db.On("GetLAOOrganizerPubKey", channelPath).Return(owner, nil)

	if createdAt >= 0 {
		db.On("GetElectionCreationTime", channelPath).Return(createdAt, nil)
	}

	if state != "" {
		db.On("IsElectionStartedOrEnded", channelPath).
			Return(state == messagedata.ElectionActionOpen || state == messagedata.ElectionActionEnd, nil)
	}

	if !isError {
		db.On("StoreMessageAndData", channelPath, msg).Return(nil)
	}

	return msg
}

func newElectionEndMsg(t *testing.T, owner kyber.Point, sender, laoID, electionID, channelPath, state, votes string,
	createdAt int64, isError bool, db *mocks.Repository) mmessage.Message {

	msg := generator.NewElectionCloseMsg(t, sender, laoID, electionID, votes, 1, nil)

	db.On("GetLAOOrganizerPubKey", channelPath).Return(owner, nil)

	if state != "" {
		db.On("IsElectionStarted", channelPath).
			Return(state == messagedata.ElectionActionOpen, nil)
	}

	if createdAt >= 0 {
		db.On("GetElectionCreationTime", channelPath).Return(createdAt, nil)
	}

	if votes != "" {
		questions := map[string]telection.Question{
			"questionID1": {
				ID: []byte("questionID1"),
				ValidVotes: map[string]telection.ValidVote{
					"voteID1": {
						ID: "voteID1",
					},
					"VoteID2": {
						ID: "voteID2",
					},
				},
			},
			"questionID2": {
				ID: []byte("questionID2"),
				ValidVotes: map[string]telection.ValidVote{
					"voteID3": {
						ID: "voteID3",
					},
				},
			},
		}

		db.On("GetElectionQuestionsWithValidVotes", channelPath).Return(questions, nil)
	}

	if !isError {
		db.On("GetElectionType", channelPath).Return(mlao.OpenBallot, nil)
		db.On("StoreElectionEndWithResult", channelPath, msg, mock.AnythingOfType("mmessage.Message")).
			Return(nil)
	}

	return msg
}

func newVoteCastVoteIntMsg(t *testing.T, sender, laoID, electionID, electionPath, state, electionType string,
	createdAt int64, votes []generator.VoteInt, questions map[string]telection.Question, owner kyber.Point,
	db *mocks.Repository, isEroor bool) mmessage.Message {

	msg := generator.NewVoteCastVoteIntMsg(t, sender, laoID, electionID, 1, votes, nil)
	db.On("GetLAOOrganizerPubKey", electionPath).Return(owner, nil)
	db.On("GetElectionAttendees", electionPath).Return(map[string]struct{}{ownerPubBuf64: {}}, nil)

	if state == messagedata.ElectionActionOpen {
		db.On("IsElectionStarted", electionPath).
			Return(true, nil)
	}

	if state == messagedata.ElectionActionEnd {
		db.On("IsElectionEnded", electionPath).
			Return(false, nil)
		db.On("IsElectionStarted", electionPath).
			Return(true, nil)
	}

	if createdAt >= 0 {
		db.On("GetElectionCreationTime", electionPath).Return(createdAt, nil)
	}

	if electionType != "" {
		db.On("GetElectionType", electionPath).Return(electionType, nil)
	}

	if questions != nil {
		db.On("GetElectionQuestions", electionPath).Return(questions, nil)
	}

	if !isEroor {
		db.On("StoreMessageAndData", electionPath, msg).Return(nil)
	}
	return msg
}

func newVoteCastVoteStringMsg(t *testing.T, sender, laoID, electionID, electionPath, electionType string,
	createdAt int64, votes []generator.VoteString, questions map[string]telection.Question, owner kyber.Point,
	db *mocks.Repository) mmessage.Message {

	msg := generator.NewVoteCastVoteStringMsg(t, sender, laoID, electionID, 1, votes, nil)
	db.On("GetLAOOrganizerPubKey", electionPath).Return(owner, nil)
	db.On("GetElectionAttendees", electionPath).Return(map[string]struct{}{ownerPubBuf64: {}}, nil)

	if createdAt >= 0 {
		db.On("GetElectionCreationTime", electionPath).Return(createdAt, nil)
	}

	if electionType != "" {
		db.On("GetElectionType", electionPath).Return(electionType, nil)
	}

	if questions != nil {
		db.On("GetElectionQuestions", electionPath).Return(questions, nil)
	}

	return msg
}
