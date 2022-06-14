package uint53

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func Test_Uint53Max_Equivalent(t *testing.T) {
	require.Equal(t, Uint53(1<<53-1), MaxUint53)
}

func Test_InRange_Examples(t *testing.T) {
	require.True(t, InRange(0))
	require.True(t, InRange(MaxUint53))
	require.False(t, InRange(MaxUint53+1))
}

func Test_SafePlus_Examples(t *testing.T) {
	r, err := SafePlus(0, 1)
	require.NoError(t, err)
	require.EqualValues(t, r, 1)

	r, err = SafePlus(MaxUint53, 0)
	require.NoError(t, err)
	require.EqualValues(t, r, MaxUint53)

	r, err = SafePlus(MaxUint53, 1)
	require.EqualError(t, err, "uint53 addition overflow")
	require.EqualValues(t, r, 0)

	r, err = SafePlus(MaxUint53/2, MaxUint53/2)
	require.NoError(t, err)
	require.EqualValues(t, r, MaxUint53-1)

	r, err = SafePlus(MaxUint53+1, 0)
	require.EqualError(t, err, "Uint53.SafePlus: argument out of range")
	require.EqualValues(t, r, 0)
}
