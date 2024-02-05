package inbox

import (
	state "popstellar/hub/standard_hub/hub_state"
	"popstellar/message/query/method/message"
	"sync"
)

type HubInbox struct {
	sync.RWMutex
	Inbox
	// messageIdsByChannel stores all the message ids and the corresponding channel ids
	// to help servers determine in which channel the message ids go
	messageIdsByChannel state.MessageIds
}

func NewHubInbox(channelID string) *HubInbox {
	return &HubInbox{
		Inbox:               *NewInbox(channelID),
		messageIdsByChannel: state.NewMessageIdsMap(),
	}
}

func (i *HubInbox) StoreMessage(channel string, msg message.Message) {
	i.RLock()
	defer i.RUnlock()
	i.Inbox.StoreMessage(msg)
	i.messageIdsByChannel.Add(channel, msg.MessageID)
}

func (i *HubInbox) IsEmpty() bool {
	i.RLock()
	defer i.RUnlock()
	return i.messageIdsByChannel.IsEmpty()
}

func (i *HubInbox) GetIDsTable() map[string][]string {
	i.RLock()
	defer i.RUnlock()
	return i.messageIdsByChannel.GetTable()
}
