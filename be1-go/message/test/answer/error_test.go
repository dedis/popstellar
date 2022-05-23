package answer

import (
	"popstellar/message/answer"
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Error_Constructor(t *testing.T) {
	err := answer.NewInvalidActionError("@@@")
	require.Equal(t, -1, err.Code)
	require.Equal(t, "invalid action: @@@", err.Description)

	err = answer.NewInvalidObjectError("@@@")
	require.Equal(t, -1, err.Code)
	require.Equal(t, "invalid object: @@@", err.Description)
}
