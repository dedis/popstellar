package WebSocket

import (
	b64 "encoding/base64"
	"encoding/json"
	"github.com/boltdb/bolt"
	"log"
	"student20_pop/db"
	"student20_pop/define"
)

type Witness struct {
	db                 *bolt.DB
	subscribedChannels []string
}

func NewWitness() *Witness {
	databaseTemp, err := db.OpenDB(db.WitDatabase)
	if err != nil {
		log.Fatal("couldn't start a new db")
	}
	w := &Witness{
		db:                 databaseTemp,
		subscribedChannels: make([]string, 0),
	}
	return w
}

func (w *Witness) CloseDB() {
	w.db.Close()
}

//Currently can only sign messages and store "publish messages"
func (h *hub) witnessHandleWholeMessage(msg []byte, userId int) {
	generic, err := define.AnalyseGeneric(msg)
	if err != nil {
		err = define.ErrRequestDataInvalid
		h.responseToSender = define.CreateResponse(err, nil, generic)
		return
	}

	var history []byte = nil

	switch generic.Method {
	case "publish":
		err = nil //h.handlePublish(generic)
	default:
		// do nothing
	}

	h.responseToSender = define.CreateResponse(err, history, generic)
}

func (h *hub) witnessHandlePublish(generic define.Generic) error {
	params, err := define.AnalyseParamsFull(generic.Params)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	message, err := define.AnalyseMessage(params.Message)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	data := define.Data{}
	base64Text := make([]byte, b64.StdEncoding.DecodedLen(len(message.Data)))
	l, _ := b64.StdEncoding.Decode(base64Text, message.Data)
	err = json.Unmarshal(base64Text[:l], &data)

	//data, err := define.AnalyseData(message.Data)
	if err != nil {
		return define.ErrRequestDataInvalid
	}

	switch data["object"] {
	case "lao":
		switch data["action"] {
		case "update_properties":
			return h.witnessHandleUpdateProperties(message, params.Channel, generic)
		default:
			return db.CreateMessage(message, params.Channel)
		}

	default:
		return db.CreateMessage(message, params.Channel)
	}
}

func (h *hub) witnessHandleUpdateProperties(message define.Message, channel string, generic define.Generic) error {
	err := db.CreateMessage(message, channel)
	if err != nil {
		return err
	}
	h.message = witnessSignMessage(message)
	h.channel = []byte(channel)
	return nil
}

func witnessSignMessage(message define.Message) []byte {
	return nil
}
