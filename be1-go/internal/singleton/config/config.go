package config

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/errors"
	"sync"
)

var once sync.Once
var instance *config

type config struct {
	ownerPubKey         kyber.Point
	serverPubKey        kyber.Point
	serverSecretKey     kyber.Scalar
	clientServerAddress string
	serverServerAddress string
}

func InitConfig(ownerPubKey, serverPubKey kyber.Point, serverSecretKey kyber.Scalar,
	clientServerAddress, serverServerAddress string) {
	once.Do(func() {
		instance = &config{
			ownerPubKey:         ownerPubKey,
			serverPubKey:        serverPubKey,
			serverSecretKey:     serverSecretKey,
			clientServerAddress: clientServerAddress,
			serverServerAddress: serverServerAddress,
		}
	})
}

// ONLY FOR TEST PURPOSE
// SetConfig is only here to be used to reset the config before each test
func SetConfig(ownerPubKey, serverPubKey kyber.Point, serverSecretKey kyber.Scalar,
	clientServerAddress, serverServerAddress string) {
	instance = &config{
		ownerPubKey:         ownerPubKey,
		serverPubKey:        serverPubKey,
		serverSecretKey:     serverSecretKey,
		clientServerAddress: clientServerAddress,
		serverServerAddress: serverServerAddress,
	}
}

func getConfig() (*config, error) {
	if instance == nil {
		return nil, errors.NewInternalServerError("config was not instantiated")
	}

	return instance, nil
}

func GetOwnerPublicKeyInstance() (kyber.Point, error) {
	config, err := getConfig()
	if err != nil {
		return nil, err
	}

	return config.ownerPubKey, nil
}

func GetServerPublicKeyInstance() (kyber.Point, error) {
	config, err := getConfig()
	if err != nil {
		return nil, err
	}

	return config.serverPubKey, nil
}

func GetServerSecretKeyInstance() (kyber.Scalar, error) {
	config, err := getConfig()
	if err != nil {
		return nil, err
	}

	return config.serverSecretKey, nil
}

func GetServerInfo() (string, string, string, error) {
	config, err := getConfig()
	if err != nil {
		return "", "", "", err
	}

	pkBuf, err := config.serverPubKey.MarshalBinary()
	if err != nil {
		return "", "", "", errors.NewJsonUnmarshalError("server public key: %v", err)
	}

	return base64.URLEncoding.EncodeToString(pkBuf), instance.clientServerAddress, instance.serverServerAddress, nil
}
