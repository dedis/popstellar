package method

// Subscribe ...
type Subscribe struct {
	ID int `json:"id"`

	Params struct {
		Channel string `json:"channel"`
	}
}
