package channel

import (
	"encoding/base64"
	"github.com/stretchr/testify/require"
	"popstellar/crypto"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"testing"
	"time"
)

func Test_handleChannelLao(t *testing.T) {
	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	err := state.SetState(t, subs, peers, queries)
	require.NoError(t, err)

	organizerBuf, err := base64.URLEncoding.DecodeString(ownerPubBuf64)
	require.NoError(t, err)

	ownerPublicKey := crypto.Suite.Point()
	err = ownerPublicKey.UnmarshalBinary(organizerBuf)
	require.NoError(t, err)

	serverSecretKey := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	serverPublicKey := crypto.Suite.Point().Mul(serverSecretKey, nil)

	err = config.SetConfig(t, ownerPublicKey, serverPublicKey, serverSecretKey, "clientAddress", "serverAddress")
	require.NoError(t, err)

	var args []input
	mockRepo, err := database.SetDatabase(t)
	require.NoError(t, err)

	ownerPubBuf, err := ownerPublicKey.MarshalBinary()
	require.NoError(t, err)
	owner := base64.URLEncoding.EncodeToString(ownerPubBuf)
	laoID := base64.URLEncoding.EncodeToString([]byte("laoID"))

	// Test 1:Success For LaoState message
	args = append(args, input{
		name:     "Test 1",
		msg:      NewLaoStateMsg(t, owner, laoID, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	creation := time.Now().Unix()
	start := creation + 2
	end := start + 1

	// Test 2: Error when RollCallCreate ID is not the expected hash
	args = append(args, input{
		name:     "Test 2",
		msg:      NewRollCallCreateMsg(t, owner, laoID, WrongLaoName, creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call id is",
	})

	// Test 3: Error when RollCallCreate proposed start is before creation
	args = append(args, input{
		name:     "Test 3",
		msg:      NewRollCallCreateMsg(t, owner, laoID, GoodLaoName, creation, creation-1, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call proposed start time should be greater than creation time",
	})

	// Test 4: Error when RollCallCreate proposed end is before proposed start
	args = append(args, input{
		name:     "Test 4",
		msg:      NewRollCallCreateMsg(t, owner, laoID, GoodLaoName, creation, start, start-1, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call proposed end should be greater than proposed start",
	})

	// Test 5: Success for RollCallCreate message
	args = append(args, input{
		name:     "Test 5",
		msg:      NewRollCallCreateMsg(t, owner, laoID, GoodLaoName, creation, start, end, false, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	opens := base64.URLEncoding.EncodeToString([]byte("opens"))
	wrongOpens := base64.URLEncoding.EncodeToString([]byte("wrongOpens"))

	// Test 6: Error when RollCallOpen ID is not the expected hash
	args = append(args, input{
		name:     "Test 6",
		msg:      NewRollCallOpenMsg(t, owner, laoID, wrongOpens, "", time.Now().Unix(), true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call update id is",
	})

	// Test 7: Error when RollCallOpen opens is not the same as previous RollCallCreate
	args = append(args, input{
		name:     "Test 7",
		msg:      NewRollCallOpenMsg(t, owner, laoID, opens, wrongOpens, time.Now().Unix(), true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "previous id does not exist",
	})

	// Test 8: Success for RollCallOpen message
	laoID = base64.URLEncoding.EncodeToString([]byte("laoID2"))
	args = append(args, input{
		name:     "Test 8",
		msg:      NewRollCallOpenMsg(t, owner, laoID, opens, opens, time.Now().Unix(), false, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	closes := base64.URLEncoding.EncodeToString([]byte("closes"))
	wrongCloses := base64.URLEncoding.EncodeToString([]byte("wrongCloses"))

	// Test 9: Error when RollCallClose ID is not the expected hash
	args = append(args, input{
		name:     "Test 9",
		msg:      NewRollCallCloseMsg(t, owner, laoID, wrongCloses, "", time.Now().Unix(), true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "roll call update id is",
	})

	// Test 10: Error when RollCallClose closes is not the same as previous RollCallOpen
	args = append(args, input{
		name:     "Test 10",
		msg:      NewRollCallCloseMsg(t, owner, laoID, closes, wrongCloses, time.Now().Unix(), true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "previous id does not exist",
	})

	// Test 11: Success for RollCallClose message
	laoID = base64.URLEncoding.EncodeToString([]byte("laoID3"))
	args = append(args, input{
		name:     "Test 11",
		msg:      NewRollCallCloseMsg(t, owner, laoID, closes, closes, time.Now().Unix(), false, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	electionsName := "electionName"
	question := "question"
	wrongQuestion := "wrongQuestion"
	// Test 12: Error when sender is not the organizer of the lao for ElectionSetup
	args = append(args, input{
		name: "Test 12",
		msg: NewElectionSetupMsg(t, ownerPublicKey, WrongSender, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "sender public key does not match organizer public key",
	})

	wrongLaoID := base64.URLEncoding.EncodeToString([]byte("wrongLaoID"))
	// Test 13: Error when ElectionSetup lao is not the same as the channel
	args = append(args, input{
		name: "Test 13",
		msg: NewElectionSetupMsg(t, ownerPublicKey, owner, wrongLaoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "lao id is",
	})

	// Test 14: Error when ElectionSetup ID is not the expected hash
	args = append(args, input{
		name: "Test 14",
		msg: NewElectionSetupMsg(t, ownerPublicKey, owner, laoID, laoID, "wrongName", question, messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "election id is",
	})

	// Test 15: Error when proposedStart is before createdAt
	args = append(args, input{
		name: "Test 15",
		msg: NewElectionSetupMsg(t, ownerPublicKey, owner, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, creation-1, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "election start should be greater that creation time",
	})

	// Test 16: Error when proposedEnd is before proposedStart
	args = append(args, input{
		name: "Test 16",
		msg: NewElectionSetupMsg(t, ownerPublicKey, owner, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, start-1, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "election end should be greater that start time",
	})

	// Test 17: Error when ElectionSetup question is empty
	args = append(args, input{
		name: "Test 17",
		msg: NewElectionSetupMsg(t, ownerPublicKey, owner, laoID, laoID, electionsName, "", messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "Question is empty",
	})

	//Test 18: Error when question hash is not the same as the expected hash
	args = append(args, input{
		name: "Test 18",
		msg: NewElectionSetupMsg(t, ownerPublicKey, owner, laoID, laoID, electionsName, wrongQuestion, messagedata.OpenBallot,
			creation, start, end, true, mockRepo),
		channel:  laoID,
		isError:  true,
		contains: "Question id is",
	})

	// Test 19: Success for ElectionSetup message
	laoID = base64.URLEncoding.EncodeToString([]byte("laoID4"))
	args = append(args, input{
		name: "Test 19",
		msg: NewElectionSetupMsg(t, ownerPublicKey, owner, laoID, laoID, electionsName, question, messagedata.OpenBallot,
			creation, start, end, false, mockRepo),
		channel:  laoID,
		isError:  false,
		contains: "",
	})

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelLao(arg.channel, arg.msg)
			if arg.isError {
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
			}
		})
	}
}
