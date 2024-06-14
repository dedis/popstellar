package hrumor

import (
	"fmt"
	"popstellar/internal/errors"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/method/rumor/trumor"
	"sort"
	"sync"
	"time"
)

const bufferEntryLifeTime = 3 * time.Second

type buffer struct {
	sync.Mutex
	queue     []mrumor.Rumor
	senderIDs map[string]struct{}
}

func newBuffer() *buffer {
	return &buffer{
		queue:     make([]mrumor.Rumor, 0),
		senderIDs: make(map[string]struct{}),
	}
}

func (b *buffer) insert(rumor mrumor.Rumor) error {
	b.Lock()
	defer b.Unlock()

	ID := fmt.Sprintf("%s:%d", rumor.Params.SenderID, rumor.Params.RumorID)

	_, ok := b.senderIDs[ID]
	if ok {
		return errors.NewDuplicateResourceError("rumor %s is already inside the buffer", ID)
	}

	b.queue = append(b.queue, rumor)

	sort.Slice(b.queue, func(i, j int) bool {
		return b.queue[i].IsBefore(b.queue[j])
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

func (b *buffer) getNextRumor(state trumor.RumorTimestamp) (mrumor.Rumor, bool) {
	b.Lock()
	defer b.Unlock()

	for _, rumor := range b.queue {
		if state.IsValid(rumor.Params.Timestamp) {
			b.deleteEntry(fmt.Sprintf("%s:%d", rumor.Params.SenderID, rumor.Params.RumorID))
			return rumor, true
		}
	}

	return mrumor.Rumor{}, false
}

func (b *buffer) deleteEntry(ID string) {
	for i, rumor := range b.queue {
		if fmt.Sprintf("%s:%d", rumor.Params.SenderID, rumor.Params.RumorID) == ID {
			queue := make([]mrumor.Rumor, 0)
			queue = append(queue, b.queue[:i]...)
			queue = append(queue, b.queue[i+1:]...)
			b.queue = queue
		}
	}

	delete(b.senderIDs, ID)
}
