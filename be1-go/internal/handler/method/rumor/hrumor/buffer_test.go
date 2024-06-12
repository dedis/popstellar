package hrumor

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/method/rumor/mrumor"
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

	rumor00 := mrumor.Rumor{
		Params: mrumor.ParamsRumor{
			SenderID: "sender0",
			RumorID:  0,
			Messages: nil,
		},
	}
	rumor01 := mrumor.Rumor{
		Params: mrumor.ParamsRumor{
			SenderID: "sender0",
			RumorID:  1,
			Messages: nil,
		},
	}

	err := buf.insert(rumor00)
	require.NoError(t, err)

	err = buf.insert(rumor01)
	require.NoError(t, err)

	_, ok := buf.getNextRumor(rumor00.Params.SenderID, rumor00.Params.RumorID)
	require.True(t, ok)

	// no next rumor

	rumor10 := mrumor.Rumor{
		Params: mrumor.ParamsRumor{
			SenderID: "sender1",
			RumorID:  0,
			Messages: nil,
		},
	}

	err = buf.insert(rumor10)
	require.NoError(t, err)

	_, ok = buf.getNextRumor(rumor10.Params.SenderID, rumor10.Params.RumorID)
	require.False(t, ok)
}
