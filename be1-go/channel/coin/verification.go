package coin

import (
	"crypto/sha256"
	"encoding/base64"
	"golang.org/x/xerrors"
	"popstellar/message/messagedata"
	"strconv"
)

func (c *Channel) verifyMessageTransactionPost(transactionPost messagedata.TransactionPost) error {
	c.log.Info().Msgf("verifying transaction#post message transaction_id validity %s",
		transactionPost.TransactionId)

	locktime := strconv.Itoa(transactionPost.Transaction.Locktime)
	locktimeLen := strconv.Itoa(len(locktime))

	version := strconv.Itoa(transactionPost.Transaction.Version)
	versionLen := strconv.Itoa(len(version))

	var transactionIdBefHash = locktimeLen + locktime

	for _, inp := range transactionPost.Transaction.Inputs {
		pubKey := inp.Script.PubKey
		pubKeyLen := strconv.Itoa(len(pubKey))
		transactionIdBefHash += pubKeyLen + pubKey

		sig := inp.Script.Sig
		sigLen := strconv.Itoa(len(sig))
		transactionIdBefHash += sigLen + sig

		typee := inp.Script.Type
		typeeLen := strconv.Itoa(len(typee))
		transactionIdBefHash += typeeLen + typee

		hash := inp.Hash
		hashLen := strconv.Itoa(len(hash))
		transactionIdBefHash += hashLen + hash

		index := strconv.Itoa(inp.Index)
		indexLen := strconv.Itoa(len(index))
		transactionIdBefHash += indexLen + index
	}

	for _, out := range transactionPost.Transaction.Outputs {
		pubKey := out.Script.PubKeyHash
		pubKeyLen := strconv.Itoa(len(pubKey))
		transactionIdBefHash += pubKeyLen + pubKey

		typee := out.Script.Type
		typeeLen := strconv.Itoa(len(typee))
		transactionIdBefHash += typeeLen + typee

		value := strconv.Itoa(out.Value)
		valueLen := strconv.Itoa(len(value))
		transactionIdBefHash += valueLen + value
	}

	transactionIdBefHash += versionLen + version
	computedTransactionId := Hash(transactionIdBefHash)

	if transactionPost.TransactionId != computedTransactionId {
		return xerrors.Errorf("Transaction Id is not valid, value=%s, computed=%s", transactionPost.TransactionId, computedTransactionId)
	}

	return nil
}

func Hash(strs ...string) string {
	h := sha256.New()
	for _, s := range strs {
		//h.Write([]byte(fmt.Sprintf("%d", len(s))))
		h.Write([]byte(s))
	}

	return base64.URLEncoding.EncodeToString(h.Sum(nil))
}
