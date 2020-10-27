package src

import (
	src "student20_pop/define"
	"testing"
)

type MessageLaoCreate struct {
	OrganizerPkey []byte
	Timestamp     int64
	Name          []byte
	Ip            []byte
}

func TestLaoToJson(t *testing.T){
	lao = src.LAO{}

}