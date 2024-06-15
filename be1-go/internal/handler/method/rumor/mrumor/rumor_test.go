package mrumor

import (
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/query/mquery"
	"sort"
	"testing"
)

func TestRumor_IsBefore(t *testing.T) {
	timestamp00 := make(map[string]int)

	rumor00 := Rumor{
		Base: mquery.Base{},
		ID:   0,
		Params: ParamsRumor{
			SenderID:  "0",
			RumorID:   0,
			Timestamp: timestamp00,
			Messages:  nil,
		},
	}

	timestamp01 := make(map[string]int)
	timestamp01["0"] = 0

	rumor01 := Rumor{
		Base: mquery.Base{},
		ID:   0,
		Params: ParamsRumor{
			SenderID:  "0",
			RumorID:   1,
			Timestamp: timestamp01,
			Messages:  nil,
		},
	}

	timestamp02 := make(map[string]int)
	timestamp02["0"] = 1
	timestamp02["1"] = 0
	timestamp02["2"] = 0
	timestamp02["3"] = 0

	rumor02 := Rumor{
		Base: mquery.Base{},
		ID:   0,
		Params: ParamsRumor{
			SenderID:  "0",
			RumorID:   2,
			Timestamp: timestamp02,
			Messages:  nil,
		},
	}

	timestamp10 := make(map[string]int)

	rumor10 := Rumor{
		Base: mquery.Base{},
		ID:   0,
		Params: ParamsRumor{
			SenderID:  "1",
			RumorID:   0,
			Timestamp: timestamp10,
			Messages:  nil,
		},
	}

	timestamp20 := make(map[string]int)
	timestamp20["0"] = 0
	timestamp20["1"] = 0

	rumor20 := Rumor{
		Base: mquery.Base{},
		ID:   0,
		Params: ParamsRumor{
			SenderID:  "2",
			RumorID:   0,
			Timestamp: timestamp20,
			Messages:  nil,
		},
	}

	timestamp30 := make(map[string]int)
	timestamp30["0"] = 1
	timestamp30["1"] = 0
	timestamp30["2"] = 0

	rumor30 := Rumor{
		Base: mquery.Base{},
		ID:   0,
		Params: ParamsRumor{
			SenderID:  "3",
			RumorID:   0,
			Timestamp: timestamp30,
			Messages:  nil,
		},
	}

	rumors := []Rumor{rumor02, rumor30, rumor00, rumor01, rumor10, rumor20}

	sort.Slice(rumors, func(i, j int) bool {
		return rumors[i].IsBefore(rumors[j])
	})

	myState := make(RumorTimestamp)

	for _, rumor := range rumors {
		ok := myState.IsValid(rumor.Params.Timestamp)
		require.True(t, ok)
		myState[rumor.Params.SenderID] = rumor.Params.RumorID
	}
}

func Test_IsValid(t *testing.T) {
	ts := make(RumorTimestamp)
	ts["0"] = 3
	ts["1"] = 3
	ts["2"] = 2

	rumor := Rumor{
		Base: mquery.Base{},
		ID:   0,
		Params: ParamsRumor{
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

	isValid := state.IsValid(rumor.Params.Timestamp)
	require.True(t, isValid)

	// invalid rumor because one entry in the timestamp is smaller

	state = make(RumorTimestamp)
	state["0"] = 3
	state["1"] = 3
	state["2"] = 1

	isValid = state.IsValid(rumor.Params.Timestamp)
	require.False(t, isValid)

	// invalid rumor because one entry in the timestamp doesn't exist

	state = make(RumorTimestamp)
	state["0"] = 3
	state["1"] = 3

	isValid = state.IsValid(rumor.Params.Timestamp)
	require.False(t, isValid)

	// invalid rumor because not + 1

	state = make(RumorTimestamp)
	state["0"] = 2
	state["1"] = 3
	state["2"] = 2

	isValid = state.IsValid(rumor.Params.Timestamp)
	require.False(t, isValid)

	// invalid rumor because no entry inside the timestamp
	state = make(RumorTimestamp)
	state["1"] = 3
	state["2"] = 2

	isValid = state.IsValid(rumor.Params.Timestamp)
	require.False(t, isValid)

}
