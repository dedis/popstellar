package generatortest

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
)

func NewFederationChallengeRequest(t *testing.T, sender string,
	timestamp int64, senderSk kyber.Scalar) message.Message {

	challengeRequest := messagedata.FederationChallengeRequest{
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
	validUntil int64, senderSk kyber.Scalar) message.Message {

	challenge := messagedata.FederationChallenge{
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
	publicKey string, challengeMsg message.Message,
	senderSk kyber.Scalar) message.Message {

	expect := messagedata.FederationExpect{
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
	publicKey string, challengeMsg message.Message,
	senderSk kyber.Scalar) message.Message {

	expect := messagedata.FederationInit{
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
	challengeMsg message.Message, senderSk kyber.Scalar) message.Message {

	result := messagedata.FederationResult{
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
	challengeMsg message.Message, senderSk kyber.Scalar) message.Message {

	result := messagedata.FederationResult{
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
