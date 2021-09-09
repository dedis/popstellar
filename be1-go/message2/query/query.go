package query

import (
	"encoding/json"
	"student20_pop/message2/query/method"

	"golang.org/x/xerrors"
)

// Query ...
type Query struct {
	Method string

	method.Subscribe
	method.Broadcast
	method.Unsubscribe
	method.Publish
	method.Catchup
}

func (q *Query) UnmarshalJSON(buf []byte) error {
	var objmap map[string]json.RawMessage

	err := json.Unmarshal(buf, &objmap)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal generic map: %v", err)
	}

	// fill the "Method" field
	err = json.Unmarshal(objmap["method"], &q.Method)
	if err != nil {
		return xerrors.Errorf("failed to get method string: %v", err)
	}

	// get a pointer to the corresponding object of the method
	obj := q.MethodToObj(q.Method)
	if obj == nil {
		return xerrors.Errorf("method string '%s' unknown", q.Method)
	}

	// fills the corresponding object, recall that we got a pointer
	err = json.Unmarshal(buf, obj)
	if err != nil {
		return xerrors.Errorf("failed to unmarshal obj: %v", err)
	}

	return nil
}

// MethodToObj returns a pointer that points to the object corresponding to the
// method string. Return nil if the method string is unknown.
func (q *Query) MethodToObj(method string) interface{} {
	switch method {
	case "publish":
		return &q.Publish
	case "subscribe":
		return &q.Subscribe
	case "unsubscribe":
		return &q.Unsubscribe
	case "broadcast":
		return &q.Broadcast
	case "catchup":
		return &q.Catchup
	}

	return nil
}
