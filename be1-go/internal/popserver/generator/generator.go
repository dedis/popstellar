package generator

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/crypto"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
	"testing"
)

func newMessage(t *testing.T, sender string, senderPK kyber.Scalar, data []byte) message.Message {
	data64 := base64.URLEncoding.EncodeToString(data)

	signature64 := "Signature"

	if senderPK != nil {
		signatureBuf, err := schnorr.Sign(crypto.Suite, senderPK, data)
		require.NoError(t, err)

		signature64 = base64.URLEncoding.EncodeToString(signatureBuf)
	}

	messageID64 := messagedata.Hash(data64, signature64)

	return message.Message{
		Data:              data64,
		Sender:            sender,
		Signature:         signature64,
		MessageID:         messageID64,
		WitnessSignatures: nil,
	}
}

func NewNothingMsg(t *testing.T, sender string, senderPK kyber.Scalar) message.Message {
	data := struct {
		Object string `json:"object"`
		Action string `json:"action"`
		Not    string `json:"not"`
	}{
		Object: "lao",
		Action: "nothing",
		Not:    "no",
	}
	buf, err := json.Marshal(data)
	require.NoError(t, err)

	return newMessage(t, sender, senderPK, buf)
}
