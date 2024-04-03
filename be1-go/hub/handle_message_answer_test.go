package hub

import (
	"github.com/stretchr/testify/require"
	"popstellar/message/answer"
	"testing"
)

func Test_handleGetMessagesByIDAnswer(t *testing.T) {
	type input struct {
		name        string
		params      handlerParameters
		message     answer.Answer
		isErrorTest bool
	}

	inputs := make([]input, 0)

	// failed to query db

	//id := 1
	//
	//result := make(map[string][]json.RawMessage)
	//msgsByChannel := make(map[string]map[string]message.Message)
	//
	//answer := answer.Answer{
	//	ID: &id,
	//	Result: &answer.Result{
	//		MessagesByChannel: result,
	//	},
	//}
	//
	//mockRepository := mocks.NewRepository(t)
	//mockRepository.On("AddNewBlackList", msgsByChannel).Return(xerrors.Errorf("db disconnected"))
	//
	//params := newHandlerParameters(mockRepository)
	//
	//inputs = append(inputs, input{
	//	name:        "failed to query db",
	//	params:      params,
	//	message:     answer,
	//	isErrorTest: true,
	//})

	for _, i := range inputs {
		t.Run(i.name, func(t *testing.T) {
			errAnswer := handleGetMessagesByIDAnswer(i.params, i.message)
			if i.isErrorTest {
				require.Error(t, errAnswer)
			}
		})
	}

}
