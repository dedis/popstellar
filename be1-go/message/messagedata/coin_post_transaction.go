package messagedata

import (
	"encoding/base64"
	"golang.org/x/xerrors"
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
	Value  int        `json:"value"`
	Script LockScript `json:"script"`
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

// Verify verifies that the PostTransaction message is valid
func (message PostTransaction) Verify() error {
	// verify id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.TransactionID)
	if err != nil {
		return xerrors.Errorf("transaction id is %s, should be base64URL encoded", message.TransactionID)
	}

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

		value := strconv.Itoa(out.Value)
		hashFields = append(hashFields, value)
	}

	hashFields = append(hashFields, version)

	expectedID := Hash(hashFields...)

	if message.TransactionID != expectedID {
		return xerrors.Errorf("transaction id is not valid: %s != %s", message.TransactionID, expectedID)
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
