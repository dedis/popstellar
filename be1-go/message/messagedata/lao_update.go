package messagedata

// LaoUpdate defines a message data
type LaoUpdate struct {
	Object string `json:"object"`
	Action string `json:"action"`
	ID     string `json:"id"`
	Name   string `json:"name"`

	// LastModified is a Unix timestamp
	LastModified int64 `json:"last_modified"`

	Witnesses []string `json:"witnesses"`
}

// Verifiy that the LaoUpdate message is valid
func (message LaoUpdate) Verifiy() error {

	// TODO verify LaoUpdate message once consensus is done

	return nil
}
