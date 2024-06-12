package hrumor

import (
	"fmt"
	"popstellar/internal/errors"
	"popstellar/internal/handler/method/rumor/mrumor"
	"sync"
	"time"
)

const bufferEntryLifeTime = 3 * time.Second

type buffer struct {
	sync.Mutex
	values map[string]mrumor.Rumor
}

func newBuffer() *buffer {
	return &buffer{
		values: make(map[string]mrumor.Rumor),
	}
}

func (b *buffer) insert(rumor mrumor.Rumor) error {
	b.Lock()
	defer b.Unlock()

	ID := fmt.Sprintf("%s:%d", rumor.Params.SenderID, rumor.Params.RumorID)

	_, ok := b.values[ID]
	if ok {
		return errors.NewDuplicateResourceError("rumor %s is already inside the buffer", ID)
	}

	b.values[ID] = rumor

	go b.deleteWithDelay(ID)

	return nil
}

func (b *buffer) deleteWithDelay(ID string) {
	defer b.Unlock()

	timer := time.NewTimer(bufferEntryLifeTime)

	<-timer.C

	b.Lock()
	delete(b.values, ID)
}

func (b *buffer) getNextRumor(senderID string, rumorID int) (mrumor.Rumor, bool) {
	b.Lock()
	defer b.Unlock()

	ID := fmt.Sprintf("%s:%d", senderID, rumorID+1)
	rumor, ok := b.values[ID]
	if !ok {
		return mrumor.Rumor{}, false
	}

	delete(b.values, ID)

	return rumor, true
}
