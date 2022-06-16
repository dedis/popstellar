package messagedata

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"golang.org/x/xerrors"
	"popstellar/crypto"
	"popstellar/message/answer"
	"strconv"
	"strings"
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
	for _, out := range message.Transaction.Outputs {
		if out.Value < 0 {
			return answer.NewErrorf(-4, "transaction output value is %d, "+
				"shouldn't be negative", out.Value)
		}
	}

	// verify id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(message.TransactionID)
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

func (message PostTransaction) verifySignature() error {
	var sigComp []string

	for _, inp := range message.Transaction.Inputs {
		hash := inp.Hash
		sigComp = append(sigComp, hash)

		index := strconv.Itoa(inp.Index)
		sigComp = append(sigComp, index)
	}

	for _, out := range message.Transaction.Outputs {
		value := strconv.Itoa(out.Value)
		sigComp = append(sigComp, value)

		typee := out.Script.Type
		sigComp = append(sigComp, typee)

		pubKey := out.Script.PubKeyHash
		sigComp = append(sigComp, pubKey)
	}

	dataBytes := []byte(strings.Join(sigComp, ""))

	for _, inp := range message.Transaction.Inputs {
		signatureBytes, err := base64.URLEncoding.DecodeString(inp.Script.Sig)
		if err != nil {
			return xerrors.Errorf("failed to decode signature string: %v", err)
		}

		publicKeySender, err := base64.URLEncoding.DecodeString(inp.Script.PubKey)
		if err != nil {
			return xerrors.Errorf("failed to decode public key string: %v", err)
		}

		err = schnorr.VerifyWithChecks(crypto.Suite, publicKeySender, dataBytes, signatureBytes)
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
