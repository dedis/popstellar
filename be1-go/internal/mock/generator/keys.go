package generator

import (
	"github.com/stretchr/testify/require"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/crypto"
	"testing"
)

type Keypair struct {
	Public     kyber.Point
	PublicBuf  []byte
	Private    kyber.Scalar
	PrivateBuf []byte
}

func GenerateKeyPair(t *testing.T) Keypair {
	secret := crypto.Suite.Scalar().Pick(crypto.Suite.RandomStream())
	point := crypto.Suite.Point().Mul(secret, nil)

	publicBuf, err := point.MarshalBinary()
	require.NoError(t, err)
	privateBuf, err := secret.MarshalBinary()
	require.NoError(t, err)

	return Keypair{point, publicBuf, secret, privateBuf}
}
