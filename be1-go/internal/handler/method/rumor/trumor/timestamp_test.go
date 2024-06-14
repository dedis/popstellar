package trumor

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/method/rumor/mrumor"
	"popstellar/internal/handler/query/mquery"
	"testing"
)

func Test_IsValid(t *testing.T) {
	ts := make(RumorTimestamp)
	ts["0"] = 4
	ts["1"] = 3
	ts["2"] = 2

	rumor := mrumor.Rumor{
		Base: mquery.Base{},
		ID:   0,
		Params: mrumor.ParamsRumor{
			SenderID:  "0",
			RumorID:   4,
			Timestamp: ts,
			Messages:  nil,
		},
	}

	// valid rumor

	state := make(RumorTimestamp)
	state["0"] = 3
	state["1"] = 3
	state["2"] = 2

	isValid := state.IsValid(rumor)
	require.True(t, isValid)

	// invalid rumor because one entry in the timestamp is smaller

	state = make(RumorTimestamp)
	state["0"] = 3
	state["1"] = 3
	state["2"] = 1

	isValid = state.IsValid(rumor)
	require.False(t, isValid)

	// invalid rumor because one entry in the timestamp doesn't exist

	state = make(RumorTimestamp)
	state["0"] = 3
	state["1"] = 3

	isValid = state.IsValid(rumor)
	require.False(t, isValid)

	// invalid rumor because not + 1

	state = make(RumorTimestamp)
	state["0"] = 2
	state["1"] = 3
	state["2"] = 2

	isValid = state.IsValid(rumor)
	require.False(t, isValid)

	// invalid rumor because no entry inside the timestamp
	state = make(RumorTimestamp)
	state["1"] = 3
	state["2"] = 2

	isValid = state.IsValid(rumor)
	require.False(t, isValid)

}
