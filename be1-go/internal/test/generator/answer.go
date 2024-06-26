package generator

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"popstellar/internal/handler/answer/manswer"
	"popstellar/internal/handler/method/rumor/mrumor"
	"testing"
)

func NewRumorStateAnswer(t *testing.T, id int, rumors []mrumor.Rumor) (manswer.Answer, []byte) {
	data := make([]json.RawMessage, 0)
	for _, rumor := range rumors {
		paramBuf, err := json.Marshal(rumor.Params)
		require.NoError(t, err)
		data = append(data, paramBuf)
	}

	answer := manswer.Answer{
		ID:     &id,
		Result: &manswer.Result{Data: data},
	}

	answerBuf, err := json.Marshal(&answer)
	require.NoError(t, err)

	return answer, answerBuf
}

func NewRumorAnswer(t *testing.T, id int, err *manswer.Error) (manswer.Answer, []byte) {
	answer := manswer.Answer{
		ID:    &id,
		Error: err,
	}

	answerBuf, err2 := json.Marshal(&answer)
	require.NoError(t, err2)

	return answer, answerBuf
}
