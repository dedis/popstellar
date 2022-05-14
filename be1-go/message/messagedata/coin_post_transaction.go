package messagedata

// TransactionPost defines a message data
type PostTransaction struct {
	Object        string      `json:"object"`
	Action        string      `json:"action"`
	TransactionId string      `json:"transaction_id"`
	Transaction   Transaction `json:"transaction"`
}

//Transaction define the input and output account for the transaction
type Transaction struct {
	Version  int      `json:"version"`
	Inputs   []Input  `json:"inputs"`  // min 1
	Outputs  []Output `json:"outputs"` //min 1
	Locktime int      `json:"lock_time"`
}

// Input define the source from the money used in transaction
type Input struct {
	Hash   string       `json:"tx_out_hash"`
	Index  int          `json:"tx_out_index"`
	Script UnlockScript `json:"script"`
}

// Output define the destination from the money used in transaction
type Output struct {
	Value  int        `json:"value"`
	Script LockScript `json:"script"`
}

// LockScript define the locking value for transaction
type LockScript struct {
	Type       string `json:"type"`
	PubKeyHash string `json:"pubkey_hash"`
}

// UnlockScript define the unlocking value for transaction
type UnlockScript struct {
	Type   string `json:"type"`
	PubKey string `json:"pubkey"`
	Sig    string `json:"sig"`
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
