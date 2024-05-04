package config

import (
	"go.dedis.ch/kyber/v3"
	"golang.org/x/xerrors"
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

func GetOwnerPublicKeyInstance() (kyber.Point, bool) {
	if instance == nil {
		return nil, false
	}

	return instance.ownerPubKey, true
}

func GetServerPublicKeyInstance() (kyber.Point, bool) {
	if instance == nil || instance.serverPubKey == nil {
		return nil, false
	}

	return instance.serverPubKey, true
}

func GetServerSecretKeyInstance() (kyber.Scalar, bool) {
	if instance == nil || instance.serverPubKey == nil {
		return nil, false
	}

	return instance.serverSecretKey, true
}

func GetServerInfo() (kyber.Point, string, string, bool) {
	if instance == nil {
		return nil, "", "", false
	}

	return instance.serverPubKey, instance.clientServerAddress, instance.serverServerAddress, true
}
