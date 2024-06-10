package generator

import (
	"encoding/base64"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/internal/crypto"
	"popstellar/internal/handler/message/mmessage"
	"testing"
)

func newMessage(t *testing.T, sender string, senderSK kyber.Scalar, data []byte) mmessage.Message {
	data64 := base64.URLEncoding.EncodeToString(data)

	signature64 := base64.URLEncoding.EncodeToString([]byte(sender))

	if senderSK != nil {
		signatureBuf, err := schnorr.Sign(crypto.Suite, senderSK, data)
		require.NoError(t, err)

		signature64 = base64.URLEncoding.EncodeToString(signatureBuf)
	}

	messageID64 := mmessage.Hash(data64, signature64)

	return mmessage.Message{
		Data:              data64,
		Sender:            sender,
		Signature:         signature64,
		MessageID:         messageID64,
		WitnessSignatures: []mmessage.WitnessSignature{},
	}
}

func NewNothingMsg(t *testing.T, sender string, senderSK kyber.Scalar) mmessage.Message {
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

	return newMessage(t, sender, senderSK, buf)
}

func NewNothingQuery(t *testing.T, id int) []byte {
	wrongQuery := struct {
		Jsonrpc string `json:"Jsonrpc"`
		ID      int    `json:"ID"`
	}{
		Jsonrpc: "2.0",
		ID:      id,
	}

	wrongQueryBuf, err := json.Marshal(&wrongQuery)
	require.NoError(t, err)

	return wrongQueryBuf
}
