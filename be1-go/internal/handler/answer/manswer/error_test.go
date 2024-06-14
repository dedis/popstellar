package manswer

import (
	"github.com/stretchr/testify/require"
	"testing"
)

func Test_Error_Constructor(t *testing.T) {
	err := NewInvalidActionError("@@@")
	require.Equal(t, -1, err.Code)
	require.Equal(t, "invalid action: @@@", err.Description)
}
