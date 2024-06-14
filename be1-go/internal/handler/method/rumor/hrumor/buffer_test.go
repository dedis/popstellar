package hrumor

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/method/rumor/trumor"
	"testing"
	"time"
)

func Test_insert(t *testing.T) {
	buf := newBuffer()

	rumor := mrumor.Rumor{
		Params: mrumor.ParamsRumor{
			SenderID: "sender",
			RumorID:  0,
			Messages: nil,
		},
	}

	// successful insert

	err := buf.insert(rumor)
	require.NoError(t, err)

	// returns error when rumor already buffered

	err = buf.insert(rumor)
	require.Error(t, err)

	// successful insert after the delay

	time.Sleep(bufferEntryLifeTime * 2)

	err = buf.insert(rumor)
	require.NoError(t, err)
}

func Test_getNextRumor(t *testing.T) {
	buf := newBuffer()

	// one next rumor

	sender := "sender0"

	timestamp00 := make(map[string]int)

	rumor00 := mrumor.Rumor{
		Params: mrumor.ParamsRumor{
			SenderID:  sender,
			RumorID:   0,
			Timestamp: timestamp00,
			Messages:  nil,
		},
	}

	timestamp01 := make(map[string]int)
	timestamp01[sender] = 0

	rumor01 := mrumor.Rumor{
		Params: mrumor.ParamsRumor{
			SenderID:  sender,
			RumorID:   1,
			Timestamp: timestamp01,
			Messages:  nil,
		},
	}

	err := buf.insert(rumor00)
	require.NoError(t, err)

	err = buf.insert(rumor01)
	require.NoError(t, err)

	state := make(trumor.RumorTimestamp)

	_, ok := buf.getNextRumor(state)
	require.True(t, ok)

	state[sender] = 0

	_, ok = buf.getNextRumor(state)
	require.True(t, ok)

	// no next rumor

	sender = "sender1"

	timestamp10 := make(map[string]int)
	timestamp10[sender] = 1

	rumor10 := mrumor.Rumor{
		Params: mrumor.ParamsRumor{
			SenderID:  sender,
			RumorID:   1,
			Timestamp: timestamp10,
			Messages:  nil,
		},
	}

	err = buf.insert(rumor10)
	require.NoError(t, err)

	_, ok = buf.getNextRumor(state)
	require.False(t, ok)
}
