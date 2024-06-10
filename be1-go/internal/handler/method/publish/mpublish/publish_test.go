package mpublish

import (
	"embed"
	"encoding/json"
	"popstellar/internal/handler/jsonrpc/mjsonrpc"
	"testing"

	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testData embed.FS

func Test_Publish(t *testing.T) {
	buf, err := testData.ReadFile("testdata/publish.json")
	require.NoError(t, err)

	var msg mjsonrpc.JSONRPCBase

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "2.0", msg.JSONRPC)

	rpctype, err := mjsonrpc.GetType(buf)
	require.NoError(t, err)

	require.Equal(t, mjsonrpc.RPCTypeQuery, rpctype)

	var publish Publish

	err = json.Unmarshal(buf, &publish)
	require.NoError(t, err)

	require.Equal(t, "publish", publish.Method)
	require.Equal(t, "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=", publish.Params.Channel)
	require.Equal(t, 4, publish.ID)
	require.Equal(t, "eyJvYmplY3QiOiJyb2xsX2NhbGwiLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiUm9sbCBDYWxsIiwiY3JlYXRpb24iOjE2MzMwMzYxMjAsInByb3Bvc2VkX3N0YXJ0IjoxNjMzMDM2Mzg4LCJwcm9wb3NlZF9lbmQiOjE2MzMwMzk2ODgsImxvY2F0aW9uIjoiRVBGTCIsImlkIjoial9kSmhZYnpubXZNYnVMc0ZNQ2dzYlB5YjJ6Nm1vZ2VtSmFON1NWaHVVTT0ifQ==", publish.Params.Message.Data)
	require.Equal(t, "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", publish.Params.Message.Sender)
	require.Equal(t, "FFqBXhZSaKvBnTvrDNIeEYMpFKI5oIa5SAewquxIBHTTEyTIDnUgmvkwgccV9NrujPwDnRt1f4CIEqzXqhbjCw==", publish.Params.Message.Signature)
	require.Equal(t, "sD_PdryBuOr14_65h8L-e1lzdQpDWxUAngtu1uwqgEI=", publish.Params.Message.MessageID)
	require.Len(t, publish.Params.Message.WitnessSignatures, 0)
}
