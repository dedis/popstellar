package src

import (
	"student20_pop/channel"
	"testing"
)

type MessageLaoCreate struct {
	OrganizerPkey []byte
	Timestamp     int64
	Name          []byte
	Ip            []byte
}

func TestLaoToJson(t *testing.T) {
	lao = channel.LAO{}

}
