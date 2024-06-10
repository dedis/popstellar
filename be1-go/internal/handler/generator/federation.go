package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/handler/message/mmessage"
	"popstellar/internal/handler/messagedata"
	mfederation2 "popstellar/internal/handler/messagedata/federation/mfederation"
	"testing"
)

func NewFederationChallengeRequest(t *testing.T, sender string,
	timestamp int64, senderSk kyber.Scalar) mmessage.Message {

	challengeRequest := mfederation2.FederationChallengeRequest{
		Object:    messagedata.FederationObject,
		Action:    messagedata.FederationActionChallengeRequest,
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
		Object:     messagedata.FederationObject,
		Action:     messagedata.FederationActionChallenge,
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
		Object:        messagedata.FederationObject,
		Action:        messagedata.FederationActionExpect,
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
		Object:        messagedata.FederationObject,
		Action:        messagedata.FederationActionInit,
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
		Object:       messagedata.FederationObject,
		Action:       messagedata.FederationActionResult,
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
		Object:       messagedata.FederationObject,
		Action:       messagedata.FederationActionResult,
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
