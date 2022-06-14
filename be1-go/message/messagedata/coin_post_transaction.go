package messagedata

import (
	"bytes"
	"encoding/base64"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/channel/coin/uint53"
	"popstellar/crypto"
	"popstellar/message/answer"
	"strconv"
)

// PostTransaction defines a message data
type PostTransaction struct {
	Object        string      `json:"object"`
	Action        string      `json:"action"`
	TransactionID string      `json:"transaction_id"`
	Transaction   Transaction `json:"transaction"`
}

// Transaction defines the input and output account for the transaction
type Transaction struct {
	Version  int      `json:"version"`
	Inputs   []Input  `json:"inputs"`  // min 1
	Outputs  []Output `json:"outputs"` // min 1
	Locktime int      `json:"lock_time"`
}

// Input defines the source from the money used in transaction
type Input struct {
	Hash   string       `json:"tx_out_hash"`
	Index  int          `json:"tx_out_index"`
	Script UnlockScript `json:"script"`
}

// Output defines the destination for the money used in transaction
type Output struct {
	Value  uint53.Uint53 `json:"value"`
	Script LockScript    `json:"script"`
}

// LockScript defines the locking value for transaction
type LockScript struct {
	Type       string `json:"type"`
	PubKeyHash string `json:"pubkey_hash"`
}

// UnlockScript defines the unlocking value for transaction
type UnlockScript struct {
	Type   string `json:"type"`
	PubKey string `json:"pubkey"`
	Sig    string `json:"sig"`
}

// SumOutputs computes the sum of the amounts in all outputs.
// It may signal an overflow error
func (transaction Transaction) SumOutputs() (uint53.Uint53, error) {
	var acc uint53.Uint53 = 0
	var err error
	for _, out := range transaction.Outputs {
		acc, err = uint53.SafePlus(acc, out.Value)
		if err != nil {
			return 0, xerrors.Errorf("failed to perform uint53 addition: %v", err)
		}
	}

	return acc, nil
}

// Verify verifies that the PostTransaction message is valid
func (message PostTransaction) Verify() error {
	_, err := message.Transaction.SumOutputs()
	if err != nil {
		return xerrors.Errorf("failed to compute the sum of outputs: %w", err)
	}

	// verify id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(message.TransactionID)
	if err != nil {
		return xerrors.Errorf("transaction id is %s, should be base64URL "+
			"encoded", message.TransactionID)
	}

	err = message.verifyTransactionId()
	if err != nil {
		return xerrors.Errorf("failed to verify the transaction id: %v", err)
	}

	err = message.verifySignature()
	if err != nil {
		return xerrors.Errorf("failed to verify the signature: %v", err)
	}

	return nil
}

func (message PostTransaction) verifyTransactionId() error {
	locktime := strconv.Itoa(message.Transaction.Locktime)

	version := strconv.Itoa(message.Transaction.Version)

	var hashFields []string

	for _, inp := range message.Transaction.Inputs {
		pubKey := inp.Script.PubKey
		hashFields = append(hashFields, pubKey)

		sig := inp.Script.Sig
		hashFields = append(hashFields, sig)

		typee := inp.Script.Type
		hashFields = append(hashFields, typee)

		hash := inp.Hash
		hashFields = append(hashFields, hash)

		index := strconv.Itoa(inp.Index)
		hashFields = append(hashFields, index)
	}

	hashFields = append(hashFields, locktime)

	for _, out := range message.Transaction.Outputs {
		pubKey := out.Script.PubKeyHash
		hashFields = append(hashFields, pubKey)

		typee := out.Script.Type
		hashFields = append(hashFields, typee)

		value := strconv.FormatUint(out.Value, 10)
		hashFields = append(hashFields, value)
	}

	hashFields = append(hashFields, version)

	expectedID := Hash(hashFields...)

	if message.TransactionID != expectedID {
		return xerrors.Errorf("transaction id is not valid: %s != %s", message.TransactionID, expectedID)
	}

	return nil
}

func (message PostTransaction) verifySignature() error {
	var sigComp = new(bytes.Buffer)

	for _, inp := range message.Transaction.Inputs {
		hash := inp.Hash
		sigComp.WriteString(hash)

		index := strconv.Itoa(inp.Index)
		sigComp.WriteString(index)
	}

	for _, out := range message.Transaction.Outputs {
		value := strconv.FormatUint(out.Value, 10)
		sigComp.WriteString(value)

		typee := out.Script.Type
		sigComp.WriteString(typee)

		pubKey := out.Script.PubKeyHash
		sigComp.WriteString(pubKey)
	}

	for _, inp := range message.Transaction.Inputs {
		signatureBytes, err := base64.URLEncoding.DecodeString(inp.Script.Sig)
		if err != nil {
			return xerrors.Errorf("failed to decode signature string: %v", err)
		}

		publicKeySender, err := base64.URLEncoding.DecodeString(inp.Script.PubKey)
		if err != nil {
			return xerrors.Errorf("failed to decode public key string: %v", err)
		}

		err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, sigComp.Bytes(), signatureBytes)
		if err != nil {
			return answer.NewErrorf(-4, "failed to verify signature : %v", err)
		}
	}

	return nil
}

// GetObject implements MessageData
func (PostTransaction) GetObject() string {
	return CoinObject
}

// GetAction implements MessageData
func (PostTransaction) GetAction() string {
	return CoinActionPostTransaction
}

// NewEmpty implements MessageData
func (PostTransaction) NewEmpty() MessageData {
	return &PostTransaction{}
}
