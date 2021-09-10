package method

// Catchup ....
type Catchup struct {
	ID int `json:"id"`

	Params struct {
		Channel string `json:"channel"`
	}
}
