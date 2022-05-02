package messagedata

// TransactionPost defines a message data
type TransactionPost struct {
	Object      string            `json:"object"`
	Action      string            `json:"action"`
	Transaction TransactionStruct `json:"transaction"`
}

//TBC
type TransactionStruct struct {
	Version  int           `json:"version"`
	TxIn     []TxInStruct  `json:"txin"`  // min 1
	TxOut    []TxOutStruct `json:"txout"` //min 1
	Locktime int           `json:"locktime"`
}

type TxInStruct struct {
	TxOutHash  string       `json:"txouthash"`
	TxOutIndex int          `json:"txoutindex"`
	ScripIn    UnlockScript `json:"script"`
}

type TxOutStruct struct {
	Value     int        `json:"value"`
	ScriptOut LockScript `json:"script"`
}

type LockScript struct {
	Type       string `json:"Type"`
	PubKeyHash string `json:"PubkeyHash"`
}

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
