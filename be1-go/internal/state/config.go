package state

import (
	"encoding/base64"
	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"go.dedis.ch/kyber/v3/sign/schnorr"
	"popstellar/internal/crypto"
	"popstellar/internal/errors"
)

type Config struct {
	ownerPubKey         kyber.Point
	serverPubKey        kyber.Point
	serverSecretKey     kyber.Scalar
	clientServerAddress string
	serverServerAddress string
	log                 zerolog.Logger
}

func CreateConfig(ownerPubKey, serverPubKey kyber.Point, serverSecretKey kyber.Scalar,
	clientServerAddress, serverServerAddress string, log zerolog.Logger) *Config {
	return &Config{
		ownerPubKey:         ownerPubKey,
		serverPubKey:        serverPubKey,
		serverSecretKey:     serverSecretKey,
		clientServerAddress: clientServerAddress,
		serverServerAddress: serverServerAddress,
		log:                 log.With().Str("module", "config").Logger(),
	}
}

func (c *Config) GetOwnerPublicKey() kyber.Point {
	return c.ownerPubKey
}

func (c *Config) GetServerPublicKey() kyber.Point {
	return c.serverPubKey
}

func (c *Config) GetServerSecretKey() kyber.Scalar {
	return c.serverSecretKey
}

func (c *Config) GetServerInfo() (string, string, string, error) {
	pkBuf, err := c.serverPubKey.MarshalBinary()
	if err != nil {
		return "", "", "", errors.NewJsonUnmarshalError("server public key: %v", err)
	}

	return base64.URLEncoding.EncodeToString(pkBuf), c.clientServerAddress, c.serverServerAddress, nil
}

func (c *Config) Sign(data []byte) ([]byte, error) {
	signatureBuf, err := schnorr.Sign(crypto.Suite, c.serverSecretKey, data)
	if err != nil {
		return nil, errors.NewInternalServerError("failed to sign the data: %v", err)
	}
	return signatureBuf, nil
}
