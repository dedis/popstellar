package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/channel"
	mfederation2 "popstellar/internal/handler/channel/federation/mfederation"
	"popstellar/internal/handler/message/mmessage"
	"testing"
)

func NewFederationChallengeRequest(t *testing.T, sender string,
	timestamp int64, senderSk kyber.Scalar) mmessage.Message {

	challengeRequest := mfederation2.FederationChallengeRequest{
		Object:    channel.FederationObject,
		Action:    channel.FederationActionChallengeRequest,
		Timestamp: timestamp,
	}

	challengeRequestBuf, err := json.Marshal(challengeRequest)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSk, challengeRequestBuf)

	return msg
}

func NewFederationChallenge(t *testing.T, sender string, value string,
	validUntil int64, senderSk kyber.Scalar) mmessage.Message {

	challenge := mfederation2.FederationChallenge{
		Object:     channel.FederationObject,
		Action:     channel.FederationActionChallenge,
		Value:      value,
		ValidUntil: validUntil,
	}

	challengeBuf, err := json.Marshal(challenge)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSk, challengeBuf)

	return msg
}

func NewFederationExpect(t *testing.T, sender, laoId, serverAddress,
	publicKey string, challengeMsg mmessage.Message,
	senderSk kyber.Scalar) mmessage.Message {

	expect := mfederation2.FederationExpect{
		Object:        channel.FederationObject,
		Action:        channel.FederationActionExpect,
		LaoId:         laoId,
		ServerAddress: serverAddress,
		PublicKey:     publicKey,
		ChallengeMsg:  challengeMsg,
	}

	expectBuf, err := json.Marshal(expect)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSk, expectBuf)

	return msg
}

func NewFederationInit(t *testing.T, sender, laoId, serverAddress,
	publicKey string, challengeMsg mmessage.Message,
	senderSk kyber.Scalar) mmessage.Message {

	expect := mfederation2.FederationInit{
		Object:        channel.FederationObject,
		Action:        channel.FederationActionInit,
		LaoId:         laoId,
		ServerAddress: serverAddress,
		PublicKey:     publicKey,
		ChallengeMsg:  challengeMsg,
	}

	expectBuf, err := json.Marshal(expect)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSk, expectBuf)

	return msg
}

func NewSuccessFederationResult(t *testing.T, sender, publicKey string,
	challengeMsg mmessage.Message, senderSk kyber.Scalar) mmessage.Message {

	result := mfederation2.FederationResult{
		Object:       channel.FederationObject,
		Action:       channel.FederationActionResult,
		Status:       "success",
		Reason:       "",
		PublicKey:    publicKey,
		ChallengeMsg: challengeMsg,
	}

	resultBuf, err := json.Marshal(result)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSk, resultBuf)

	return msg
}

func NewFailedFederationResult(t *testing.T, sender, reason string,
	challengeMsg mmessage.Message, senderSk kyber.Scalar) mmessage.Message {

	result := mfederation2.FederationResult{
		Object:       channel.FederationObject,
		Action:       channel.FederationActionResult,
		Status:       "failure",
		Reason:       reason,
		PublicKey:    "",
		ChallengeMsg: challengeMsg,
	}

	resultBuf, err := json.Marshal(result)
	require.NoError(t, err)

	msg := newMessage(t, sender, senderSk, resultBuf)

	return msg
}
