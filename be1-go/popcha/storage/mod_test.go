package storage

import (
	"bytes"
	"github.com/stretchr/testify/require"
	"math/rand"
	"popstellar"
	"reflect"
	"testing"
	"testing/quick"
)

const (
	Alphabet      = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01233456789"
	MaxStringSize = 128
	MaxChecks     = 100000
)

func TestNewStorage(t *testing.T) {
	s, err := NewStorage(popstellar.Logger)
	require.NoError(t, err)
	s.log.Info().Msg("Test Storage")

	require.NoError(t, s.AddNonce("NONCEv1"))

	require.NoError(t, s.SetIdentifier("pop", "id"))

	require.Equal(t, "id", s.GetIdentifier("pop"))
}

func TestStorageAddSameNonce(t *testing.T) {
	s, err := NewStorage(popstellar.Logger)
	require.NoError(t, err)
	s.log.Info().Msg("Test Storage")

	nonceReplayConfig := quick.Config{
		MaxCount: MaxChecks,
		Values: func(values []reflect.Value, r *rand.Rand) {
			values[0] = reflect.ValueOf(genString(r, r.Intn(MaxStringSize)))
		}}

	err = quick.Check(nonceReplayCaught(s), &nonceReplayConfig)
	require.NoError(t, err)
}

func genString(r *rand.Rand, s int) string {
	if s == 0 {
		s += 1
	}
	var b bytes.Buffer
	for i := 0; i < s; i++ {
		rdmIdx := r.Intn(len(Alphabet))
		b.WriteString(string(Alphabet[rdmIdx]))
	}
	return b.String()
}

func nonceReplayCaught(s *Storage) func(nonce string) bool {
	return func(nonce string) bool {
		alreadyIn := s.GetNonce(nonce)

		err := s.AddNonce(nonce)
		if err != nil {
			return alreadyIn
		}
		return s.AddNonce(nonce) != nil
	}
}
