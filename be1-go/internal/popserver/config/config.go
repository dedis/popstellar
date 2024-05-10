package config

import (
	"encoding/base64"
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
	"popstellar/message/answer"
	"sync"
	"testing"
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

func SetConfig(t *testing.T, ownerPubKey, serverPubKey kyber.Point, serverSecretKey kyber.Scalar,
	clientServerAddress, serverServerAddress string) error {
	if t == nil {
		return xerrors.Errorf("only for tests")
	}

	instance = &config{
		ownerPubKey:         ownerPubKey,
		serverPubKey:        serverPubKey,
		serverSecretKey:     serverSecretKey,
		clientServerAddress: clientServerAddress,
		serverServerAddress: serverServerAddress,
	}

	return nil
}

func getConfig() (*config, *answer.Error) {
	if instance == nil {
		errAnswer := answer.NewInternalServerError("config was not instantiated")
		return nil, errAnswer
	}

	return instance, nil
}

func GetOwnerPublicKeyInstance() (kyber.Point, *answer.Error) {
	config, errAnswer := getConfig()
	if errAnswer != nil {
		return nil, errAnswer
	}

	return config.ownerPubKey, nil
}

func GetServerPublicKeyInstance() (kyber.Point, *answer.Error) {
	config, errAnswer := getConfig()
	if errAnswer != nil {
		return nil, errAnswer
	}

	return config.serverPubKey, nil
}

func GetServerSecretKeyInstance() (kyber.Scalar, *answer.Error) {
	config, errAnswer := getConfig()
	if errAnswer != nil {
		return nil, errAnswer
	}

	return config.serverSecretKey, nil
}

func GetServerInfo() (string, string, string, *answer.Error) {
	config, errAnswer := getConfig()
	if errAnswer != nil {
		return "", "", "", errAnswer
	}

	pkBuf, err := config.serverPubKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshall server public key", err)
		return "", "", "", errAnswer
	}

	return base64.URLEncoding.EncodeToString(pkBuf), instance.clientServerAddress, instance.serverServerAddress, nil
}
