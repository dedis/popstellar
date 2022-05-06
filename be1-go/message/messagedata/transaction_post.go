package messagedata

// TransactionPost defines a message data
type TransactionPost struct {
	Object        string      `json:"object"`
	Action        string      `json:"action"`
	TransactionId string      `json:"transaction_id"`
	Transaction   Transaction `json:"transaction"`
}

//Transaction define the input and output account for the transaction
type Transaction struct {
	Version  int      `json:"version"`
	Inputs   []Input  `json:"txin"`  // min 1
	Outputs  []Output `json:"txout"` //min 1
	Locktime int      `json:"locktime"`
}

// Input define the source from the money used in transaction
type Input struct {
	Hash   string       `json:"txouthash"`
	Index  int          `json:"txoutindex"`
	Script UnlockScript `json:"script"`
}

// Output define the destination from the money used in transaction
type Output struct {
	Value  int        `json:"value"`
	Script LockScript `json:"script"`
}

// LockScript define the locking value for transaction
type LockScript struct {
	Type       string `json:"Type"`
	PubKeyHash string `json:"PubkeyHash"`
}

// UnlockScript define the unlocking value for transaction
type UnlockScript struct {
	Type   string `json:"Type"`
	PubKey string `json:"Pubkey"`
	Sig    string `json:"Sig"`
}

// GetObject implements MessageData
func (TransactionPost) GetObject() string {
	return TransactionObject
}

// GetAction implements MessageData
func (TransactionPost) GetAction() string {
	return TransactionActionPost
}

// NewEmpty implements MessageData
func (TransactionPost) NewEmpty() MessageData {
	return &TransactionPost{}
}
