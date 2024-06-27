package hrumor

import (
	"fmt"
	"popstellar/internal/errors"
	"popstellar/internal/handler/method/rumor/mrumor"
	"sort"
	"sync"
	"time"
)

const bufferEntryLifeTime = 3 * time.Second

type buffer struct {
	sync.Mutex
	queue     []mrumor.ParamsRumor
	senderIDs map[string]struct{}
}

func newBuffer() *buffer {
	return &buffer{
		queue:     make([]mrumor.ParamsRumor, 0),
		senderIDs: make(map[string]struct{}),
	}
}

func (b *buffer) insert(params mrumor.ParamsRumor) error {
	b.Lock()
	defer b.Unlock()

	ID := fmt.Sprintf("%s:%d", params.SenderID, params.RumorID)

	_, ok := b.senderIDs[ID]
	if ok {
		return errors.NewDuplicateResourceError("rumor %s is already inside the buffer", ID)
	}

	b.queue = append(b.queue, params)

	sort.Slice(b.queue, func(i, j int) bool {
		return b.queue[i].Timestamp.IsBefore(b.queue[j].Timestamp)
	})

	b.senderIDs[ID] = struct{}{}

	go b.deleteWithDelay(ID)

	return nil
}

func (b *buffer) deleteWithDelay(ID string) {
	timer := time.NewTimer(bufferEntryLifeTime)

	<-timer.C

	b.Lock()
	defer b.Unlock()

	b.deleteEntry(ID)
}

func (b *buffer) getNextRumorParams(state mrumor.RumorTimestamp) (mrumor.ParamsRumor, bool) {
	b.Lock()
	defer b.Unlock()

	for _, param := range b.queue {
		if state.IsValid(param.Timestamp) {
			b.deleteEntry(fmt.Sprintf("%s:%d", param.SenderID, param.RumorID))
			return param, true
		}
	}

	return mrumor.ParamsRumor{}, false
}

func (b *buffer) deleteEntry(ID string) {
	for i, param := range b.queue {
		if fmt.Sprintf("%s:%d", param.SenderID, param.RumorID) == ID {
			queue := make([]mrumor.ParamsRumor, 0)
			queue = append(queue, b.queue[:i]...)
			queue = append(queue, b.queue[i+1:]...)
			b.queue = queue
		}
	}

	delete(b.senderIDs, ID)
}
