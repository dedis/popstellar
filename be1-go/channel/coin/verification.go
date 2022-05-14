package coin

import (
	"golang.org/x/xerrors"
	"popstellar/message/messagedata"
	"strconv"
)

func (c *Channel) verifyMessageTransactionPost(transactionPost messagedata.PostTransaction) error {
	c.log.Info().Msgf("verifying transaction#post message transaction_id validity %s",
		transactionPost.TransactionId)

	locktime := strconv.Itoa(transactionPost.Transaction.Locktime)

	version := strconv.Itoa(transactionPost.Transaction.Version)

	var transactionIdBefHash []string

	for _, inp := range transactionPost.Transaction.Inputs {
		pubKey := inp.Script.PubKey
		transactionIdBefHash = append(transactionIdBefHash, pubKey)

		sig := inp.Script.Sig
		transactionIdBefHash = append(transactionIdBefHash, sig)

		typee := inp.Script.Type
		transactionIdBefHash = append(transactionIdBefHash, typee)

		hash := inp.Hash
		transactionIdBefHash = append(transactionIdBefHash, hash)

		index := strconv.Itoa(inp.Index)
		transactionIdBefHash = append(transactionIdBefHash, index)
	}

	transactionIdBefHash = append(transactionIdBefHash, locktime)

	for _, out := range transactionPost.Transaction.Outputs {
		pubKey := out.Script.PubKeyHash
		transactionIdBefHash = append(transactionIdBefHash, pubKey)

		typee := out.Script.Type
		transactionIdBefHash = append(transactionIdBefHash, typee)

		value := strconv.Itoa(out.Value)
		transactionIdBefHash = append(transactionIdBefHash, value)
	}

	transactionIdBefHash = append(transactionIdBefHash, version)

	computedTransactionId := messagedata.Hash(transactionIdBefHash...)

	if transactionPost.TransactionId != computedTransactionId {
		return xerrors.Errorf("Transaction Id is not valid, value=%s, computed=%s", transactionPost.TransactionId, computedTransactionId)
	}

	return nil
}
