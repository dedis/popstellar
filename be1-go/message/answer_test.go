package message

import (
	"encoding/json"
	"testing"
)

func TestAnswer_ResultGeneral(t *testing.T) {
	data := `
	{
		"id": 1,
		"result": [{"method": "subscribe"}]
	}
	`

	answer := &Answer{}
	json.Unmarshal([]byte(data), answer)
}
