package handler

import (
	"database/sql"
	"encoding/base64"
	"fmt"
	"github.com/stretchr/testify/require"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/database/repository"
	"popstellar/internal/popserver/generatortest"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	"popstellar/message/messagedata"
	"testing"
	"time"
)

func Test_handleChannelFederation(t *testing.T) {
	var args []input

	mockRepository := repository.NewMockRepository(t)
	database.SetDatabase(mockRepository)

	subs := types.NewSubscribers()
	queries := types.NewQueries(&noLog)
	peers := types.NewPeers()

	organizerPk, organizerSk := generateKeys()
	organizer2Pk, organizer2Sk := generateKeys()
	notOrganizerPk, notOrganizerSk := generateKeys()
	serverPk, serverSk := generateKeys()

	config.SetConfig(organizerPk, serverPk, serverSk, "client", "server")

	state.SetState(subs, peers, queries)

	organizerBuf, err := organizerPk.MarshalBinary()
	require.NoError(t, err)
	organizer := base64.URLEncoding.EncodeToString(organizerBuf)

	organizer2Buf, err := organizer2Pk.MarshalBinary()
	require.NoError(t, err)
	organizer2 := base64.URLEncoding.EncodeToString(organizer2Buf)

	notOrganizerBuf, err := notOrganizerPk.MarshalBinary()
	require.NoError(t, err)
	notOrganizer := base64.URLEncoding.EncodeToString(notOrganizerBuf)

	laoID := "lsWUv1bKBQ0t1DqWZTFwb0nhLsP_EtfGoXHny4hsrwA="
	laoPath := fmt.Sprintf("/root/%s", laoID)
	channelPath := fmt.Sprintf("/root/%s/federation", laoID)

	serverAddressA := "ws://localhost:9801/client"
	value := "82eadde2a4ba832518b90bb93c8480ee1ae16a91d5efe9281e91e2ec11da03e4"
	validUntil := time.Now().Add(5 * time.Minute).Unix()

	mockRepository.On("GetOrganizerPubKey", laoPath).Return(organizerPk, nil)

	// Test 1 Error when FederationChallengeRequest sender is not the same as
	// the lao organizer
	args = append(args, input{
		name:    "Test 1",
		channel: channelPath,
		msg: generatortest.NewFederationChallengeRequest(t,
			notOrganizer, validUntil, notOrganizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channel",
	})

	// Test 2 Error when FederationChallengeRequest timestamp is negative
	args = append(args, input{
		name:    "Test 2",
		channel: channelPath,
		msg: generatortest.NewFederationChallengeRequest(t,
			organizer, -1, organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 3 Error when FederationExpect sender is not the same as the lao
	// organizer
	args = append(args, input{
		name:    "Test 3",
		channel: channelPath,
		msg: generatortest.NewFederationExpect(t, notOrganizer, laoID,
			serverAddressA, organizer2,
			generatortest.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			notOrganizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channel",
	})

	// Test 4 Error when FederationExpect serverAddress is not valid format
	args = append(args, input{
		name:    "Test 4",
		channel: channelPath,
		msg: generatortest.NewFederationExpect(t, organizer, laoID,
			"ws:localhost:12345/client", organizer2,
			generatortest.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 5 Error when FederationExpect publicKey is not valid format
	args = append(args, input{
		name:    "Test 5",
		channel: channelPath,
		msg: generatortest.NewFederationExpect(t, organizer, laoID,
			serverAddressA, "organizer2",
			generatortest.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 6 Error when FederationExpect laoId is not valid format
	args = append(args, input{
		name:    "Test 6",
		channel: channelPath,
		msg: generatortest.NewFederationExpect(t, organizer, "laoID",
			serverAddressA, organizer2,
			generatortest.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 7 Error when FederationExpect challenge message is not a challenge
	args = append(args, input{
		name:    "Test 7",
		channel: channelPath,
		msg: generatortest.NewFederationExpect(t, organizer, laoID,
			serverAddressA, organizer2,
			generatortest.NewFederationChallengeRequest(t, organizer,
				validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "failed to unmarshal jsonData",
	})

	// Test 8 Error when FederationExpect challenge is not from organizer
	args = append(args, input{
		name:    "Test 8",
		channel: channelPath,
		msg: generatortest.NewFederationExpect(t, organizer, laoID,
			serverAddressA, organizer2,
			generatortest.NewFederationChallenge(t, notOrganizer,
				value, validUntil, notOrganizerSk),
			organizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channel",
	})

	// Test 9 Error when FederationInit sender is not the same as the lao
	// organizer
	args = append(args, input{
		name:    "Test 9",
		channel: channelPath,
		msg: generatortest.NewFederationInit(t, notOrganizer, laoID,
			serverAddressA, organizer2,
			generatortest.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			notOrganizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channel",
	})

	// Test 10 Error when FederationInit serverAddress is not valid format
	args = append(args, input{
		name:    "Test 10",
		channel: channelPath,
		msg: generatortest.NewFederationInit(t, organizer, laoID,
			"ws:localhost:12345/client", organizer2,
			generatortest.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 11 Error when FederationInit publicKey is not valid format
	args = append(args, input{
		name:    "Test 11",
		channel: channelPath,
		msg: generatortest.NewFederationInit(t, organizer, laoID,
			serverAddressA, "organizer2",
			generatortest.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 12 Error when FederationInit laoId is not valid format
	args = append(args, input{
		name:    "Test 12",
		channel: channelPath,
		msg: generatortest.NewFederationInit(t, organizer, "laoID",
			serverAddressA, organizer2,
			generatortest.NewFederationChallenge(t, organizer,
				value, validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "VerifyJSON",
	})

	// Test 13 Error when FederationInit challenge message is not a challenge
	args = append(args, input{
		name:    "Test 13",
		channel: channelPath,
		msg: generatortest.NewFederationInit(t, organizer, laoID,
			serverAddressA, organizer2,
			generatortest.NewFederationChallengeRequest(t, organizer,
				validUntil, organizerSk),
			organizerSk),
		isError:  true,
		contains: "failed to unmarshal jsonData",
	})

	// Test 14 Error when FederationInit challenge is not from organizer
	args = append(args, input{
		name:    "Test 14",
		channel: channelPath,
		msg: generatortest.NewFederationInit(t, organizer, laoID,
			serverAddressA, organizer2,
			generatortest.NewFederationChallenge(t, notOrganizer,
				value, validUntil, notOrganizerSk),
			organizerSk),
		isError:  true,
		contains: "sender is not the organizer of the channel",
	})

	federationChallenge1 := messagedata.FederationChallenge{
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	mockRepository.On("GetFederationExpect", organizer,
		notOrganizer, federationChallenge1).Return(messagedata.
		FederationExpect{}, sql.ErrNoRows)

	// Test 15 Error when FederationChallenge is received without any
	// matching FederationExpect
	args = append(args, input{
		name:    "Test 15",
		channel: channelPath,
		msg: generatortest.NewFederationChallenge(t, notOrganizer, value,
			validUntil, notOrganizerSk),
		isError:  true,
		contains: "failed to get federation expect",
	})

	require.NotEqual(t, notOrganizerPk, organizer2Sk)

	for _, arg := range args {
		t.Run(arg.name, func(t *testing.T) {
			errAnswer := handleChannelFederation(arg.channel, arg.msg)
			if arg.isError {
				require.Contains(t, errAnswer.Error(), arg.contains)
			} else {
				require.Nil(t, errAnswer)
			}
		})
	}
}
