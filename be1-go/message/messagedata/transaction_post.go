package messagedata

/*
type TransactionPost struct {
	Object string `json:"object"`
	Action string `json:"action"`

	Properties struct {
		Transaction struct {
			Type        string `json:"type"`
			Description string `json:"description"`
			Properties  struct {
				Version struct {
					Type        string `json:"type"`
					Description string `json:"description"`
				} `json:"Version"`
				TxIn struct {
					Type        string `json:"type"`
					Description string `json:"description"`
					Items       struct {
						Type        string `json:"type"`
						Description string `json:"description"`
						Properties  struct {
							TxOutHash struct {
								Type            string `json:"type"`
								ContentEncoding string `json:"contentEncoding"`
								Description     string `json:"description"`
							} `json:"TxOutHash"`
							TxOutIndex struct {
								Type        string `json:"type"`
								Description string `json:"description"`
							} `json:"TxOutIndex"`
							Script struct {
								Type        string `json:"type"`
								Description string `json:"description"`
								Properties  struct {
									Type struct {
										Type        string `json:"type"`
										Description string `json:"description"`
									} `json:"Type"`
									Pubkey struct {
										Type            string `json:"type"`
										ContentEncoding string `json:"contentEncoding"`
										Description     string `json:"description"`
									} `json:"Pubkey"`
									Sig struct {
										Type            string `json:"type"`
										ContentEncoding string `json:"contentEncoding"`
										Description     string `json:"description"`
									} `json:"Sig"`
								} `json:"properties"`
								Required []string `json:"required"`
							} `json:"Script"`
						} `json:"properties"`
						Required []string `json:"required"`
					} `json:"items"`
					MinItems int `json:"minItems"`
				} `json:"TxIn"`
				TxOut struct {
					Type        string `json:"type"`
					Description string `json:"description"`
					Items       struct {
						Type        string `json:"type"`
						Description string `json:"description"`
						Properties  struct {
							Value struct {
								Type        string `json:"type"`
								Description string `json:"description"`
							} `json:"Value"`
							Script struct {
								Type        string `json:"type"`
								Description string `json:"description"`
								Properties  struct {
									Type struct {
										Type        string `json:"type"`
										Description string `json:"description"`
									} `json:"Type"`
									PubkeyHash struct {
										Type            string `json:"type"`
										ContentEncoding string `json:"contentEncoding"`
										Description     string `json:"description"`
									} `json:"PubkeyHash"`
								} `json:"properties"`
								Required []string `json:"required"`
							} `json:"Script"`
						} `json:"properties"`
						Required []string `json:"required"`
					} `json:"items"`
					MinItems int `json:"minItems"`
				} `json:"TxOut"`
				LockTime struct {
					Type        string `json:"type"`
					Description string `json:"description"`
				} `json:"LockTime"`
			} `json:"properties"`
			Required []string `json:"required"`
		} `json:"transaction"`
	} `json:"properties"`
	Required []string `json:"required"`
}

*/
