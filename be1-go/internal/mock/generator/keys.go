package generator

import (
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"testing"
)

func GenerateKeyPair(t *testing.T) (kyber.Point, []byte, kyber.Scalar, []byte) {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	publicBuf, err := point.MarshalBinary()
	require.NoError(t, err)
	privateBuf, err := secret.MarshalBinary()
	require.NoError(t, err)

	return point, publicBuf, secret, privateBuf
}
