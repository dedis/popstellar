package method

// Unsubscribe ....
type Unsubscribe struct {
	ID int `json:"id"`

	Params struct {
		Channel string `json:"channel"`
	}
}
