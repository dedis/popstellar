package message

import (
	"encoding/json"

	"golang.org/x/xerrors"
)

type Query struct {
	Subscribe   *Subscribe
	Unsubscribe *Unsubscribe
	Publish     *Publish
	Catchup     *Catchup
	Broadcast   *Broadcast
}

type Subscribe struct {
	ID     int    `json:"id"`
	Method string `json:"method"`
	Params Params `json:"params"`
}

type Params struct {
	Channel string `json:"channel"`

	Message *Message `json:"message"`
}

type Unsubscribe struct {
	ID     int    `json:"id"`
	Method string `json:"method"`
	Params Params `json:"params"`
}

type Publish struct {
	ID     int    `json:"id"`
	Params Params `json:"params"`
}

type Catchup struct {
	ID     int    `json:"id"`
	Method string `json:"method"`
	Params Params `json:"params"`
}

type Broadcast struct {
	ID     int    `json:"id"`
	Method string `json:"method"`
	Params Params `json:"params"`
}

func (q *Query) UnmarshalJSON(data []byte) error {
	type internal struct {
		Method string `json:"method"`
	}

	tmp := &internal{}

	err := json.Unmarshal(data, tmp)
	if err != nil {
		return xerrors.Errorf("failed to parse query method: %v", err)
	}

	switch tmp.Method {
	case "subscribe":
		subscribe := &Subscribe{}

		err := json.Unmarshal(data, subscribe)
		if err != nil {
			return xerrors.Errorf("failed to parse subscribe message: %v", err)
		}

		q.Subscribe = subscribe
		return nil
	case "unsubscribe":
		unsubscribe := &Unsubscribe{}

		err := json.Unmarshal(data, unsubscribe)
		if err != nil {
			return xerrors.Errorf("failed to parse subscribe message: %v", err)
		}

		q.Unsubscribe = unsubscribe
		return nil
	case "publish":
		publish := &Publish{}

		err := json.Unmarshal(data, publish)
		if err != nil {
			return xerrors.Errorf("failed to parse publish message: %v", err)
		}

		q.Publish = publish
		return nil
	case "broadcast":
		broadcast := &Broadcast{}

		err := json.Unmarshal(data, broadcast)
		if err != nil {
			return xerrors.Errorf("failed to parse broadcast message: %v", err)
		}

		q.Broadcast = broadcast
		return nil
	case "catchup":
		catchup := &Catchup{}

		err := json.Unmarshal(data, catchup)
		if err != nil {
			return xerrors.Errorf("failed to parse catchup message: %v", err)
		}

		q.Catchup = catchup
		return nil
	default:
		return xerrors.Errorf("failed to parse query: invalid method type: %s", tmp.Method)
	}
}
