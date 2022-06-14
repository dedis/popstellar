package messagedata

import (
	"encoding/json"
	"os"
	"path/filepath"
	"popstellar/message/messagedata"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Coin_Post_Transaction(t *testing.T) {
	file := filepath.Join(relativeExamplePath, "coin", "post_transaction.json")

	buf, err := os.ReadFile(file)
	require.NoError(t, err)

	object, action, err := messagedata.GetObjectAndAction(buf)
	require.NoError(t, err)

	require.Equal(t, "coin", object)
	require.Equal(t, "post_transaction", action)

	var msg messagedata.PostTransaction

	err = json.Unmarshal(buf, &msg)
	require.NoError(t, err)

	require.Equal(t, "coin", msg.Object)
	require.Equal(t, "post_transaction", msg.Action)
	require.Equal(t, "ESQimXKtIrt85EMSsmGoQ2kE2y1-ae6SAvp52Xp3KOQ=", msg.TransactionID)
	require.Equal(t, 1, msg.Transaction.Version)
	require.Equal(t, "01N1Y8twdu7wpdz5HLnkIeQSeuKpkNcQHeKF7XabLYU=", msg.Transaction.Inputs[0].Hash)
	require.Equal(t, 0, msg.Transaction.Inputs[0].Index)
	require.Equal(t, "P2PKH", msg.Transaction.Inputs[0].Script.Type)
	require.Equal(t, "oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms=", msg.Transaction.Inputs[0].Script.PubKey)
	require.Equal(t, "6hY16bDKnb7bRA5j7IMDR1CJDqAJKOLuQRgKxdpYQIrtSTVTRjo5jigqPhYQEPcK5a1WAF86V739ENFnlp6YCw==", msg.Transaction.Inputs[0].Script.Sig)
	require.EqualValues(t, 32, msg.Transaction.Outputs[0].Value)
	require.Equal(t, "P2PKH", msg.Transaction.Outputs[0].Script.Type)
	require.Equal(t, "-_qR4IHwsiq50raa8jURNArds54=", msg.Transaction.Outputs[0].Script.PubKeyHash)
	require.Equal(t, 0, msg.Transaction.Locktime)
}

func Test_Coin_Post_Transaction_Interface_Functions(t *testing.T) {
	var msg messagedata.PostTransaction

	require.Equal(t, messagedata.CoinObject, msg.GetObject())
	require.Equal(t, messagedata.CoinActionPostTransaction, msg.GetAction())
	require.Empty(t, msg.NewEmpty())
}

func Test_Coin_Post_Transaction_Verify(t *testing.T) {
	var postTransaction messagedata.PostTransaction

	object, action := "coin", "post_transaction"

	getTestBadExample := func(file string) func(*testing.T) {
		return func(t *testing.T) {
			// read the bad example file
			buf, err := os.ReadFile(filepath.Join(relativeExamplePath, "coin", file))
			require.NoError(t, err)

			obj, act, err := messagedata.GetObjectAndAction(buf)
			require.NoError(t, err)

			require.Equal(t, object, obj)
			require.Equal(t, action, act)

			err = json.Unmarshal(buf, &postTransaction)
			require.NoError(t, err)

			err = postTransaction.Verify()
			require.Error(t, err)
		}
	}

	t.Run("transaction id not base64", getTestBadExample("wrong_post_transaction_transaction_id_not_base_64.json"))
	t.Run("transaction id is wrong", getTestBadExample("post_transaction_wrong_transaction_id.json"))
}
